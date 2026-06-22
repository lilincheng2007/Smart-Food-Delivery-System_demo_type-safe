package delivery.ai.api

import cats.effect.IO
import delivery.ai.objects.apiTypes.AIMerchantBusinessSuggestionsResponse
import delivery.ai.services.AIMerchantBusinessSuggestionsService
import delivery.ai.utils.OpenAIClient
import delivery.platform.api.{APIWithRoleMessage, HttpApiError}
import delivery.domain.MerchantId

import java.sql.Connection

final case class AIMerchantBusinessSuggestionsAPIMessage(merchantId: MerchantId) extends APIWithRoleMessage[AIMerchantBusinessSuggestionsResponse]:

  override def plan(connection: Connection, username: String): IO[AIMerchantBusinessSuggestionsResponse] =
    for
      _ <- OpenAIClient.configured.flatMap { ok =>
        if !ok then IO.raiseError(HttpApiError.BadRequest("AI 服务未配置，请联系管理员")) else IO.unit
      }
      response <- AIMerchantBusinessSuggestionsService.generate(connection, username, merchantId)
    yield response

end AIMerchantBusinessSuggestionsAPIMessage
