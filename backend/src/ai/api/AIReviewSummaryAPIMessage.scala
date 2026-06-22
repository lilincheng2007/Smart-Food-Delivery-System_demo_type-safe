package delivery.ai.api

import cats.effect.IO
import delivery.ai.objects.apiTypes.AIReviewSummaryResponse
import delivery.ai.services.AIReviewSummaryService
import delivery.ai.utils.OpenAIClient
import delivery.platform.api.{APIWithRoleMessage, HttpApiError}
import delivery.domain.MerchantId

import java.sql.Connection

final case class AIReviewSummaryAPIMessage(merchantId: MerchantId) extends APIWithRoleMessage[AIReviewSummaryResponse]:

  override def plan(connection: Connection, username: String): IO[AIReviewSummaryResponse] =
    for
      _ <- OpenAIClient.configured.flatMap { ok =>
        if !ok then IO.raiseError(HttpApiError.BadRequest("AI 服务未配置，请联系管理员")) else IO.unit
      }
      response <- AIReviewSummaryService.generate(connection, merchantId)
    yield response

end AIReviewSummaryAPIMessage
