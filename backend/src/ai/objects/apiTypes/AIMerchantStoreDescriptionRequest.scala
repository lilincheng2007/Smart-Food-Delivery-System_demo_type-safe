package delivery.ai.objects.apiTypes

import delivery.domain.MerchantId

final case class AIMerchantStoreDescriptionRequest(merchantId: MerchantId, keywords: String)
