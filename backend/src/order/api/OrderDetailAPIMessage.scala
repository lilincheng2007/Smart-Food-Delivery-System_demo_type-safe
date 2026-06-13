package delivery.order.api

import cats.effect.IO
import delivery.order.objects.Order
import delivery.order.tables.order.OrderTable
import delivery.order.utils.OrderApiSupport
import delivery.platform.api.{APIWithRoleMessage, HttpApiError}
import delivery.domain.OrderId
import delivery.user.tables.customerprofile.CustomerProfileTable

import java.sql.Connection

final case class OrderDetailAPIMessage(orderId: OrderId) extends APIWithRoleMessage[Order]:
  override def plan(connection: Connection, username: String): IO[Order] =
    for
      account <- CustomerProfileTable.findByUsername(connection, username)
      order <- OrderTable.findById(connection, orderId)
      output <- (account, order) match
        case (None, _) => IO.raiseError(HttpApiError.NotFound(OrderApiSupport.customerNotFound.error))
        case (Some(value), Some(found)) if found.customerId == value.profile.id => IO.pure(found)
        case (Some(value), None) =>
          value.profile.pendingOrders
            .find(_.id == orderId)
            .orElse(value.profile.historyOrders.find(_.id == orderId)) match
            case Some(found) => IO.pure(found)
            case None        => IO.raiseError(HttpApiError.NotFound("未找到订单"))
        case (Some(_), Some(_)) => IO.raiseError(HttpApiError.NotFound("未找到订单"))
    yield output
