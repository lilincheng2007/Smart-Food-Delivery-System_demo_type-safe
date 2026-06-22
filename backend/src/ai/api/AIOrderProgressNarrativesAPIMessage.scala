package delivery.ai.api

import cats.effect.IO
import delivery.ai.objects.apiTypes.AIOrderProgressNarrativesResponse
import delivery.ai.services.AIOrderProgressNarrativesService
import delivery.platform.api.APIWithRoleMessage

import java.sql.Connection

final case class AIOrderProgressNarrativesAPIMessage() extends APIWithRoleMessage[AIOrderProgressNarrativesResponse]:

  override def plan(connection: Connection, username: String): IO[AIOrderProgressNarrativesResponse] =
    AIOrderProgressNarrativesService.generate()

end AIOrderProgressNarrativesAPIMessage
