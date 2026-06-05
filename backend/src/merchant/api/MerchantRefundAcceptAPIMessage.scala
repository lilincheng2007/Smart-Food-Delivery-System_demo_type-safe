package delivery.merchant.api

import cats.effect.IO
import delivery.order.api.RefundWorkflowSupport
import delivery.order.tables.order.OrderTable
import delivery.shared.api.{APIWithRoleMessage, HttpApiError}
import delivery.shared.objects.OrderId
import delivery.shared.objects.apiTypes.OkResponse

import java.sql.Connection

final case class MerchantRefundAcceptAPIMessage(orderId: OrderId, reason: Option[String]) extends APIWithRoleMessage[OkResponse]:
  override def plan(connection: Connection, username: String): IO[OkResponse] =
    val merchantReason = reason.map(_.trim).filter(_.nonEmpty)
    for
      order <- OrderTable.findById(connection, orderId).flatMap {
        case Some(value) => IO.pure(value)
        case None        => IO.raiseError(HttpApiError.BadRequest("未找到订单"))
      }
      _ <- MerchantAPIMessageSupport.requireOwnedStore(connection, username, order.merchantId)
      _ <-
        if !RefundWorkflowSupport.isMerchantPending(order.refundStatus) then IO.raiseError(HttpApiError.BadRequest("该订单没有待商家处理的退款申请"))
        else IO.unit
      _ <- RefundWorkflowSupport.acceptRefund(connection, order, merchantReason, adminReason = None, markMerchantReviewed = true)
    yield OkResponse(ok = true)
