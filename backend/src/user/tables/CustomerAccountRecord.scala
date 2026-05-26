package delivery.user.tables

import delivery.user.objects.CustomerProfile

final case class CustomerAccountRecord(
    role: String,
    username: String,
    password: String,
    profile: CustomerProfile
)
