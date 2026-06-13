package delivery.user.objects.apiTypes

import delivery.domain.UserRole

final case class LoginResponse(token: String, username: String, role: UserRole)
