package delivery.order.api

import delivery.order.services.{OrderCheckoutService, OrderStatusTransitionService}
import cats.effect.IO
import delivery.order.objects.apiTypes.OrderCancelResponse
import delivery.order.tables.order.OrderTable
import delivery.platform.api.{APIWithRoleMessage, HttpApiError}
import delivery.domain.{OrderId, OrderStatus}
import delivery.user.tables.customerprofile.CustomerProfileTable

import java.sql.Connection

final case class OrderCancelAPIMessage(orderId: OrderId) extends APIWithRoleMessage[OrderCancelResponse]:
  override def plan(connection: Connection, username: String): IO[OrderCancelResponse] =
    for
      account <- CustomerProfileTable.findByUsername(connection, username).flatMap {
        case Some(value) => IO.pure(value)
        case None        => IO.raiseError(HttpApiError.BadRequest("未找到顾客账号"))
      }
      order <- OrderTable.findById(connection, orderId).flatMap {
        case Some(value) => IO.pure(value)
        case None        => IO.raiseError(HttpApiError.BadRequest("未找到订单"))
      }
      _ <-
        if order.customerId != account.profile.id then IO.raiseError(HttpApiError.BadRequest("无权操作该订单"))
        else IO.unit
      refundAmount = if order.payableAmount > 0 then order.payableAmount else order.totalAmount
      canceledOrder <- OrderStatusTransitionService.transition(connection, order, OrderStatus.已取消, actorRole = "customer")
      nextAccount = account.copy(profile = account.profile.copy(walletBalance = OrderCheckoutService.roundMoney(account.profile.walletBalance + refundAmount)))
      _ <- CustomerProfileTable.upsert(connection, nextAccount)
    yield OrderCancelResponse(canceledOrder, nextAccount.profile.walletBalance)
