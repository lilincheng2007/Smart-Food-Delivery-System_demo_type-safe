package delivery.ai.objects

import delivery.shared.objects.MerchantId

final case class AIMerchantProductDescriptionsRequest(merchantId: MerchantId, keywords: String)
