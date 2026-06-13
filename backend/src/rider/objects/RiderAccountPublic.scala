package delivery.rider.objects

import delivery.domain.UserRole

final case class RiderAccountPublic(role: UserRole, username: String, profile: RiderProfile)
