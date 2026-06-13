package delivery.user.objects.apiTypes

import delivery.domain.UserRole
import delivery.user.objects.CustomerAccountPublic

final case class CustomerMeResponse(
    username: String,
    role: UserRole,
    customerAccount: CustomerAccountPublic
)
