package delivery.ai.objects.apiTypes

final case class AIReviewSummaryResponse(
    merchantId: String,
    storeName: String,
    summary: String,
    highlights: List[String],
    reviewCount: Int
)
