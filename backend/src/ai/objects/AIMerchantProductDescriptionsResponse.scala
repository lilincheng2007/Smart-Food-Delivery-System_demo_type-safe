package delivery.ai.objects

import delivery.shared.objects.MerchantId

final case class AIMerchantProductDescriptionsResponse(
    merchantId: MerchantId,
    products: List[AIGeneratedProductDescription],
    generatedAt: String
)
