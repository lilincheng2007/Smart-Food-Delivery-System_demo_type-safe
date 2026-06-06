package delivery.admin.routes

import delivery.admin.api.*
import delivery.admin.objects.*
import delivery.admin.objects.apiTypes.*
import delivery.shared.api.RegisteredAPIMessage
import delivery.shared.api.RegisteredAPIMessage.apiWithRole
import delivery.shared.json.ApiJsonCodecs.given
import delivery.shared.objects.apiTypes.OkResponse
import io.circe.generic.auto.*

object AdminRoutes:

  val apiMessages: List[RegisteredAPIMessage] = List(
    apiWithRole[AdminStoreOnboardingRequestsAPIMessage, StoreOnboardingRequestsResponse]("admin"),
    apiWithRole[AdminStoreOnboardingAcceptAPIMessage, OkResponse]("admin"),
    apiWithRole[AdminStoreOnboardingRejectAPIMessage, OkResponse]("admin"),
    apiWithRole[AdminRefundRequestsAPIMessage, AdminRefundRequestsResponse]("admin"),
    apiWithRole[AdminRefundAcceptAPIMessage, OkResponse]("admin"),
    apiWithRole[AdminRefundRejectAPIMessage, OkResponse]("admin"),
    apiWithRole[AdminPlatformPromotionsAPIMessage, PlatformPromotionsResponse]("admin"),
    apiWithRole[AdminPlatformPromotionsUpdateAPIMessage, OkResponse]("admin")
  )

end AdminRoutes
