package delivery.merchant.objects.apiTypes

import delivery.admin.objects.StoreOnboardingRequest
import delivery.merchant.objects.MerchantAccountPublic
import delivery.domain.UserRole

final case class MerchantMeResponse(
    username: String,
    role: UserRole,
    merchantAccount: MerchantAccountPublic,
    onboardingRequests: List[StoreOnboardingRequest] = Nil
)
