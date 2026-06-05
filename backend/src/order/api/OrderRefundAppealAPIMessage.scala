package delivery.order.api

import cats.effect.IO
import delivery.order.objects.apiTypes.OrderRefundRequestResponse
import delivery.order.tables.order.OrderTable
import delivery.shared.api.{APIWithRoleMessage, HttpApiError}
import delivery.shared.objects.{OrderId, RefundStatus}
import delivery.user.tables.customerprofile.CustomerProfileTable

import java.sql.Connection

final case class OrderRefundAppealAPIMessage(orderId: OrderId) extends APIWithRoleMessage[OrderRefundRequestResponse]:
  override def plan(connection: Connection, username: String): IO[OrderRefundRequestResponse] =
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
        else if !order.refundStatus.contains(RefundStatus.商家已驳回) then IO.raiseError(HttpApiError.BadRequest("仅商家驳回后的退款申请可提交管理员仲裁"))
        else IO.unit
      appealedOrder = order.copy(
        refundStatus = Some(RefundStatus.待管理员仲裁),
        refundAdminReason = None
      )
      nextProfile = account.profile.copy(
        historyOrders = appealedOrder :: account.profile.historyOrders.filterNot(_.id == orderId)
      )
      _ <- OrderTable.upsert(connection, appealedOrder)
      _ <- CustomerProfileTable.upsert(connection, account.copy(profile = nextProfile))
    yield OrderRefundRequestResponse(appealedOrder)
