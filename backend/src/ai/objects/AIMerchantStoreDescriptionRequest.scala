package delivery.ai.objects

import delivery.shared.objects.MerchantId

final case class AIMerchantStoreDescriptionRequest(merchantId: MerchantId, keywords: String)
