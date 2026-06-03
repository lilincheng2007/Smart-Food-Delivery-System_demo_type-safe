package delivery.merchant.api

import cats.effect.IO
import delivery.order.tables.order.OrderTable
import delivery.shared.api.{APIWithRoleMessage, HttpApiError}
import delivery.shared.objects.{OrderId, OrderStatus}
import delivery.shared.objects.apiTypes.OkResponse

import java.sql.Connection

final case class MerchantOrderAcceptAPIMessage(orderId: OrderId) extends APIWithRoleMessage[OkResponse]:
  override def plan(connection: Connection, username: String): IO[OkResponse] =
    for
      order <- OrderTable.findById(connection, orderId).flatMap {
        case Some(value) => IO.pure(value)
        case None        => IO.raiseError(HttpApiError.BadRequest("未找到订单"))
      }
      _ <- MerchantAPIMessageSupport.requireOwnedStore(connection, username, order.merchantId)
      _ <-
        if MerchantAPIMessageSupport.canAcceptOrder(order.status) then IO.unit
        else IO.raiseError(HttpApiError.BadRequest(s"当前状态不可接单：${order.status}"))
      _ <- OrderTable.upsert(connection, order.copy(status = OrderStatus.制作中))
    yield OkResponse(ok = true)
