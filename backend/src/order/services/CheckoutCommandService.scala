package delivery.order.services

import cats.effect.IO
import cats.syntax.all.*
import delivery.admin.tables.platformpromotion.PlatformPromotionTable
import delivery.merchant.objects.Merchant
import delivery.merchant.tables.catalogproduct.CatalogProductTable
import delivery.merchant.tables.merchantstore.MerchantStoreTable
import delivery.order.objects.apiTypes.{CheckoutRequest, CheckoutResponse}
import delivery.order.tables.checkoutrequest.CheckoutRequestTable
import delivery.order.tables.order.OrderTable
import delivery.platform.api.HttpApiError
import delivery.promotion.objects.Promotion
import delivery.promotion.services.{PromotionUsage, StandardPlatformVoucherService, VoucherRedemptionService}
import delivery.user.tables.customerprofile.CustomerProfileTable

import java.sql.Connection

object CheckoutCommandService:

  def submitCheckout(connection: Connection, username: String, body: CheckoutRequest): IO[CheckoutResponse] =
    for
      account <- CustomerProfileTable.findByUsername(connection, username).flatMap {
        case Some(value) => IO.pure(value)
        case None        => IO.raiseError(HttpApiError.NotFound("未找到顾客"))
      }
      normalizedAccount = account.copy(profile = account.profile.copy(vouchers = StandardPlatformVoucherService.mergeStandardPlatformVouchers(account.profile.id, account.profile.vouchers)))
      products <- CatalogProductTable.listForUpdate(connection)
      merchants <- MerchantStoreTable.listCatalog(connection)
      platformPromotions <- PlatformPromotionTable.get(connection)
      profileForOrders =
        (body.customerName, body.customerPhone, body.deliveryAddress) match
          case (Some(n), Some(ph), Some(ad))
              if n.trim.nonEmpty && ph.trim.nonEmpty && ad.trim.nonEmpty =>
            normalizedAccount.profile.copy(name = n.trim, phone = ph.trim, defaultAddress = ad.trim)
          case _ => normalizedAccount.profile
      built <- OrderCheckoutService.buildOrdersForCheckout(products, merchants, platformPromotions, profileForOrders, body.lines, body.voucherId, body.merchantNotes)
      response <- built match
        case Left(message) => IO.raiseError(HttpApiError.BadRequest(message))
        case Right(checkout) =>
          val nextVouchers = checkout.usedVoucher.map(voucher => VoucherRedemptionService.consumeVoucher(normalizedAccount.profile, voucher)).getOrElse(normalizedAccount.profile.vouchers)
          val nextAccount = normalizedAccount.copy(profile =
            normalizedAccount.profile.copy(
              walletBalance = CheckoutPricingService.roundMoney(normalizedAccount.profile.walletBalance - checkout.payableAmount),
              pendingOrders = checkout.orders.reverse ::: normalizedAccount.profile.pendingOrders,
              vouchers = nextVouchers
            )
          )
          for
            _ <- CheckoutInventoryService.inventoryDeductions(products, body.lines).traverse_(CatalogProductTable.upsert(connection, _))
            _ <- checkout.orders.traverse_(OrderTable.upsert(connection, _))
            _ <- persistPromotionUsage(connection, merchants, platformPromotions, checkout.orders)
            _ <- CheckoutRequestTable.insert(connection, username, body, checkout.orders.map(_.id))
            _ <- CustomerProfileTable.upsert(connection, nextAccount)
          yield CheckoutResponse(
            orders = checkout.orders,
            walletBalance = nextAccount.profile.walletBalance,
            originalAmount = checkout.originalAmount,
            discountAmount = checkout.discountAmount,
            payableAmount = checkout.payableAmount,
            usedVoucher = checkout.usedVoucher,
            priceBreakdown = checkout.priceBreakdown
          )
    yield response

  private def persistPromotionUsage(
      connection: Connection,
      merchants: List[Merchant],
      platformPromotions: List[Promotion],
      orders: List[delivery.order.objects.Order]
  ): IO[Unit] =
    val platformIds = platformPromotions.map(_.id).toSet
    val usedPlatformIds = orders.flatMap(_.appliedPromotions.map(_.id)).filter(platformIds.contains).toSet
    val platformUpdate =
      if usedPlatformIds.isEmpty then IO.unit
      else PlatformPromotionTable.set(connection, PromotionUsage.decrement(platformPromotions, usedPlatformIds))

    val merchantUpdates = orders.traverse_ { order =>
      merchants.find(_.id == order.merchantId) match
        case None => IO.unit
        case Some(merchant) =>
          val merchantPromotionIds = merchant.promotions.map(_.id).toSet
          val usedMerchantIds = order.appliedPromotions.map(_.id).filter(merchantPromotionIds.contains).toSet
          if usedMerchantIds.isEmpty then IO.unit
          else MerchantStoreTable.updatePromotions(connection, merchant.copy(promotions = PromotionUsage.decrement(merchant.promotions, usedMerchantIds)))
    }

    platformUpdate >> merchantUpdates

end CheckoutCommandService
