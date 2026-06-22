package delivery.admin.json

import delivery.admin.api.*
import delivery.admin.objects.*
import delivery.admin.objects.apiTypes.*
import delivery.order.json.OrderJsonCodecs.given
import delivery.platform.json.CommonJsonCodecs.given
import delivery.promotion.json.PromotionJsonCodecs.given
import io.circe.Codec
import io.circe.generic.semiauto.*

object AdminJsonCodecs:

  given Codec[AdminStoreOnboardingRequestsAPIMessage] = deriveCodec
  given Codec[AdminStoreOnboardingAcceptAPIMessage] = deriveCodec
  given Codec[AdminStoreOnboardingRejectAPIMessage] = deriveCodec
  given Codec[AdminRefundRequestsAPIMessage] = deriveCodec
  given Codec[AdminRefundAcceptAPIMessage] = deriveCodec
  given Codec[AdminRefundRejectAPIMessage] = deriveCodec
  given Codec[AdminOrderMonitorAPIMessage] = deriveCodec
  given Codec[AdminPlatformPromotionsAPIMessage] = deriveCodec
  given Codec[AdminPlatformPromotionsUpdateAPIMessage] = deriveCodec

  given Codec[StoreOnboardingRequest] = deriveCodec
  given Codec[StoreOnboardingRequestsResponse] = deriveCodec
  given Codec[AdminRefundRequestsResponse] = deriveCodec
  given Codec[AdminOrderMonitorItem] = deriveCodec
  given Codec[AdminOrderMonitorResponse] = deriveCodec
  given Codec[PlatformPromotionsResponse] = deriveCodec

end AdminJsonCodecs
