package delivery.admin.api

import cats.effect.IO
import delivery.order.tables.order.OrderTable
import delivery.platform.api.{APIWithRoleMessage, HttpApiError}
import delivery.domain.{OrderId, RefundStatus}
import delivery.domain.apiTypes.OkResponse
import delivery.user.tables.customerprofile.CustomerProfileTable

import java.sql.Connection

final case class AdminRefundRejectAPIMessage(orderId: OrderId, reason: String) extends APIWithRoleMessage[OkResponse]:
  override def plan(connection: Connection, username: String): IO[OkResponse] =
    val trimmedReason = reason.trim
    for
      order <- OrderTable.findById(connection, orderId).flatMap {
        case Some(value) => IO.pure(value)
        case None        => IO.raiseError(HttpApiError.BadRequest("未找到订单"))
      }
      _ <-
        if !order.refundStatus.contains(RefundStatus.待管理员仲裁) then IO.raiseError(HttpApiError.BadRequest("该订单没有待管理员仲裁的退款申请"))
        else if trimmedReason.isEmpty then IO.raiseError(HttpApiError.BadRequest("驳回原因不能为空"))
        else IO.unit
      updatedOrder = order.copy(refundStatus = Some(RefundStatus.已驳回), refundAdminReason = Some(trimmedReason))
      account <- CustomerProfileTable.findById(connection, order.customerId)
      _ <- OrderTable.upsert(connection, updatedOrder)
      _ <- account match
        case Some(value) =>
          CustomerProfileTable.upsert(connection, value.copy(profile = value.profile.copy(
            historyOrders = updatedOrder :: value.profile.historyOrders.filterNot(_.id == order.id)
          )))
        case None => IO.unit
    yield OkResponse(ok = true)
