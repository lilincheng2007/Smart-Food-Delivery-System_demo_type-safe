package delivery.order.api

import delivery.order.services.OrderCheckoutService
import cats.effect.IO
import delivery.order.objects.Order
import delivery.platform.api.HttpApiError
import delivery.domain.{OrderStatus, RefundStatus}
import delivery.user.tables.customerprofile.CustomerProfileTable

import java.sql.Connection

object RefundWorkflowSupport:

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
        walletBalance = OrderCheckoutService.roundMoney(account.profile.walletBalance + refundAmount),
        foodiePoints = nextPoints,
        foodieLevel = OrderCheckoutService.levelOf(nextPoints),
        historyOrders = refundedOrder :: account.profile.historyOrders.filterNot(_.id == order.id),
        pendingOrders = account.profile.pendingOrders.filterNot(_.id == order.id)
      )
      _ <- CustomerProfileTable.upsert(connection, account.copy(profile = nextProfile))
    yield refundedOrder

end RefundWorkflowSupport
