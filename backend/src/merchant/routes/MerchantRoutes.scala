package delivery.merchant.routes

import cats.effect.IO
import delivery.admin.objects.apiTypes.StoreOnboardingRequestsResponse
import delivery.merchant.api.*
import delivery.merchant.objects.*
import delivery.merchant.objects.apiTypes.*
import delivery.platform.api.RegisteredAPIMessage
import delivery.platform.api.RegisteredAPIMessage.{api, apiWithRole}
import delivery.platform.json.ApiJsonCodecs.given
import delivery.domain.apiTypes.OkResponse

object MerchantRoutes:
  val apiMessages: List[RegisteredAPIMessage] =
    val registered = List(
    api[CatalogAPIMessage, CatalogResponse],
    apiWithRole[MerchantMeAPIMessage, MerchantMeResponse]("merchant"),
    apiWithRole[MerchantCreateStoreOnboardingRequestAPIMessage, String]("merchant"),
    apiWithRole[MerchantStoreAPIMessage, String]("merchant"),
    apiWithRole[MerchantStoreOnboardingRequestsAPIMessage, StoreOnboardingRequestsResponse]("merchant"),
    apiWithRole[MerchantStoreDescriptionAPIMessage, OkResponse]("merchant"),
    apiWithRole[MerchantStoreAnnouncementAPIMessage, OkResponse]("merchant"),
    apiWithRole[MerchantBusinessHoursAPIMessage, OkResponse]("merchant"),
    apiWithRole[MerchantStorePromotionsAPIMessage, OkResponse]("merchant"),
    apiWithRole[MerchantStoreImageAPIMessage, OkResponse]("merchant"),
    apiWithRole[MerchantStoreImageFileAPIMessage, String]("merchant"),
    apiWithRole[MerchantCreateProductAPIMessage, Product]("merchant"),
    apiWithRole[MerchantProductAPIMessage, Product]("merchant"),
    apiWithRole[MerchantProductImageFileAPIMessage, Product]("merchant"),
    apiWithRole[MerchantProductDescriptionsAPIMessage, OkResponse]("merchant"),
    apiWithRole[MerchantOrderAcceptAPIMessage, OkResponse]("merchant"),
    apiWithRole[MerchantOrderRejectAPIMessage, OkResponse]("merchant"),
    apiWithRole[MerchantOrderReadyAPIMessage, OkResponse]("merchant"),
    apiWithRole[MerchantOrderPrepDelayAPIMessage, OkResponse]("merchant"),
    apiWithRole[MerchantRefundRequestsAPIMessage, MerchantRefundRequestsResponse]("merchant"),
    apiWithRole[MerchantRefundAcceptAPIMessage, OkResponse]("merchant"),
    apiWithRole[MerchantRefundRejectAPIMessage, OkResponse]("merchant")
  )
    val requiredNames = Set("merchantstoreannouncementapi")
    val registeredNames = registered.map(_.apiName).toSet
    val missing = requiredNames.diff(registeredNames)
    require(missing.isEmpty, s"商家 API 未注册：${missing.toList.sorted.mkString(", ")}")
    registered

end MerchantRoutes
