package delivery.ai.routes

import delivery.ai.api.{AIDietWeeklyReportAPIMessage, AIMerchantBusinessSuggestionsAPIMessage, AIMerchantProductDescriptionsAPIMessage, AIMerchantStoreDescriptionAPIMessage, AIOrderProgressNarrativesAPIMessage, AIReviewSummaryAPIMessage, AISearchAPIMessage}
import delivery.ai.objects.apiTypes.{AIDietWeeklyReportResponse, AIMerchantBusinessSuggestionsResponse, AIMerchantProductDescriptionsResponse, AIMerchantStoreDescriptionResponse, AIOrderProgressNarrativesResponse, AIReviewSummaryResponse, AISearchResponse}
import delivery.shared.api.RegisteredAPIMessage
import delivery.shared.api.RegisteredAPIMessage.apiWithRole
import delivery.shared.json.ApiJsonCodecs.given
import io.circe.generic.auto.*

object AIRoutes:

  val apiMessages: List[RegisteredAPIMessage] = List(
    apiWithRole[AISearchAPIMessage, AISearchResponse]("customer"),
    apiWithRole[AIDietWeeklyReportAPIMessage, AIDietWeeklyReportResponse]("customer"),
    apiWithRole[AIOrderProgressNarrativesAPIMessage, AIOrderProgressNarrativesResponse]("customer"),
    apiWithRole[AIReviewSummaryAPIMessage, AIReviewSummaryResponse]("customer"),
    apiWithRole[AIMerchantBusinessSuggestionsAPIMessage, AIMerchantBusinessSuggestionsResponse]("merchant"),
    apiWithRole[AIMerchantStoreDescriptionAPIMessage, AIMerchantStoreDescriptionResponse]("merchant"),
    apiWithRole[AIMerchantProductDescriptionsAPIMessage, AIMerchantProductDescriptionsResponse]("merchant")
  )

end AIRoutes

