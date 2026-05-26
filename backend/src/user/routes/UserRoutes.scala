package delivery.user.routes

import delivery.shared.api.RegisteredAPIMessage
import delivery.shared.api.RegisteredAPIMessage.{api, apiWithRole}
import delivery.shared.json.ApiJsonCodecs.given
import delivery.shared.objects.OkResponse
import delivery.user.api.*
import delivery.user.objects.{CustomerMeResponse, LoginResponse}
import io.circe.generic.auto.*

object UserRoutes:

  val apiMessages: List[RegisteredAPIMessage] = List(
    api[LoginAPIMessage, LoginResponse],
    api[RegisterAPIMessage, OkResponse],
    apiWithRole[CustomerMeAPIMessage, CustomerMeResponse]("customer"),
    apiWithRole[CustomerProfilePatchAPIMessage, OkResponse]("customer")
  )

end UserRoutes
