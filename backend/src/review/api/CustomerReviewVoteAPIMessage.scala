package delivery.review.api

import cats.effect.IO
import delivery.review.tables.MerchantReviewVoteTable
import delivery.platform.api.{APIWithRoleMessage, HttpApiError}
import delivery.domain.apiTypes.OkResponse
import delivery.user.tables.customerprofile.CustomerProfileTable

import java.sql.Connection

final case class CustomerReviewVoteAPIMessage(reviewId: String, vote: String) extends APIWithRoleMessage[OkResponse]:
  override def plan(connection: Connection, username: String): IO[OkResponse] =
    val normalized = vote.trim.toLowerCase
    for
      customer <- CustomerProfileTable.findByUsername(connection, username).flatMap {
        case Some(value) => IO.pure(value)
        case None        => IO.raiseError(HttpApiError.BadRequest("未找到顾客账号"))
      }
      _ <-
        if normalized != "up" && normalized != "down" && normalized != "none" then IO.raiseError(HttpApiError.BadRequest("投票类型必须为 up、down 或 none"))
        else MerchantReviewVoteTable.setVote(connection, reviewId, customer.profile.id, normalized)
    yield OkResponse(ok = true)
