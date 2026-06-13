package delivery.ai.objects.apiTypes

import delivery.domain.MerchantId

final case class AIMerchantBusinessSuggestionsResponse(
    merchantId: MerchantId,
    summary: String,
    suggestions: List[String],
    generatedAt: String
)

