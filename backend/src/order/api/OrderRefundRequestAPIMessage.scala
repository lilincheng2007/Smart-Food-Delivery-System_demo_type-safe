package delivery.order.api

import cats.effect.IO
import delivery.order.objects.apiTypes.OrderRefundRequestResponse
import delivery.order.tables.order.OrderTable
import delivery.shared.api.{APIWithRoleMessage, HttpApiError}
import delivery.shared.objects.{OrderId, OrderStatus, RefundStatus}
import delivery.user.tables.customerprofile.CustomerProfileTable

import java.sql.Connection

final case class OrderRefundRequestAPIMessage(orderId: OrderId, reason: String, imageUrl: Option[String]) extends APIWithRoleMessage[OrderRefundRequestResponse]:
  override def plan(connection: Connection, username: String): IO[OrderRefundRequestResponse] =
    val trimmedReason = reason.trim
    val trimmedImageUrl = imageUrl.map(_.trim).filter(_.nonEmpty)
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
        else if order.status != OrderStatus.已完成 then IO.raiseError(HttpApiError.BadRequest("仅已完成订单可申请退款"))
        else if trimmedReason.isEmpty then IO.raiseError(HttpApiError.BadRequest("退款理由不能为空"))
        else if RefundWorkflowSupport.isMerchantPending(order.refundStatus) || RefundWorkflowSupport.isAdminPending(order.refundStatus) then IO.raiseError(HttpApiError.BadRequest("退款申请正在审核中"))
        else if order.refundStatus.contains(RefundStatus.商家已驳回) then IO.raiseError(HttpApiError.BadRequest("商家已驳回该申请，可提交管理员仲裁"))
        else if order.refundStatus.contains(RefundStatus.已通过) then IO.raiseError(HttpApiError.BadRequest("订单已退款"))
        else IO.unit
      now <- IO.realTimeInstant.map(_.toString)
      requestedOrder = order.copy(
        refundStatus = Some(RefundStatus.待商家审核),
        refundReason = Some(trimmedReason),
        refundImageUrl = trimmedImageUrl,
        refundRequestedAt = Some(now),
        refundMerchantReason = None,
        refundMerchantReviewedAt = None,
        refundAdminReason = None,
        refundedAt = None
      )
      nextProfile = account.profile.copy(
        historyOrders = requestedOrder :: account.profile.historyOrders.filterNot(_.id == orderId)
      )
      _ <- OrderTable.upsert(connection, requestedOrder)
      _ <- CustomerProfileTable.upsert(connection, account.copy(profile = nextProfile))
    yield OrderRefundRequestResponse(requestedOrder)
