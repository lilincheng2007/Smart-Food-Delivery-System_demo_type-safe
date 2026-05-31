package delivery.rider.objects

final case class RiderTimeoutCardRedeemResponse(
    ok: Boolean,
    currentEnergyPoints: Int,
    currentTimeoutCardCount: Int
)
