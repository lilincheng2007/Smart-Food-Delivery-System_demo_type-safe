package delivery.shared.json

import delivery.ai.objects.*
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

  private def enumCodec[A](typeName: String, values: Array[A]): Codec[A] =
    Codec.from(
      Decoder.decodeString.emap { raw =>
        values.find(_.toString == raw).toRight(s"$typeName 不合法：$raw")
      },
      Encoder.encodeString.contramap[A](_.toString)
    )

  given Codec[UserRole] = enumCodec("用户角色", UserRole.values)
  given Codec[MerchantCategory] = enumCodec("商户分类", MerchantCategory.values)
  given Codec[RiderStatus] = enumCodec("骑手状态", RiderStatus.values)
  given Codec[ServiceChannel] = enumCodec("服务渠道", ServiceChannel.values)
  given Codec[OrderStatus] = enumCodec("订单状态", OrderStatus.values)
  given Codec[ListingStatus] = enumCodec("上下架状态", ListingStatus.values)
  given Codec[InventoryStatus] = enumCodec("库存状态", InventoryStatus.values)

  given Codec[HealthOk] = deriveCodec
  given Codec[OkResponse] = deriveCodec
  given Codec[ErrorBody] = deriveCodec

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

  private val merchantDecoder0: Decoder[Merchant] = Decoder.instance { c =>
    for
      id <- c.downField("id").as[String]
      storeName <- c.downField("storeName").as[String]
      category <- c.downField("category").as[MerchantCategory]
      address <- c.downField("address").as[String]
      phone <- c.downField("phone").as[String]
      rating <- c.downField("rating").as[Double]
      tags <- c.downField("tags").as[List[String]]
      featuredProductIds <- c.downField("featuredProductIds").as[List[String]]
      imageUrl <- c.downField("imageUrl").as[Option[String]]
      description <- c.downField("description").as[Option[String]]
    yield Merchant(id, storeName, category, address, phone, rating, tags, featuredProductIds, imageUrl, description.getOrElse(""))
  }

  private val merchantEncoder0: Encoder[Merchant] = Encoder.instance { m =>
    Json.obj(
      "id" -> m.id.asJson,
      "storeName" -> m.storeName.asJson,
      "category" -> m.category.asJson,
      "address" -> m.address.asJson,
      "phone" -> m.phone.asJson,
      "rating" -> m.rating.asJson,
      "tags" -> m.tags.asJson,
      "featuredProductIds" -> m.featuredProductIds.asJson,
      "imageUrl" -> m.imageUrl.asJson,
      "description" -> m.description.asJson
    )
  }

  given Decoder[Merchant] = merchantDecoder0
  given Encoder[Merchant] = merchantEncoder0
  given Codec[Merchant] = Codec.from(merchantDecoder0, merchantEncoder0)
  given Codec[MerchantAccountRecord] = deriveCodec
  given Codec[MerchantStoreProfile] = deriveCodec
  given Codec[MerchantProfile] = deriveCodec
  given Codec[MerchantProfileBody] = deriveCodec
  given Codec[CreateProductRequest] = deriveCodec
  given Codec[CreateStoreRequest] = deriveCodec
  given Codec[UpdateStoreImageRequest] = deriveCodec
  given Codec[UpdateProductRequest] = deriveCodec
  given Codec[ProductDescriptionPatch] = deriveCodec
  given Codec[CatalogResponse] = deriveCodec
  given Codec[MerchantAccountPublic] = deriveCodec
  given Codec[MerchantMeResponse] = deriveCodec

  given Codec[Rider] = deriveCodec
  given Codec[RiderAccountRecord] = deriveCodec
  given Codec[RiderProfile] = deriveCodec
  given Codec[RiderAccountPublic] = deriveCodec
  given Codec[RiderAvailableOrdersResponse] = deriveCodec
  given Codec[RiderMeResponse] = deriveCodec

  given Codec[AIRecommendedProduct] = deriveCodec
  given Codec[AIRecommendedMerchant] = deriveCodec
  given Codec[AISearchResponse] = deriveCodec
  given Codec[AISearchRequest] = deriveCodec
  given Codec[AIMerchantStoreDescriptionRequest] = deriveCodec
  given Codec[AIMerchantStoreDescriptionResponse] = deriveCodec
  given Codec[AIMerchantProductDescriptionsRequest] = deriveCodec
  given Codec[AIGeneratedProductDescription] = deriveCodec
  given Codec[AIMerchantProductDescriptionsResponse] = deriveCodec

end ApiJsonCodecs
