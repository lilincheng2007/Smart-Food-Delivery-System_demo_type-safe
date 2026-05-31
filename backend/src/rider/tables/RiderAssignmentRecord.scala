package delivery.rider.tables

import delivery.shared.objects.{OrderId, OrderStatus, RiderId}

import java.time.Instant

final case class RiderAssignmentRecord(
    riderId: RiderId,
    orderId: OrderId,
    status: OrderStatus,
    assignedAt: Instant,
    completedAt: Option[Instant],
    deadlineAt: Option[Instant],
    wasTimeout: Boolean,
    timeoutExempted: Boolean,
    timeoutCardUsed: Boolean,
    overtimeSeconds: Int
)
