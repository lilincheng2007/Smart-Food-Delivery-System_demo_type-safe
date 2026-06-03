package delivery.merchant.api

import cats.effect.IO
import delivery.order.api.OrderAPIMessageSupport
import delivery.order.tables.order.OrderTable
import delivery.shared.api.{APIWithRoleMessage, HttpApiError}
import delivery.shared.objects.{OrderId, OrderStatus}
import delivery.shared.objects.apiTypes.OkResponse
import delivery.user.tables.customerprofile.CustomerProfileTable

import java.sql.Connection

final case class MerchantOrderRejectAPIMessage(orderId: OrderId) extends APIWithRoleMessage[OkResponse]:
  override def plan(connection: Connection, username: String): IO[OkResponse] =
    for
      order <- OrderTable.findById(connection, orderId).flatMap {
        case Some(value) => IO.pure(value)
        case None        => IO.raiseError(HttpApiError.BadRequest("未找到订单"))
      }
      _ <- MerchantAPIMessageSupport.requireOwnedStore(connection, username, order.merchantId)
      _ <-
        if MerchantAPIMessageSupport.canRejectOrder(order.status) then IO.unit
        else IO.raiseError(HttpApiError.BadRequest(s"当前状态不可拒收：${order.status}"))
      account <- CustomerProfileTable.findById(connection, order.customerId).flatMap {
        case Some(value) => IO.pure(value)
        case None        => IO.raiseError(HttpApiError.BadRequest("未找到顾客账号"))
      }
      refundAmount = if order.payableAmount > 0 then order.payableAmount else order.totalAmount
      canceledOrder = order.copy(status = OrderStatus.已取消)
      nextAccount = account.copy(profile = account.profile.copy(walletBalance = OrderAPIMessageSupport.roundMoney(account.profile.walletBalance + refundAmount)))
      _ <- OrderTable.upsert(connection, canceledOrder)
      _ <- CustomerProfileTable.upsert(connection, nextAccount)
    yield OkResponse(ok = true)
