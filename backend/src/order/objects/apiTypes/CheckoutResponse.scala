package delivery.order.objects.apiTypes

import delivery.order.objects.Order
import delivery.domain.Voucher

final case class CheckoutResponse(
    orders: List[Order],
    walletBalance: Double,
    originalAmount: Double,
    discountAmount: Double,
    payableAmount: Double,
    usedVoucher: Option[Voucher],
    priceBreakdown: delivery.order.objects.OrderPriceBreakdown
)
