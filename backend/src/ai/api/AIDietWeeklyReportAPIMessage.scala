package delivery.ai.api

import cats.effect.IO
import delivery.ai.objects.apiTypes.AIDietWeeklyReportResponse
import delivery.ai.services.AIDietWeeklyReportService
import delivery.ai.utils.OpenAIClient
import delivery.platform.api.{APIWithRoleMessage, HttpApiError}

import java.sql.Connection

final case class AIDietWeeklyReportAPIMessage() extends APIWithRoleMessage[AIDietWeeklyReportResponse]:

  override def plan(connection: Connection, username: String): IO[AIDietWeeklyReportResponse] =
    for
      _ <- OpenAIClient.configured.flatMap { ok =>
        if !ok then IO.raiseError(HttpApiError.BadRequest("AI 服务未配置，请联系管理员")) else IO.unit
      }
      response <- AIDietWeeklyReportService.generate(connection, username)
    yield response

end AIDietWeeklyReportAPIMessage
