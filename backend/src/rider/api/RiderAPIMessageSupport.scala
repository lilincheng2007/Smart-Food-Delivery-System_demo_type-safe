package delivery.rider.api

import delivery.rider.objects.RiderDeliveryStatus
import delivery.rider.tables.RiderAssignmentRecord
import delivery.rider.utils.RiderTimeoutPolicy
import delivery.shared.objects.OrderStatus

object RiderAPIMessageSupport:

  def isHistoryOrderStatus(status: OrderStatus): Boolean =
    OrderStatus.history.contains(status)

  def isAvailableOrder(orderStatus: OrderStatus): Boolean =
    orderStatus == OrderStatus.待骑手接单

  def statusView(record: RiderAssignmentRecord, canUseTimeoutCard: Boolean): RiderDeliveryStatus =
    RiderDeliveryStatus(
      orderId = record.orderId,
      assignedAt = record.assignedAt.toString,
      completedAt = record.completedAt.map(_.toString),
      deadlineAt = record.deadlineAt.getOrElse(RiderTimeoutPolicy.deadlineAt(record.assignedAt)).toString,
      wasTimeout = record.wasTimeout,
      timeoutExempted = record.timeoutExempted,
      timeoutCardUsed = record.timeoutCardUsed,
      overtimeSeconds = record.overtimeSeconds,
      canUseTimeoutCard = canUseTimeoutCard && record.wasTimeout && !record.timeoutExempted
    )

end RiderAPIMessageSupport
