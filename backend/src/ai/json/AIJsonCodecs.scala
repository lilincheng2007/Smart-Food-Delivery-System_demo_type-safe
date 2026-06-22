package delivery.ai.json

import delivery.ai.api.*
import delivery.ai.objects.*
import delivery.ai.objects.apiTypes.*
import delivery.merchant.json.MerchantJsonCodecs.given
import delivery.platform.json.CommonJsonCodecs.given
import delivery.review.json.ReviewJsonCodecs.given
import io.circe.Codec
import io.circe.generic.semiauto.*

object AIJsonCodecs:

  given Codec[AISearchAPIMessage] = deriveCodec
  given Codec[AIDietWeeklyReportAPIMessage] = deriveCodec
  given Codec[AIOrderProgressNarrativesAPIMessage] = deriveCodec
  given Codec[AIReviewSummaryAPIMessage] = deriveCodec
  given Codec[AIMerchantBusinessSuggestionsAPIMessage] = deriveCodec
  given Codec[AIMerchantStoreDescriptionAPIMessage] = deriveCodec
  given Codec[AIMerchantProductDescriptionsAPIMessage] = deriveCodec

  given Codec[AIRecommendedProduct] = deriveCodec
  given Codec[AIRecommendedMerchant] = deriveCodec
  given Codec[AISearchResponse] = deriveCodec
  given Codec[AISearchRequest] = deriveCodec

  given Codec[AIDietWeeklyReportRequest] = deriveCodec
  given Codec[DietNutritionItem] = deriveCodec
  given Codec[DietWeeklySummary] = deriveCodec
  given Codec[AIDietWeeklyReportResponse] = deriveCodec

  given Codec[AIOrderProgressNarrativesRequest] = deriveCodec
  given Codec[AIOrderProgressNarrativeGroup] = deriveCodec
  given Codec[AIOrderProgressNarrativesResponse] = deriveCodec

  given Codec[AIMerchantStoreDescriptionRequest] = deriveCodec
  given Codec[AIMerchantStoreDescriptionResponse] = deriveCodec
  given Codec[AIMerchantBusinessSuggestionsResponse] = deriveCodec
  given Codec[AIMerchantProductDescriptionsRequest] = deriveCodec
  given Codec[AIGeneratedProductDescription] = deriveCodec
  given Codec[AIMerchantProductDescriptionsResponse] = deriveCodec
  given Codec[AIReviewSummaryResponse] = deriveCodec

end AIJsonCodecs
