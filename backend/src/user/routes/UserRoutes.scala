package delivery.user.routes

import delivery.platform.api.RegisteredAPIMessage
import delivery.platform.api.RegisteredAPIMessage.{api, apiWithRole}
import delivery.platform.json.ApiJsonCodecs.given
import delivery.domain.apiTypes.OkResponse
import delivery.user.api.*
import delivery.user.objects.apiTypes.{CustomerMeResponse, CustomerWalletTopUpResponse, LoginResponse}

object UserRoutes:

  val apiMessages: List[RegisteredAPIMessage] = List(
    api[LoginAPIMessage, LoginResponse],
    api[RegisterAPIMessage, OkResponse],
    apiWithRole[CustomerMeAPIMessage, CustomerMeResponse]("customer"),
    apiWithRole[CustomerProfilePatchAPIMessage, OkResponse]("customer"),
    apiWithRole[CustomerVoucherDiscardAPIMessage, OkResponse]("customer"),
    apiWithRole[CustomerRechargeAPIMessage, CustomerWalletTopUpResponse]("customer")
  )

end UserRoutes
