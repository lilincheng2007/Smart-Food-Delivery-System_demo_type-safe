package delivery.shared.json

import delivery.admin.objects.*
import delivery.admin.state.*
import delivery.merchant.objects.*
import delivery.merchant.state.*
import delivery.order.objects.*
import delivery.order.state.*
import delivery.rider.objects.*
import delivery.rider.state.*
import delivery.shared.objects.*
import delivery.user.objects.*
import delivery.user.state.*
import io.circe.Codec
import io.circe.Decoder
import io.circe.Encoder
import io.circe.Json
import io.circe.generic.semiauto.*
import io.circe.syntax.*

object ApiJsonCodecs:

  given Codec[DeliveryState] = deriveCodec
  given Codec[HealthOk] = deriveCodec
  given Codec[OkResponse] = deriveCodec
  given Codec[ErrorBody] = deriveCodec

  given Codec[AuthCredential] = deriveCodec
  given Codec[Customer] = deriveCodec
  given Codec[CustomerAccount] = deriveCodec
  given Codec[UserServiceState] = deriveCodec
  given Codec[LoginRequest] = deriveCodec
  given Codec[RegisterRequest] = deriveCodec
  given Codec[LoginResponse] = deriveCodec
  given Codec[CustomerProfilePatch] = deriveCodec
  given Codec[CheckoutCompleteRequest] = deriveCodec
  given Codec[Voucher] = deriveCodec
  given Codec[OrderItem] = deriveCodec

  private val orderDecoder0: Decoder[Order] = deriveDecoder
  private val orderEncoder0: Encoder[Order] = Encoder.instance { o =>
    val fields = List.newBuilder[(String, Json)]
    fields += "id" -> o.id.asJson
    fields += "customerId" -> o.customerId.asJson
    fields += "customerName" -> o.customerName.asJson
    fields += "customerPhone" -> o.customerPhone.asJson
    fields += "merchantId" -> o.merchantId.asJson
    o.riderId.foreach(r => fields += "riderId" -> r.asJson)
    fields += "items" -> o.items.asJson
    fields += "totalAmount" -> o.totalAmount.asJson
    fields += "deliveryAddress" -> o.deliveryAddress.asJson
    fields += "status" -> o.status.asJson
    fields += "placedAt" -> o.placedAt.asJson
    Json.obj(fields.result()*)
  }

  given Decoder[Order] = orderDecoder0
  given Encoder[Order] = orderEncoder0
  given Codec[Order] = Codec.from(orderDecoder0, orderEncoder0)
  given Codec[OrderServiceState] = deriveCodec
  given Codec[CustomerProfile] = deriveCodec
  given Codec[CustomerAccountPublic] = deriveCodec
  given Codec[CustomerMeResponse] = deriveCodec

  given Codec[CheckoutLine] = deriveCodec
  given Codec[CheckoutRequest] = deriveCodec
  given Codec[CheckoutResponse] = deriveCodec

  given Codec[Product] = deriveCodec
  given Codec[Merchant] = deriveCodec
  given Codec[MerchantAccount] = deriveCodec
  given Codec[MerchantServiceState] = deriveCodec
  given Codec[MerchantStoreProfile] = deriveCodec
  given Codec[MerchantProfile] = deriveCodec
  given Codec[MerchantProfileBody] = deriveCodec
  given Codec[CreateProductRequest] = deriveCodec
  given Codec[CreateProductResponse] = deriveCodec
  given Codec[CreateStoreRequest] = deriveCodec
  given Codec[CreateStoreResponse] = deriveCodec
  given Codec[UpdateStoreImageRequest] = deriveCodec
  given Codec[StoreImageUploadResponse] = deriveCodec
  given Codec[UpdateProductRequest] = deriveCodec
  given Codec[UpdateProductResponse] = deriveCodec
  given Codec[CatalogResponse] = deriveCodec
  given Codec[MerchantAccountPublic] = deriveCodec
  given Codec[MerchantMeResponse] = deriveCodec

  given Codec[Rider] = deriveCodec
  given Codec[RiderAccount] = deriveCodec
  given Codec[RiderServiceState] = deriveCodec
  given Codec[RiderProfile] = deriveCodec
  given Codec[RiderAccountPublic] = deriveCodec
  given Codec[RiderMeResponse] = deriveCodec
  given Codec[RiderUpdateOrderStatusResponse] = deriveCodec

  given Codec[AdminAccount] = deriveCodec
  given Codec[AdminServiceState] = deriveCodec
  given Codec[ComplaintTicket] = deriveCodec
  given Codec[CustomerServiceAgent] = deriveCodec
  given Codec[MerchantApplication] = deriveCodec
  given Codec[OperationsManager] = deriveCodec
  given Codec[PromotionCampaign] = deriveCodec
  given Codec[AdminAccountPublic] = deriveCodec
  given Codec[AdminMeResponse] = deriveCodec
  given Codec[RootInfoModule] = deriveCodec
  given Codec[RootInfoResponse] = deriveCodec
  given Codec[OverviewResponse] = deriveCodec
  given Codec[OrdersPanelResponse] = deriveCodec
  given Codec[PlatformMetaResponse] = deriveCodec

end ApiJsonCodecs
