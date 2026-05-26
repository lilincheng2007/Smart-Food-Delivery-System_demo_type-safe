package delivery.order.tables

import cats.effect.IO
import cats.syntax.all.*
import delivery.order.tables.checkoutrequest.CheckoutRequestTableInitializer
import delivery.order.tables.order.OrderTableInitializer
import delivery.order.tables.orderitem.OrderItemTableInitializer

import java.sql.Connection

object OrderTables:
  val ServiceState = "order_service_state"
  val Orders = "orders"
  val OrderItems = "order_items"
  val CheckoutRequests = "checkout_requests"

  val all: List[String] = List(ServiceState, Orders, OrderItems, CheckoutRequests)

  def initialize(connection: Connection): IO[Unit] =
    List(
      OrderTableInitializer.initialize(connection),
      OrderItemTableInitializer.initialize(connection),
      CheckoutRequestTableInitializer.initialize(connection)
    ).sequence_.void

end OrderTables
