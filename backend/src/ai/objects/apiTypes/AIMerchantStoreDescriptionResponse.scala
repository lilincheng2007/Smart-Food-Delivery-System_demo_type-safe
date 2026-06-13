package delivery.ai.objects.apiTypes

import delivery.domain.MerchantId

final case class AIMerchantStoreDescriptionResponse(
    merchantId: MerchantId,
    description: String,
    generatedAt: String
)
