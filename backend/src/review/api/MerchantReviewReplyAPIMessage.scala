package delivery.review.api

import cats.effect.IO
import delivery.merchant.utils.MerchantApiSupport
import delivery.review.tables.MerchantReviewTable
import delivery.platform.api.{APIWithRoleMessage, HttpApiError}
import delivery.domain.MerchantId
import delivery.domain.apiTypes.OkResponse

import java.sql.Connection

final case class MerchantReviewReplyAPIMessage(reviewId: String, merchantId: MerchantId, replyContent: String) extends APIWithRoleMessage[OkResponse]:
  override def plan(connection: Connection, username: String): IO[OkResponse] =
    val trimmed = replyContent.trim
    if trimmed.isEmpty then IO.raiseError(HttpApiError.BadRequest("回复内容不能为空"))
    else if trimmed.length > 500 then IO.raiseError(HttpApiError.BadRequest("回复内容不能超过 500 个字符"))
    else
      for
        _ <- MerchantApiSupport.requireOwnedStore(connection, username, merchantId)
        updated <- MerchantReviewTable.reply(connection, reviewId, merchantId, trimmed)
        _ <- if updated then IO.unit else IO.raiseError(HttpApiError.BadRequest("未找到该店铺评价"))
      yield OkResponse(ok = true)
