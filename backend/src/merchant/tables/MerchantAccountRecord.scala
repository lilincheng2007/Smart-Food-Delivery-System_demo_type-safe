package delivery.merchant.tables

import delivery.merchant.objects.MerchantProfile

final case class MerchantAccountRecord(
    role: String,
    username: String,
    password: String,
    profile: MerchantProfile
)
