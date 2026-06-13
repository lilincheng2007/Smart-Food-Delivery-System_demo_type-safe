package delivery.merchant.api

import delivery.merchant.services.MerchantBusinessService
import cats.effect.IO
import delivery.order.api.OrderStatusTransitionService
import delivery.order.tables.order.OrderTable
import delivery.platform.api.{APIWithRoleMessage, HttpApiError}
import delivery.domain.{OrderId, OrderStatus}
import delivery.domain.apiTypes.{OkResponse}

import java.sql.Connection

final case class MerchantOrderReadyAPIMessage(orderId: OrderId) extends APIWithRoleMessage[OkResponse]:
  override def plan(connection: Connection, username: String): IO[OkResponse] =
    for
      order <- OrderTable.findById(connection, orderId).flatMap {
        case Some(value) => IO.pure(value)
        case None        => IO.raiseError(HttpApiError.BadRequest("未找到订单"))
      }
      _ <- MerchantBusinessService.requireOwnedStore(connection, username, order.merchantId)
      _ <- OrderStatusTransitionService.transition(connection, order, OrderStatus.待骑手接单, actorRole = "merchant")
    yield OkResponse(ok = true)
