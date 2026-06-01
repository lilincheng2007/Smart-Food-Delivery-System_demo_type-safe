package delivery.order.objects

import delivery.shared.objects.{MerchantId, OrderId, OrderStatus, RiderId, UserId, Voucher}

final case class Order(
    id: OrderId,
    customerId: UserId,
    customerName: String,
    customerPhone: String,
    merchantId: MerchantId,
    riderId: Option[RiderId],
    items: List[OrderItem],
    totalAmount: Double,
    deliveryAddress: String,
    status: OrderStatus,
    placedAt: String,
    originalAmount: Double = 0,
    discountAmount: Double = 0,
    payableAmount: Double = 0,
    usedVoucher: Option[Voucher] = None,
    pointsAwarded: Int = 0
)
