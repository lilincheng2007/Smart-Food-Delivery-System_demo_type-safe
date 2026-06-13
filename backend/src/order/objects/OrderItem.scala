package delivery.order.objects

import delivery.domain.ProductId

final case class OrderItem(
    productId: ProductId,
    name: String,
    unitPrice: Double,
    quantity: Int
)
