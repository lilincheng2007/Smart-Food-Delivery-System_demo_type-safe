package delivery.review.api

import cats.effect.IO
import delivery.review.services.ReviewSubmissionService
import delivery.platform.api.APIWithRoleMessage
import delivery.domain.OrderId
import delivery.domain.apiTypes.OkResponse

import java.sql.Connection

final case class CustomerSubmitOrderReviewAPIMessage(
    orderId: OrderId,
    merchantRating: Int,
    merchantDescription: String,
    merchantImageUrl: Option[String],
    riderRating: Option[Int]
) extends APIWithRoleMessage[OkResponse]:
  override def plan(connection: Connection, username: String): IO[OkResponse] =
    val input = ReviewSubmissionService.SubmitOrderReviewInput(
      orderId = orderId,
      merchantRating = merchantRating,
      merchantDescription = merchantDescription,
      merchantImageUrl = merchantImageUrl,
      riderRating = riderRating
    )
    ReviewSubmissionService.submitOrderReview(connection, username, input).as(OkResponse(ok = true))
