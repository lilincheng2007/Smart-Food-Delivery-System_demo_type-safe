package delivery.ai.routes

import delivery.ai.api.AISearchAPIMessage
import delivery.ai.objects.AISearchResponse
import delivery.shared.api.RegisteredAPIMessage
import delivery.shared.api.RegisteredAPIMessage.apiWithRole
import delivery.shared.json.ApiJsonCodecs.given
import io.circe.generic.auto.*

object AIRoutes:

  val apiMessages: List[RegisteredAPIMessage] = List(
    apiWithRole[AISearchAPIMessage, AISearchResponse]("customer")
  )

end AIRoutes
