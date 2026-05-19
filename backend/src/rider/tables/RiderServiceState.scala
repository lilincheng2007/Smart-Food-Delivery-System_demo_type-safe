package delivery.rider.tables

import delivery.rider.objects.Rider

final case class RiderServiceState(
    riders: List[Rider],
    riderAccounts: List[RiderAccount]
)
