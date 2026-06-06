package delivery.ai.objects.apiTypes

import delivery.shared.objects.MerchantId

final case class AIMerchantBusinessSuggestionsResponse(
    merchantId: MerchantId,
    summary: String,
    suggestions: List[String],
    generatedAt: String
)

