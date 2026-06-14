package delivery.order.objects.apiTypes

import delivery.order.objects.OrderPriceBreakdown
import delivery.promotion.objects.Voucher

final case class CheckoutQuoteResponse(
    canCheckout: Boolean,
    failureReason: Option[String],
    walletBalance: Double,
    originalAmount: Double,
    discountAmount: Double,
    payableAmount: Double,
    usedVoucher: Option[Voucher],
    priceBreakdown: OrderPriceBreakdown
)
