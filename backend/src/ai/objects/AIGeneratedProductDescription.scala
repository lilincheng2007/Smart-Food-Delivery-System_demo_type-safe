package delivery.ai.objects

import delivery.domain.ProductId

final case class AIGeneratedProductDescription(
    productId: ProductId,
    productName: String,
    description: String
)
