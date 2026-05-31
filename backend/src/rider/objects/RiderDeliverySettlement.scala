package delivery.rider.objects

import delivery.shared.objects.OrderId

final case class RiderDeliverySettlement(
    ok: Boolean,
    orderId: OrderId,
    earnedEnergy: Int,
    currentEnergyPoints: Int,
    currentTimeoutCardCount: Int,
    wasTimeout: Boolean,
    timeoutCardUsed: Boolean,
    timeoutExempted: Boolean,
    overtimeSeconds: Int
)
