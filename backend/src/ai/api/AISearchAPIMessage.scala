package delivery.ai.api

import cats.effect.IO
import delivery.ai.objects.apiTypes.AISearchResponse
import delivery.ai.services.AISearchService
import delivery.ai.utils.OpenAIClient
import delivery.platform.api.{APIWithRoleMessage, HttpApiError}

import java.sql.Connection

final case class AISearchAPIMessage(query: String) extends APIWithRoleMessage[AISearchResponse]:

  override def plan(connection: Connection, username: String): IO[AISearchResponse] =
    val trimmed = query.trim
    if trimmed.isEmpty then IO.raiseError(HttpApiError.BadRequest("搜索内容不能为空"))
    else
      for
        _ <- OpenAIClient.configured.flatMap { ok =>
          if !ok then IO.raiseError(HttpApiError.BadRequest("AI 服务未配置，请联系管理员")) else IO.unit
        }
        response <- AISearchService.search(connection, trimmed)
      yield response

end AISearchAPIMessage
