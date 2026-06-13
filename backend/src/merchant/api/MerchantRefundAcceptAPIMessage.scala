package delivery.merchant.api

import delivery.merchant.services.MerchantBusinessService
import cats.effect.IO
import delivery.order.services.RefundWorkflowService
import delivery.order.tables.order.OrderTable
import delivery.platform.api.{APIWithRoleMessage, HttpApiError}
import delivery.domain.OrderId
import delivery.domain.apiTypes.OkResponse

import java.sql.Connection

final case class MerchantRefundAcceptAPIMessage(orderId: OrderId, reason: Option[String]) extends APIWithRoleMessage[OkResponse]:
  override def plan(connection: Connection, username: String): IO[OkResponse] =
    val merchantReason = reason.map(_.trim).filter(_.nonEmpty)
    for
      order <- OrderTable.findById(connection, orderId).flatMap {
        case Some(value) => IO.pure(value)
        case None        => IO.raiseError(HttpApiError.BadRequest("未找到订单"))
      }
      _ <- MerchantBusinessService.requireOwnedStore(connection, username, order.merchantId)
      _ <-
        if !RefundWorkflowService.isMerchantPending(order.refundStatus) then IO.raiseError(HttpApiError.BadRequest("该订单没有待商家处理的退款申请"))
        else IO.unit
      _ <- RefundWorkflowService.acceptRefund(connection, order, merchantReason, adminReason = None, markMerchantReviewed = true)
    yield OkResponse(ok = true)
