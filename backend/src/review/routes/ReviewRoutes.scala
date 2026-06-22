package delivery.review.routes

import cats.effect.IO
import delivery.review.api.*
import delivery.review.objects.*
import delivery.review.objects.apiTypes.*
import delivery.platform.api.RegisteredAPIMessage
import delivery.platform.api.RegisteredAPIMessage.{api, apiWithRole}
import delivery.platform.json.ApiJsonCodecs.given
import delivery.domain.apiTypes.OkResponse

object ReviewRoutes:
  val apiMessages: List[RegisteredAPIMessage] = List(
    api[MerchantReviewsAPIMessage, MerchantReviewsResponse],
    apiWithRole[CustomerSubmitOrderReviewAPIMessage, OkResponse]("customer"),
    apiWithRole[CustomerReviewVoteAPIMessage, OkResponse]("customer"),
    apiWithRole[MerchantReviewReplyAPIMessage, OkResponse]("merchant"),
    apiWithRole[CustomerReviewImageFileAPIMessage, String]("customer")
  )

end ReviewRoutes
