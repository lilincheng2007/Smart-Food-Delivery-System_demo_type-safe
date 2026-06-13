package delivery.user.objects

import delivery.domain.UserRole

final case class CustomerAccountPublic(role: UserRole, username: String, profile: CustomerProfile)
