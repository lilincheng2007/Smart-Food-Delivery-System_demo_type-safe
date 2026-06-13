package delivery.merchant.api

import delivery.merchant.services.MerchantBusinessService
import cats.effect.IO
import delivery.order.api.RefundWorkflowSupport
import delivery.order.tables.order.OrderTable
import delivery.platform.api.{APIWithRoleMessage, HttpApiError}
import delivery.domain.{OrderId, RefundStatus}
import delivery.domain.apiTypes.OkResponse
import delivery.user.tables.customerprofile.CustomerProfileTable

import java.sql.Connection

final case class MerchantRefundRejectAPIMessage(orderId: OrderId, reason: String) extends APIWithRoleMessage[OkResponse]:
  override def plan(connection: Connection, username: String): IO[OkResponse] =
    val trimmedReason = reason.trim
    for
      order <- OrderTable.findById(connection, orderId).flatMap {
        case Some(value) => IO.pure(value)
        case None        => IO.raiseError(HttpApiError.BadRequest("未找到订单"))
      }
      _ <- MerchantBusinessService.requireOwnedStore(connection, username, order.merchantId)
      _ <-
        if !RefundWorkflowSupport.isMerchantPending(order.refundStatus) then IO.raiseError(HttpApiError.BadRequest("该订单没有待商家处理的退款申请"))
        else if trimmedReason.isEmpty then IO.raiseError(HttpApiError.BadRequest("驳回理由不能为空"))
        else IO.unit
      now <- IO.realTimeInstant.map(_.toString)
      updatedOrder = order.copy(
        refundStatus = Some(RefundStatus.商家已驳回),
        refundMerchantReason = Some(trimmedReason),
        refundMerchantReviewedAt = Some(now),
        refundAdminReason = None
      )
      customerAccount <- CustomerProfileTable.findById(connection, order.customerId)
      _ <- OrderTable.upsert(connection, updatedOrder)
      _ <- customerAccount match
        case Some(value) =>
          CustomerProfileTable.upsert(connection, value.copy(profile = value.profile.copy(
            historyOrders = updatedOrder :: value.profile.historyOrders.filterNot(_.id == order.id)
          )))
        case None => IO.unit
    yield OkResponse(ok = true)
