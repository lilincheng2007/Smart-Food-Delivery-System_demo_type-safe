package delivery.merchant.api

import delivery.merchant.services.MerchantBusinessService
import delivery.order.services.OrderCheckoutService
import cats.effect.IO
import delivery.order.api.OrderStatusTransitionService
import delivery.order.tables.order.OrderTable
import delivery.platform.api.{APIWithRoleMessage, HttpApiError}
import delivery.domain.{OrderId, OrderStatus}
import delivery.domain.apiTypes.OkResponse
import delivery.user.tables.customerprofile.CustomerProfileTable

import java.sql.Connection

final case class MerchantOrderRejectAPIMessage(orderId: OrderId) extends APIWithRoleMessage[OkResponse]:
  override def plan(connection: Connection, username: String): IO[OkResponse] =
    for
      order <- OrderTable.findById(connection, orderId).flatMap {
        case Some(value) => IO.pure(value)
        case None        => IO.raiseError(HttpApiError.BadRequest("未找到订单"))
      }
      _ <- MerchantBusinessService.requireOwnedStore(connection, username, order.merchantId)
      _ <-
        if MerchantBusinessService.canRejectOrder(order.status) then IO.unit
        else IO.raiseError(HttpApiError.BadRequest(s"当前状态不可拒收：${order.status}"))
      account <- CustomerProfileTable.findById(connection, order.customerId).flatMap {
        case Some(value) => IO.pure(value)
        case None        => IO.raiseError(HttpApiError.BadRequest("未找到顾客账号"))
      }
      refundAmount = if order.payableAmount > 0 then order.payableAmount else order.totalAmount
      canceledOrder <- OrderStatusTransitionService.transition(connection, order, OrderStatus.已取消, actorRole = "merchant")
      nextAccount = account.copy(profile = account.profile.copy(walletBalance = OrderCheckoutService.roundMoney(account.profile.walletBalance + refundAmount)))
      _ <- CustomerProfileTable.upsert(connection, nextAccount)
    yield OkResponse(ok = true)
