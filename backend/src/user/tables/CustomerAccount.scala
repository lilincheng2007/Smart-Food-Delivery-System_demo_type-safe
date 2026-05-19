package delivery.user.tables

import delivery.user.objects.CustomerProfile

final case class CustomerAccount(
    role: String,
    username: String,
    password: String,
    profile: CustomerProfile
)
