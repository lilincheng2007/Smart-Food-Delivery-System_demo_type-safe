package delivery.rider.tables

import delivery.rider.objects.RiderProfile

final case class RiderAccount(
    role: String,
    username: String,
    password: String,
    profile: RiderProfile
)
