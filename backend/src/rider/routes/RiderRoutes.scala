package delivery.rider.routes

import delivery.rider.api.*
import delivery.rider.objects.{RiderAvailableOrdersResponse, RiderDeliverySettlement, RiderMeResponse, RiderTimeoutCardRedeemResponse, RiderUseTimeoutCardResponse}
import delivery.shared.api.RegisteredAPIMessage
import delivery.shared.api.RegisteredAPIMessage.apiWithRole
import delivery.shared.json.ApiJsonCodecs.given
import delivery.shared.objects.OkResponse
import io.circe.generic.auto.*

object RiderRoutes:

  val apiMessages: List[RegisteredAPIMessage] = List(
    apiWithRole[RiderMeAPIMessage, RiderMeResponse]("rider"),
    apiWithRole[RiderAvailableOrdersAPIMessage, RiderAvailableOrdersResponse]("rider"),
    apiWithRole[RiderGrabOrderAPIMessage, OkResponse]("rider"),
    apiWithRole[RiderUpdateOrderStatusAPIMessage, RiderDeliverySettlement]("rider"),
    apiWithRole[RiderRedeemTimeoutCardAPIMessage, RiderTimeoutCardRedeemResponse]("rider"),
    apiWithRole[RiderUseTimeoutCardAPIMessage, RiderUseTimeoutCardResponse]("rider")
  )

end RiderRoutes
