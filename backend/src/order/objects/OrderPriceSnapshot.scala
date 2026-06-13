package delivery.order.objects

import delivery.domain.ProductId

final case class OrderPriceSnapshotItem(
    productId: ProductId,
    name: String,
    unitPrice: Double,
    quantity: Int,
    lineAmount: Double
)

final case class OrderPriceSnapshot(
    items: List[OrderPriceSnapshotItem],
    productOriginalAmount: Double,
    merchantDiscountAmount: Double,
    voucherDiscountAmount: Double,
    platformDiscountAmount: Double,
    deliveryFeeAmount: Double,
    discountAmount: Double,
    payableAmount: Double,
    merchantReceivableAmount: Double,
    appliedPromotionTitles: List[String] = Nil,
    usedVoucherTitle: Option[String] = None
)
