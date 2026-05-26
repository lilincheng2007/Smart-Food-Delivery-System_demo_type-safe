package delivery.shared.json

import delivery.merchant.objects.*
import delivery.merchant.tables.*
import delivery.order.objects.*
import delivery.order.tables.*
import delivery.rider.objects.*
import delivery.rider.tables.*
import delivery.shared.objects.*
import delivery.user.objects.*
import delivery.user.tables.*
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
  /** 显式编解码：避免 deriveCodec 在 Scala 3 下对部分 PATCH 字段（如 deliveryContacts）解码不完整 */
  private val customerProfilePatchDecoder: Decoder[CustomerProfilePatch] = Decoder.instance { c =>
    for
      walletBalance <- c.downField("walletBalance").as[Option[Double]]
      defaultAddress <- c.downField("defaultAddress").as[Option[String]]
      name <- c.downField("name").as[Option[String]]
      phone <- c.downField("phone").as[Option[String]]
      deliveryContacts <- c.downField("deliveryContacts").as[Option[List[CustomerDeliveryContact]]]
    yield CustomerProfilePatch(walletBalance, defaultAddress, name, phone, deliveryContacts)
  }

  private val customerProfilePatchEncoder: Encoder[CustomerProfilePatch] = Encoder.instance { p =>
    Json
      .obj(
        "walletBalance" -> p.walletBalance.asJson,
        "defaultAddress" -> p.defaultAddress.asJson,
        "name" -> p.name.asJson,
        "phone" -> p.phone.asJson,
        "deliveryContacts" -> p.deliveryContacts.asJson
      )
      .dropNullValues
  }

  given Codec[CustomerProfilePatch] = Codec.from(customerProfilePatchDecoder, customerProfilePatchEncoder)
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

  given Codec[CustomerDeliveryContact] = deriveCodec

  private val customerProfileDecoder: Decoder[CustomerProfile] = Decoder.instance { c =>
    for
      id <- c.downField("id").as[String]
      name <- c.downField("name").as[String]
      phone <- c.downField("phone").as[String]
      defaultAddress <- c.downField("defaultAddress").as[String]
      vouchers <- c.downField("vouchers").as[List[Voucher]]
      walletBalance <- c.downField("walletBalance").as[Double]
      pendingOrders <- c.downField("pendingOrders").as[List[Order]]
      historyOrders <- c.downField("historyOrders").as[List[Order]]
      contactsOpt <- c.downField("deliveryContacts").as[Option[List[CustomerDeliveryContact]]]
    yield
      val rawList = contactsOpt.getOrElse(Nil)
      val contacts =
        if rawList.nonEmpty then rawList
        else
          List(
            CustomerDeliveryContact(
              id = s"$id-dc-legacy",
              name = name,
              phone = phone,
              address = defaultAddress,
              isDefault = true
            )
          )
      CustomerProfile(id, name, phone, defaultAddress, vouchers, walletBalance, pendingOrders, historyOrders, contacts)
  }

  private val customerProfileEncoder: Encoder[CustomerProfile] = Encoder.instance { p =>
    Json.obj(
      "id" -> p.id.asJson,
      "name" -> p.name.asJson,
      "phone" -> p.phone.asJson,
      "defaultAddress" -> p.defaultAddress.asJson,
      "vouchers" -> p.vouchers.asJson,
      "walletBalance" -> p.walletBalance.asJson,
      "pendingOrders" -> p.pendingOrders.asJson,
      "historyOrders" -> p.historyOrders.asJson,
      "deliveryContacts" -> p.deliveryContacts.asJson
    )
  }

  given Codec[CustomerProfile] = Codec.from(customerProfileDecoder, customerProfileEncoder)
  given Codec[CustomerAccountPublic] = deriveCodec
  given Codec[CustomerMeResponse] = deriveCodec

  given Codec[CheckoutLine] = deriveCodec

  private val checkoutRequestDecoder: Decoder[CheckoutRequest] = Decoder.instance { c =>
    for
      lines <- c.downField("lines").as[List[CheckoutLine]]
      cn <- c.downField("customerName").as[Option[String]]
      cp <- c.downField("customerPhone").as[Option[String]]
      da <- c.downField("deliveryAddress").as[Option[String]]
    yield CheckoutRequest(lines, cn, cp, da)
  }

  private val checkoutRequestEncoder: Encoder[CheckoutRequest] = Encoder.instance { r =>
    Json
      .obj(
        "lines" -> r.lines.asJson,
        "customerName" -> r.customerName.asJson,
        "customerPhone" -> r.customerPhone.asJson,
        "deliveryAddress" -> r.deliveryAddress.asJson
      )
      .dropNullValues
  }

  given Codec[CheckoutRequest] = Codec.from(checkoutRequestDecoder, checkoutRequestEncoder)
  given Codec[CheckoutResponse] = deriveCodec
  given Codec[CustomerOrdersResponse] = deriveCodec
  given Codec[OrderCancelResponse] = deriveCodec

  given Codec[Product] = deriveCodec
  given Codec[Merchant] = deriveCodec
  given Codec[MerchantAccount] = deriveCodec
  given Codec[MerchantServiceState] = deriveCodec
  given Codec[MerchantStoreProfile] = deriveCodec
  given Codec[MerchantProfile] = deriveCodec
  given Codec[MerchantProfileBody] = deriveCodec
  given Codec[CreateProductRequest] = deriveCodec
  given Codec[CreateStoreRequest] = deriveCodec
  given Codec[UpdateStoreImageRequest] = deriveCodec
  given Codec[UpdateProductRequest] = deriveCodec
  given Codec[CatalogResponse] = deriveCodec
  given Codec[MerchantAccountPublic] = deriveCodec
  given Codec[MerchantMeResponse] = deriveCodec

  given Codec[Rider] = deriveCodec
  given Codec[RiderAccount] = deriveCodec
  given Codec[RiderServiceState] = deriveCodec
  given Codec[RiderProfile] = deriveCodec
  given Codec[RiderAccountPublic] = deriveCodec
  given Codec[RiderMeResponse] = deriveCodec

end ApiJsonCodecs
