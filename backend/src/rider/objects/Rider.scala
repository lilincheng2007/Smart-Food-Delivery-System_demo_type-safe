package delivery.rider.objects

import delivery.domain.{RiderId, RiderStatus}

final case class Rider(
    id: RiderId,
    name: String,
    phone: String,
    realtimeLocation: String,
    status: RiderStatus,
    totalOrders: Int,
    rating: Double,
    station: String,
    salary: Double,
    energyPoints: Int = 0,
    timeoutCardCount: Int = 0,
    timeoutCount: Int = 0,
    timeoutExemptedCount: Int = 0
)
