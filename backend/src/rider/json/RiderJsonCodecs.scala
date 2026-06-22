package delivery.rider.json

import delivery.order.json.OrderJsonCodecs.given
import delivery.platform.json.CommonJsonCodecs.given
import delivery.review.json.ReviewJsonCodecs.given
import delivery.rider.api.*
import delivery.rider.objects.*
import delivery.rider.objects.apiTypes.*
import delivery.rider.tables.*
import io.circe.Codec
import io.circe.generic.semiauto.*

object RiderJsonCodecs:

  given Codec[RiderMeAPIMessage] = deriveCodec
  given Codec[RiderAvailableOrdersAPIMessage] = deriveCodec
  given Codec[RiderGrabOrderAPIMessage] = deriveCodec
  given Codec[RiderUpdateOrderStatusAPIMessage] = deriveCodec
  given Codec[RiderRedeemTimeoutCardAPIMessage] = deriveCodec
  given Codec[RiderUseTimeoutCardAPIMessage] = deriveCodec

  given Codec[Rider] = deriveCodec
  given Codec[RiderAccountRecord] = deriveCodec
  given Codec[RiderProfile] = deriveCodec
  given Codec[RiderAccountPublic] = deriveCodec
  given Codec[RiderAvailableOrdersResponse] = deriveCodec
  given Codec[RiderDeliveryStatus] = deriveCodec
  given Codec[RiderDeliverySettlement] = deriveCodec
  given Codec[RiderTimeoutCardRedeemResponse] = deriveCodec
  given Codec[RiderUseTimeoutCardResponse] = deriveCodec
  given Codec[RiderMeResponse] = deriveCodec

end RiderJsonCodecs
