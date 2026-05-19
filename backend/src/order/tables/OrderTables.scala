package delivery.order.tables

object OrderTables:
  val ServiceState = "order_service_state"
  val Orders = "orders"
  val OrderItems = "order_items"
  val CheckoutRequests = "checkout_requests"

  val all: List[String] = List(ServiceState, Orders, OrderItems, CheckoutRequests)

end OrderTables
