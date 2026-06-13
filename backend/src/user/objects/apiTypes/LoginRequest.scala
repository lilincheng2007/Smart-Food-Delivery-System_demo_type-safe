package delivery.user.objects.apiTypes

import delivery.domain.UserRole

final case class LoginRequest(role: UserRole, username: String, password: String)
