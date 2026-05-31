package delivery.ai.objects

import delivery.shared.objects.ProductId

final case class AIGeneratedProductDescription(
    productId: ProductId,
    productName: String,
    description: String
)
