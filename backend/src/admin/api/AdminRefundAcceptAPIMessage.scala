package delivery.admin.api

import cats.effect.IO
import delivery.order.services.RefundWorkflowService
import delivery.order.tables.order.OrderTable
import delivery.platform.api.{APIWithRoleMessage, HttpApiError}
import delivery.domain.{OrderId, RefundStatus}
import delivery.domain.apiTypes.OkResponse

import java.sql.Connection

final case class AdminRefundAcceptAPIMessage(orderId: OrderId, reason: Option[String]) extends APIWithRoleMessage[OkResponse]:
  override def plan(connection: Connection, username: String): IO[OkResponse] =
    val adminReason = reason.map(_.trim).filter(_.nonEmpty)
    for
      order <- OrderTable.findById(connection, orderId).flatMap {
        case Some(value) => IO.pure(value)
        case None        => IO.raiseError(HttpApiError.BadRequest("未找到订单"))
      }
      _ <-
        if !RefundWorkflowService.isAdminPending(order.refundStatus) then IO.raiseError(HttpApiError.BadRequest("该订单没有待管理员仲裁的退款申请"))
        else IO.unit
      _ <- RefundWorkflowService.acceptRefund(
        connection,
        order,
        merchantReason = None,
        adminReason = adminReason.orElse(Some("管理员仲裁通过退款")),
        markMerchantReviewed = false
      )
    yield OkResponse(ok = true)
