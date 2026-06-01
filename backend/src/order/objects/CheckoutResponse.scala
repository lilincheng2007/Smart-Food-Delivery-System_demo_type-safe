package delivery.order.objects

import delivery.shared.objects.Voucher

final case class CheckoutResponse(
    orders: List[Order],
    walletBalance: Double,
    originalAmount: Double,
    discountAmount: Double,
    payableAmount: Double,
    usedVoucher: Option[Voucher]
)
