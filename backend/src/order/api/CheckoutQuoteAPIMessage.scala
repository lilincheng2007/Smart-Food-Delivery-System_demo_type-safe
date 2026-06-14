package delivery.order.api

import cats.effect.IO
import delivery.admin.tables.platformpromotion.PlatformPromotionTable
import delivery.merchant.tables.catalogproduct.CatalogProductTable
import delivery.merchant.tables.merchantstore.MerchantStoreTable
import delivery.order.objects.apiTypes.{CheckoutQuoteResponse, CheckoutRequest, OrderMerchantNote}
import delivery.order.services.{CheckoutPricingService, OrderCheckoutService}
import delivery.platform.api.{APIWithRoleMessage, HttpApiError}
import delivery.domain.VoucherId
import delivery.promotion.services.StandardPlatformVoucherService
import delivery.user.tables.customerprofile.CustomerProfileTable

import java.sql.Connection

final case class CheckoutQuoteAPIMessage(
    lines: List[delivery.order.objects.CheckoutLine],
    customerName: Option[String],
    customerPhone: Option[String],
    deliveryAddress: Option[String],
    voucherId: Option[VoucherId],
    merchantNotes: List[OrderMerchantNote] = Nil
) extends APIWithRoleMessage[CheckoutQuoteResponse]:
  override def plan(connection: Connection, username: String): IO[CheckoutQuoteResponse] =
    val body = CheckoutRequest(lines, customerName, customerPhone, deliveryAddress, voucherId, merchantNotes)
    for
      account <- CustomerProfileTable.findByUsername(connection, username).flatMap {
        case Some(value) => IO.pure(value)
        case None        => IO.raiseError(HttpApiError.NotFound("未找到顾客"))
      }
      normalizedAccount = account.copy(profile = account.profile.copy(vouchers = StandardPlatformVoucherService.mergeStandardPlatformVouchers(account.profile.id, account.profile.vouchers)))
      products <- CatalogProductTable.listForUpdate(connection)
      merchants <- MerchantStoreTable.listCatalog(connection)
      platformPromotions <- PlatformPromotionTable.get(connection)
      profileForQuote =
        (body.customerName, body.customerPhone, body.deliveryAddress) match
          case (Some(n), Some(ph), Some(ad))
              if n.trim.nonEmpty && ph.trim.nonEmpty && ad.trim.nonEmpty =>
            normalizedAccount.profile.copy(name = n.trim, phone = ph.trim, defaultAddress = ad.trim)
          case _ => normalizedAccount.profile
      built <- OrderCheckoutService.buildOrdersForCheckout(products, merchants, platformPromotions, profileForQuote, body.lines, body.voucherId, body.merchantNotes)
      response = built match
        case Left(message) =>
          CheckoutQuoteResponse(
            canCheckout = false,
            failureReason = Some(message),
            walletBalance = normalizedAccount.profile.walletBalance,
            originalAmount = 0,
            discountAmount = 0,
            payableAmount = 0,
            usedVoucher = None,
            priceBreakdown = CheckoutPricingService.priceBreakdown(
              productOriginalAmount = 0,
              merchantDiscountAmount = 0,
              voucherDiscountAmount = 0,
              platformDiscountAmount = 0,
              deliveryFeeAmount = 0,
              payableAmount = 0
            )
          )
        case Right(checkout) =>
          CheckoutQuoteResponse(
            canCheckout = true,
            failureReason = None,
            walletBalance = normalizedAccount.profile.walletBalance,
            originalAmount = checkout.originalAmount,
            discountAmount = checkout.discountAmount,
            payableAmount = checkout.payableAmount,
            usedVoucher = checkout.usedVoucher,
            priceBreakdown = checkout.priceBreakdown
          )
    yield response
