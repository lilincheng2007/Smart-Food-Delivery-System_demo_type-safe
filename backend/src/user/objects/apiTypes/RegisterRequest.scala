package delivery.user.objects.apiTypes

import delivery.domain.UserRole

final case class RegisterRequest(role: UserRole, username: String, password: String)
