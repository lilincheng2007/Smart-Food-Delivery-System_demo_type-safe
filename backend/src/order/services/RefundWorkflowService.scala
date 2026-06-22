package delivery.order.services

import cats.effect.IO
import delivery.user.services.CustomerLoyaltyService
import delivery.order.objects.Order
import delivery.order.tables.order.OrderTable
import delivery.platform.api.HttpApiError
import delivery.domain.{OrderStatus, RefundStatus}
import delivery.user.tables.customerprofile.CustomerProfileTable

import java.sql.Connection

object RefundWorkflowService:

  def isMerchantPending(status: Option[RefundStatus]): Boolean =
    status.exists(value => value == RefundStatus.待商家审核 || value == RefundStatus.待审核)

  def isAdminPending(status: Option[RefundStatus]): Boolean =
    status.contains(RefundStatus.待管理员仲裁)

  def acceptRefund(
      connection: Connection,
      order: Order,
      merchantReason: Option[String],
      adminReason: Option[String],
      markMerchantReviewed: Boolean
  ): IO[Order] =
    for
      account <- CustomerProfileTable.findById(connection, order.customerId).flatMap {
        case Some(value) => IO.pure(value)
        case None        => IO.raiseError(HttpApiError.BadRequest("未找到顾客账号"))
      }
      now <- IO.realTimeInstant.map(_.toString)
      refundAmount = if order.payableAmount > 0 then order.payableAmount else order.totalAmount
      refundedOrder <- OrderStatusTransitionService.transition(
        connection,
        order,
        OrderStatus.已退款,
        actorRole = if markMerchantReviewed then "merchant" else "admin",
        patch = _.copy(
          refundStatus = Some(RefundStatus.已通过),
          refundMerchantReason = merchantReason.orElse(order.refundMerchantReason),
          refundMerchantReviewedAt = if markMerchantReviewed then Some(now) else order.refundMerchantReviewedAt,
          refundAdminReason = adminReason.orElse(order.refundAdminReason),
          refundedAt = Some(now)
        )
      )
      nextPoints = math.max(0, account.profile.foodiePoints - order.pointsAwarded)
      nextProfile = account.profile.copy(
        walletBalance = CheckoutPricingService.roundMoney(account.profile.walletBalance + refundAmount),
        foodiePoints = nextPoints,
        foodieLevel = CustomerLoyaltyService.levelOf(nextPoints),
        historyOrders = refundedOrder :: account.profile.historyOrders.filterNot(_.id == order.id),
        pendingOrders = account.profile.pendingOrders.filterNot(_.id == order.id)
      )
      _ <- CustomerProfileTable.upsert(connection, account.copy(profile = nextProfile))
    yield refundedOrder

  def rejectByAdmin(connection: Connection, order: Order, reason: String): IO[Order] =
    val trimmedReason = reason.trim
    for
      _ <-
        if !isAdminPending(order.refundStatus) then IO.raiseError(HttpApiError.BadRequest("该订单没有待管理员仲裁的退款申请"))
        else if trimmedReason.isEmpty then IO.raiseError(HttpApiError.BadRequest("驳回原因不能为空"))
        else IO.unit
      updatedOrder = order.copy(
        refundStatus = Some(RefundStatus.已驳回),
        refundAdminReason = Some(trimmedReason)
      )
      _ <- OrderTable.upsert(connection, updatedOrder)
      _ <- syncCustomerHistoryOrder(connection, updatedOrder)
    yield updatedOrder

  def rejectByMerchant(connection: Connection, order: Order, reason: String): IO[Order] =
    val trimmedReason = reason.trim
    for
      _ <-
        if !isMerchantPending(order.refundStatus) then IO.raiseError(HttpApiError.BadRequest("该订单没有待商家处理的退款申请"))
        else if trimmedReason.isEmpty then IO.raiseError(HttpApiError.BadRequest("驳回理由不能为空"))
        else IO.unit
      now <- IO.realTimeInstant.map(_.toString)
      updatedOrder = order.copy(
        refundStatus = Some(RefundStatus.商家已驳回),
        refundMerchantReason = Some(trimmedReason),
        refundMerchantReviewedAt = Some(now),
        refundAdminReason = None
      )
      _ <- OrderTable.upsert(connection, updatedOrder)
      _ <- syncCustomerHistoryOrder(connection, updatedOrder)
    yield updatedOrder

  private def syncCustomerHistoryOrder(connection: Connection, order: Order): IO[Unit] =
    CustomerProfileTable.findById(connection, order.customerId).flatMap {
      case Some(value) =>
        CustomerProfileTable.upsert(connection, value.copy(profile = value.profile.copy(
          historyOrders = order :: value.profile.historyOrders.filterNot(_.id == order.id)
        ))).void
      case None => IO.unit
    }

end RefundWorkflowService
