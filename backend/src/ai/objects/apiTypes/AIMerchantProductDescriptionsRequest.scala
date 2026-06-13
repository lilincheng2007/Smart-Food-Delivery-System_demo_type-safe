package delivery.ai.objects.apiTypes

import delivery.domain.MerchantId

final case class AIMerchantProductDescriptionsRequest(merchantId: MerchantId, keywords: String)
