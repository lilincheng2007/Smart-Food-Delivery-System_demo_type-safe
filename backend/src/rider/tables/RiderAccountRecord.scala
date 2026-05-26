package delivery.rider.tables

import delivery.rider.objects.RiderProfile

final case class RiderAccountRecord(
    role: String,
    username: String,
    password: String,
    profile: RiderProfile
)
