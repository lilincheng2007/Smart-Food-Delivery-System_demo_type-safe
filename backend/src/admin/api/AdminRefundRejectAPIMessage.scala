package delivery.admin.api

import cats.effect.IO
import delivery.order.services.RefundWorkflowService
import delivery.order.tables.order.OrderTable
import delivery.platform.api.{APIWithRoleMessage, HttpApiError}
import delivery.domain.OrderId
import delivery.domain.apiTypes.OkResponse

import java.sql.Connection

final case class AdminRefundRejectAPIMessage(orderId: OrderId, reason: String) extends APIWithRoleMessage[OkResponse]:
  override def plan(connection: Connection, username: String): IO[OkResponse] =
    for
      order <- OrderTable.findById(connection, orderId).flatMap {
        case Some(value) => IO.pure(value)
        case None        => IO.raiseError(HttpApiError.BadRequest("未找到订单"))
      }
      _ <- RefundWorkflowService.rejectByAdmin(connection, order, reason)
    yield OkResponse(ok = true)
