package delivery.merchant.objects

import delivery.domain.UserRole

final case class MerchantAccountPublic(role: UserRole, username: String, profile: MerchantProfile)
