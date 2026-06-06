package delivery.order.api

import cats.effect.IO
import cats.syntax.all.*
import delivery.admin.tables.platformpromotion.PlatformPromotionTable
import delivery.merchant.tables.catalogproduct.CatalogProductTable
import delivery.merchant.tables.merchantstore.MerchantStoreTable
import delivery.order.objects.{CheckoutLine}
import delivery.order.objects.apiTypes.{CheckoutRequest, CheckoutResponse, OrderMerchantNote}
import delivery.order.tables.checkoutrequest.CheckoutRequestTable
import delivery.order.tables.order.OrderTable
import delivery.order.utils.OrderApiSupport
import delivery.shared.api.{APIWithRoleMessage, HttpApiError}
import delivery.shared.objects.VoucherId
import delivery.shared.utils.{PromotionUsage, VoucherSupport}
import delivery.user.tables.customerprofile.CustomerProfileTable

import java.sql.Connection

final case class CheckoutAPIMessage(
    lines: List[CheckoutLine],
    customerName: Option[String],
    customerPhone: Option[String],
    deliveryAddress: Option[String],
    voucherId: Option[VoucherId],
    merchantNotes: List[OrderMerchantNote] = Nil
) extends APIWithRoleMessage[CheckoutResponse]:
  override def plan(connection: Connection, username: String): IO[CheckoutResponse] =
    val body = CheckoutRequest(lines, customerName, customerPhone, deliveryAddress, voucherId, merchantNotes)
    for
      account <- CustomerProfileTable.findByUsername(connection, username).flatMap {
        case Some(value) => IO.pure(value)
        case None        => IO.raiseError(HttpApiError.NotFound(OrderApiSupport.customerNotFound.error))
      }
      normalizedAccount = account.copy(profile = account.profile.copy(vouchers = VoucherSupport.mergeStandardPlatformVouchers(account.profile.id, account.profile.vouchers)))
      products <- CatalogProductTable.list(connection)
      merchants <- MerchantStoreTable.listCatalog(connection)
      platformPromotions <- PlatformPromotionTable.get(connection)
      profileForOrders =
        (body.customerName, body.customerPhone, body.deliveryAddress) match
          case (Some(n), Some(ph), Some(ad))
              if n.trim.nonEmpty && ph.trim.nonEmpty && ad.trim.nonEmpty =>
            normalizedAccount.profile.copy(name = n.trim, phone = ph.trim, defaultAddress = ad.trim)
          case _ => normalizedAccount.profile
      built <- OrderAPIMessageSupport.buildOrdersForCheckout(products, merchants, platformPromotions, profileForOrders, body.lines.map(OrderApiSupport.normalizeLine), body.voucherId, body.merchantNotes)
      result <- built match
        case Left(msg) => IO.raiseError(HttpApiError.BadRequest(msg))
        case Right(checkout) =>
          val nextVouchers = checkout.usedVoucher.map(voucher => OrderAPIMessageSupport.consumeVoucher(normalizedAccount.profile, voucher)).getOrElse(normalizedAccount.profile.vouchers)
          val nextAccount = normalizedAccount.copy(profile =
            normalizedAccount.profile.copy(
              walletBalance = OrderAPIMessageSupport.roundMoney(normalizedAccount.profile.walletBalance - checkout.payableAmount),
              pendingOrders = checkout.orders.reverse ::: normalizedAccount.profile.pendingOrders,
              vouchers = nextVouchers
            )
          )
          for
            _ <- checkout.orders.traverse_(OrderTable.upsert(connection, _))
            _ <- persistPromotionUsage(connection, username, merchants, platformPromotions, checkout.orders)
            _ <- CheckoutRequestTable.insert(connection, username, body, checkout.orders.map(_.id))
            _ <- CustomerProfileTable.upsert(connection, nextAccount)
          yield CheckoutResponse(
            orders = checkout.orders,
            walletBalance = nextAccount.profile.walletBalance,
            originalAmount = checkout.originalAmount,
            discountAmount = checkout.discountAmount,
            payableAmount = checkout.payableAmount,
            usedVoucher = checkout.usedVoucher
          )
    yield result

  private def persistPromotionUsage(
      connection: Connection,
      username: String,
      merchants: List[delivery.merchant.objects.Merchant],
      platformPromotions: List[delivery.shared.objects.Promotion],
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
