package delivery.rider.objects

import delivery.domain.OrderId

final case class RiderDeliveryStatus(
    orderId: OrderId,
    assignedAt: String,
    completedAt: Option[String],
    deadlineAt: String,
    wasTimeout: Boolean,
    timeoutExempted: Boolean,
    timeoutCardUsed: Boolean,
    overtimeSeconds: Int,
    canUseTimeoutCard: Boolean
)
