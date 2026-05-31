package delivery.order.routes

import delivery.order.api.*
import delivery.order.objects.{CheckoutResponse, CustomerOrdersResponse, Order, OrderCancelResponse}
import delivery.shared.api.RegisteredAPIMessage
import delivery.shared.api.RegisteredAPIMessage.apiWithRole
import delivery.shared.json.ApiJsonCodecs.given
import io.circe.generic.auto.*

object OrderRoutes:

  val apiMessages: List[RegisteredAPIMessage] = List(
    apiWithRole[CustomerOrdersAPIMessage, CustomerOrdersResponse]("customer"),
    apiWithRole[OrderDetailAPIMessage, Order]("customer"),
    apiWithRole[OrderCancelAPIMessage, OrderCancelResponse]("customer"),
    apiWithRole[OrderCompleteAPIMessage, Order]("customer"),
    apiWithRole[CheckoutAPIMessage, CheckoutResponse]("customer")
  )

end OrderRoutes
