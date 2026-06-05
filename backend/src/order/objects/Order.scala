package delivery.order.objects

import delivery.shared.objects.{MerchantId, OrderId, OrderStatus, RefundStatus, RiderId, UserId, Voucher}

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
    pointsAwarded: Int = 0,
    refundStatus: Option[RefundStatus] = None,
    refundReason: Option[String] = None,
    refundImageUrl: Option[String] = None,
    refundRequestedAt: Option[String] = None,
    refundMerchantReason: Option[String] = None,
    refundMerchantReviewedAt: Option[String] = None,
    refundAdminReason: Option[String] = None,
    refundedAt: Option[String] = None,
    customerNoteText: Option[String] = None,
    customerNoteImageUrl: Option[String] = None
)
