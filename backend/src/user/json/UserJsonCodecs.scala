package delivery.user.json

import delivery.order.json.OrderJsonCodecs.given
import delivery.platform.json.CommonJsonCodecs.given
import delivery.promotion.json.PromotionJsonCodecs.given
import delivery.promotion.objects.*
import delivery.user.api.*
import delivery.user.objects.*
import delivery.user.objects.apiTypes.*
import delivery.user.tables.*
import io.circe.Codec
import io.circe.Decoder
import io.circe.Encoder
import io.circe.Json
import io.circe.generic.semiauto.*
import io.circe.syntax.*

object UserJsonCodecs:

  given Codec[LoginAPIMessage] = deriveCodec
  given Codec[RegisterAPIMessage] = deriveCodec
  given Codec[CustomerMeAPIMessage] = deriveCodec
  given Codec[CustomerProfilePatchAPIMessage] = deriveCodec
  given Codec[CustomerVoucherDiscardAPIMessage] = deriveCodec
  given Codec[CustomerRechargeAPIMessage] = deriveCodec

  given Codec[AuthCredentialRecord] = deriveCodec
  given Codec[Customer] = deriveCodec
  given Codec[CustomerAccountRecord] = deriveCodec
  given Codec[LoginRequest] = deriveCodec
  given Codec[RegisterRequest] = deriveCodec
  given Codec[LoginResponse] = deriveCodec

  private val customerProfilePatchDecoder: Decoder[CustomerProfilePatch] = Decoder.instance { c =>
    for
      defaultAddress <- c.downField("defaultAddress").as[Option[String]]
      name <- c.downField("name").as[Option[String]]
      phone <- c.downField("phone").as[Option[String]]
      deliveryContacts <- c.downField("deliveryContacts").as[Option[List[CustomerDeliveryContact]]]
    yield CustomerProfilePatch(defaultAddress, name, phone, deliveryContacts)
  }

  private val customerProfilePatchEncoder: Encoder[CustomerProfilePatch] = Encoder.instance { p =>
    Json
      .obj(
        "defaultAddress" -> p.defaultAddress.asJson,
        "name" -> p.name.asJson,
        "phone" -> p.phone.asJson,
        "deliveryContacts" -> p.deliveryContacts.asJson
      )
      .dropNullValues
  }

  given Codec[CustomerProfilePatch] = Codec.from(customerProfilePatchDecoder, customerProfilePatchEncoder)
  given Codec[CustomerWalletTopUp] = deriveCodec
  given Codec[CustomerWalletTopUpResponse] = deriveCodec
  given Codec[CustomerDeliveryContact] = deriveCodec

  private val customerProfileDecoder: Decoder[CustomerProfile] = Decoder.instance { c =>
    for
      id <- c.downField("id").as[String]
      name <- c.downField("name").as[String]
      phone <- c.downField("phone").as[String]
      defaultAddress <- c.downField("defaultAddress").as[String]
      vouchers <- c.downField("vouchers").as[List[Voucher]]
      walletBalance <- c.downField("walletBalance").as[Double]
      pendingOrders <- c.downField("pendingOrders").as[List[delivery.order.objects.Order]]
      historyOrders <- c.downField("historyOrders").as[List[delivery.order.objects.Order]]
      contactsOpt <- c.downField("deliveryContacts").as[Option[List[CustomerDeliveryContact]]]
      foodiePoints <- c.downField("foodiePoints").as[Option[Int]]
      foodieLevel <- c.downField("foodieLevel").as[Option[Int]]
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
      CustomerProfile(id, name, phone, defaultAddress, vouchers, walletBalance, pendingOrders, historyOrders, contacts, foodiePoints.getOrElse(0), foodieLevel.getOrElse(1))
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
      "deliveryContacts" -> p.deliveryContacts.asJson,
      "foodiePoints" -> p.foodiePoints.asJson,
      "foodieLevel" -> p.foodieLevel.asJson
    )
  }

  given Codec[CustomerProfile] = Codec.from(customerProfileDecoder, customerProfileEncoder)
  given Codec[CustomerAccountPublic] = deriveCodec
  given Codec[CustomerMeResponse] = deriveCodec

end UserJsonCodecs
