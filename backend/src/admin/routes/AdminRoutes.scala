package delivery.admin.routes

import delivery.admin.api.*
import delivery.admin.objects.*
import delivery.admin.objects.apiTypes.*
import delivery.platform.api.RegisteredAPIMessage
import delivery.platform.api.RegisteredAPIMessage.apiWithRole
import delivery.platform.json.ApiJsonCodecs.given
import delivery.domain.apiTypes.OkResponse

object AdminRoutes:

  val apiMessages: List[RegisteredAPIMessage] = List(
    apiWithRole[AdminStoreOnboardingRequestsAPIMessage, StoreOnboardingRequestsResponse]("admin"),
    apiWithRole[AdminStoreOnboardingAcceptAPIMessage, OkResponse]("admin"),
    apiWithRole[AdminStoreOnboardingRejectAPIMessage, OkResponse]("admin"),
    apiWithRole[AdminRefundRequestsAPIMessage, AdminRefundRequestsResponse]("admin"),
    apiWithRole[AdminRefundAcceptAPIMessage, OkResponse]("admin"),
    apiWithRole[AdminRefundRejectAPIMessage, OkResponse]("admin"),
    apiWithRole[AdminOrderMonitorAPIMessage, AdminOrderMonitorResponse]("admin"),
    apiWithRole[AdminPlatformPromotionsAPIMessage, PlatformPromotionsResponse]("admin"),
    apiWithRole[AdminPlatformPromotionsUpdateAPIMessage, OkResponse]("admin")
  )

end AdminRoutes
