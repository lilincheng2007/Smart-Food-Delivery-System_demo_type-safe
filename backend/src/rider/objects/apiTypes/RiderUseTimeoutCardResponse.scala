package delivery.rider.objects.apiTypes

import delivery.domain.OrderId

final case class RiderUseTimeoutCardResponse(
    ok: Boolean,
    orderId: OrderId,
    currentTimeoutCardCount: Int,
    timeoutExempted: Boolean
)
