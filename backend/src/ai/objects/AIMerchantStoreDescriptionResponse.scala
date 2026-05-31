package delivery.ai.objects

import delivery.shared.objects.MerchantId

final case class AIMerchantStoreDescriptionResponse(
    merchantId: MerchantId,
    description: String,
    generatedAt: String
)
