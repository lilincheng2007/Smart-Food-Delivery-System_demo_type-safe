package delivery.rider.routes

import delivery.rider.api.*
import delivery.rider.objects.{RiderDeliverySettlement}
import delivery.rider.objects.apiTypes.{RiderAvailableOrdersResponse, RiderMeResponse, RiderTimeoutCardRedeemResponse, RiderUseTimeoutCardResponse}
import delivery.platform.api.RegisteredAPIMessage
import delivery.platform.api.RegisteredAPIMessage.apiWithRole
import delivery.platform.json.ApiJsonCodecs.given
import delivery.domain.apiTypes.OkResponse
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
