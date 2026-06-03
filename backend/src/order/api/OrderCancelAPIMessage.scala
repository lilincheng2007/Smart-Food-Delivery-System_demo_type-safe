package delivery.order.api

import cats.effect.IO
import delivery.order.objects.apiTypes.OrderCancelResponse
import delivery.order.tables.order.OrderTable
import delivery.shared.api.{APIWithRoleMessage, HttpApiError}
import delivery.shared.objects.{OrderId, OrderStatus}
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
        else if order.status == OrderStatus.已取消 then IO.raiseError(HttpApiError.BadRequest("订单已取消"))
        else if order.status != OrderStatus.待商家接单 then IO.raiseError(HttpApiError.BadRequest("商家接单后订单不可取消"))
        else IO.unit
      refundAmount = if order.payableAmount > 0 then order.payableAmount else order.totalAmount
      canceledOrder = order.copy(status = OrderStatus.已取消)
      nextAccount = account.copy(profile = account.profile.copy(walletBalance = OrderAPIMessageSupport.roundMoney(account.profile.walletBalance + refundAmount)))
      _ <- OrderTable.upsert(connection, canceledOrder)
      _ <- CustomerProfileTable.upsert(connection, nextAccount)
    yield OrderCancelResponse(canceledOrder, nextAccount.profile.walletBalance)
