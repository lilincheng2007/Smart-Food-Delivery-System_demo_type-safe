package delivery.shared.json

import delivery.ai.objects.*
import delivery.ai.objects.apiTypes.*
import delivery.admin.objects.*
import delivery.admin.objects.apiTypes.*
import delivery.merchant.objects.*
import delivery.merchant.objects.apiTypes.*
import delivery.merchant.tables.*
import delivery.order.objects.*
import delivery.order.objects.apiTypes.*
import delivery.order.tables.*
import delivery.review.objects.*
import delivery.review.objects.apiTypes.*
import delivery.rider.objects.*
import delivery.rider.objects.apiTypes.*
import delivery.rider.tables.*
import delivery.shared.objects.*
import delivery.shared.objects.apiTypes.*
import delivery.user.objects.*
import delivery.user.objects.apiTypes.*
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
  given Codec[OrderStatus] = Codec.from(
    Decoder.decodeString.emap(raw => OrderStatus.fromString(raw).toRight(s"订单状态 不合法：$raw")),
    Encoder.encodeString.contramap[OrderStatus](_.toString)
  )
  given Codec[RefundStatus] = enumCodec("退款状态", RefundStatus.values)
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
  given Codec[Promotion] = deriveCodec
  given Codec[ProductBundleOption] = deriveCodec
  given Codec[ProductBundleGroup] = deriveCodec
  given Codec[OrderItem] = deriveCodec

  private val orderDecoder0: Decoder[Order] = Decoder.instance { c =>
    for
      id <- c.downField("id").as[String]
      customerId <- c.downField("customerId").as[String]
      customerName <- c.downField("customerName").as[String]
      customerPhone <- c.downField("customerPhone").as[String]
      merchantId <- c.downField("merchantId").as[String]
      riderId <- c.downField("riderId").as[Option[String]]
      items <- c.downField("items").as[List[OrderItem]]
      totalAmount <- c.downField("totalAmount").as[Double]
      deliveryAddress <- c.downField("deliveryAddress").as[String]
      status <- c.downField("status").as[OrderStatus]
      placedAt <- c.downField("placedAt").as[String]
      originalAmount <- c.downField("originalAmount").as[Option[Double]]
      discountAmount <- c.downField("discountAmount").as[Option[Double]]
      payableAmount <- c.downField("payableAmount").as[Option[Double]]
      usedVoucher <- c.downField("usedVoucher").as[Option[Voucher]]
      merchantDiscountAmount <- c.downField("merchantDiscountAmount").as[Option[Double]]
      platformDiscountAmount <- c.downField("platformDiscountAmount").as[Option[Double]]
      merchantReceivableAmount <- c.downField("merchantReceivableAmount").as[Option[Double]]
      appliedPromotions <- c.downField("appliedPromotions").as[Option[List[Promotion]]]
      pointsAwarded <- c.downField("pointsAwarded").as[Option[Int]]
      refundStatus <- c.downField("refundStatus").as[Option[RefundStatus]]
      refundReason <- c.downField("refundReason").as[Option[String]]
      refundImageUrl <- c.downField("refundImageUrl").as[Option[String]]
      refundRequestedAt <- c.downField("refundRequestedAt").as[Option[String]]
      refundMerchantReason <- c.downField("refundMerchantReason").as[Option[String]]
      refundMerchantReviewedAt <- c.downField("refundMerchantReviewedAt").as[Option[String]]
      refundAdminReason <- c.downField("refundAdminReason").as[Option[String]]
      refundedAt <- c.downField("refundedAt").as[Option[String]]
      customerNoteText <- c.downField("customerNoteText").as[Option[String]]
      customerNoteImageUrl <- c.downField("customerNoteImageUrl").as[Option[String]]
    yield Order(
      id,
      customerId,
      customerName,
      customerPhone,
      merchantId,
      riderId,
      items,
      totalAmount,
      deliveryAddress,
      status,
      placedAt,
      originalAmount.getOrElse(totalAmount),
      discountAmount.getOrElse(0),
      payableAmount.getOrElse(totalAmount),
      usedVoucher,
      merchantDiscountAmount.getOrElse(0),
      platformDiscountAmount.getOrElse(0),
      merchantReceivableAmount.getOrElse(payableAmount.getOrElse(totalAmount)),
      appliedPromotions.getOrElse(Nil),
      pointsAwarded.getOrElse(0),
      refundStatus,
      refundReason,
      refundImageUrl,
      refundRequestedAt,
      refundMerchantReason,
      refundMerchantReviewedAt,
      refundAdminReason,
      refundedAt,
      customerNoteText,
      customerNoteImageUrl
    )
  }

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
    fields += "originalAmount" -> o.originalAmount.asJson
    fields += "discountAmount" -> o.discountAmount.asJson
    fields += "payableAmount" -> o.payableAmount.asJson
    o.usedVoucher.foreach(voucher => fields += "usedVoucher" -> voucher.asJson)
    fields += "merchantDiscountAmount" -> o.merchantDiscountAmount.asJson
    fields += "platformDiscountAmount" -> o.platformDiscountAmount.asJson
    fields += "merchantReceivableAmount" -> o.merchantReceivableAmount.asJson
    fields += "appliedPromotions" -> o.appliedPromotions.asJson
    fields += "pointsAwarded" -> o.pointsAwarded.asJson
    o.refundStatus.foreach(status => fields += "refundStatus" -> status.asJson)
    o.refundReason.foreach(reason => fields += "refundReason" -> reason.asJson)
    o.refundImageUrl.foreach(imageUrl => fields += "refundImageUrl" -> imageUrl.asJson)
    o.refundRequestedAt.foreach(value => fields += "refundRequestedAt" -> value.asJson)
    o.refundMerchantReason.foreach(reason => fields += "refundMerchantReason" -> reason.asJson)
    o.refundMerchantReviewedAt.foreach(value => fields += "refundMerchantReviewedAt" -> value.asJson)
    o.refundAdminReason.foreach(reason => fields += "refundAdminReason" -> reason.asJson)
    o.refundedAt.foreach(value => fields += "refundedAt" -> value.asJson)
    o.customerNoteText.foreach(value => fields += "customerNoteText" -> value.asJson)
    o.customerNoteImageUrl.foreach(value => fields += "customerNoteImageUrl" -> value.asJson)
    Json.obj(fields.result()*)
  }

  given Decoder[Order] = orderDecoder0
  given Encoder[Order] = orderEncoder0
  given Codec[Order] = Codec.from(orderDecoder0, orderEncoder0)
  given Codec[OrderChatMessage] = deriveCodec
  given Codec[OrderChatUnreadCount] = deriveCodec

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

  given Codec[CheckoutBundleSelection] = deriveCodec
  private val checkoutLineDecoder: Decoder[CheckoutLine] = Decoder.instance { c =>
    for
      merchantId <- c.downField("merchantId").as[String]
      productId <- c.downField("productId").as[String]
      quantity <- c.downField("quantity").as[Int]
      bundleSelections <- c.downField("bundleSelections").as[Option[List[CheckoutBundleSelection]]]
    yield CheckoutLine(merchantId, productId, quantity, bundleSelections.getOrElse(Nil))
  }
  private val checkoutLineEncoder: Encoder[CheckoutLine] = Encoder.instance { line =>
    Json
      .obj(
        "merchantId" -> line.merchantId.asJson,
        "productId" -> line.productId.asJson,
        "quantity" -> line.quantity.asJson,
        "bundleSelections" -> line.bundleSelections.asJson
      )
      .dropNullValues
  }
  given Codec[CheckoutLine] = Codec.from(checkoutLineDecoder, checkoutLineEncoder)
  given Codec[OrderMerchantNote] = deriveCodec

  private val checkoutRequestDecoder: Decoder[CheckoutRequest] = Decoder.instance { c =>
    for
      lines <- c.downField("lines").as[List[CheckoutLine]]
      cn <- c.downField("customerName").as[Option[String]]
      cp <- c.downField("customerPhone").as[Option[String]]
      da <- c.downField("deliveryAddress").as[Option[String]]
      voucherId <- c.downField("voucherId").as[Option[String]]
      merchantNotes <- c.downField("merchantNotes").as[Option[List[OrderMerchantNote]]]
    yield CheckoutRequest(lines, cn, cp, da, voucherId, merchantNotes.getOrElse(Nil))
  }

  private val checkoutRequestEncoder: Encoder[CheckoutRequest] = Encoder.instance { r =>
    Json
      .obj(
        "lines" -> r.lines.asJson,
        "customerName" -> r.customerName.asJson,
        "customerPhone" -> r.customerPhone.asJson,
        "deliveryAddress" -> r.deliveryAddress.asJson,
        "voucherId" -> r.voucherId.asJson,
        "merchantNotes" -> r.merchantNotes.asJson
      )
      .dropNullValues
  }

  given Codec[CheckoutRequest] = Codec.from(checkoutRequestDecoder, checkoutRequestEncoder)
  given Codec[CheckoutResponse] = deriveCodec
  given Codec[CustomerOrdersResponse] = deriveCodec
  given Codec[OrderCancelResponse] = deriveCodec
  given Codec[OrderRefundRequestResponse] = deriveCodec
  given Codec[OrderChatMessagesResponse] = deriveCodec
  given Codec[OrderChatUnreadCountsResponse] = deriveCodec

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
      announcement <- c.downField("announcement").as[Option[String]]
      promotions <- c.downField("promotions").as[Option[List[Promotion]]]
    yield Merchant(id, storeName, category, address, phone, rating, tags, featuredProductIds, imageUrl, description.getOrElse(""), announcement.getOrElse(""), promotions.getOrElse(Nil))
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
      "description" -> m.description.asJson,
      "announcement" -> m.announcement.asJson,
      "promotions" -> m.promotions.asJson
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
  given Codec[MerchantRefundRequestsResponse] = deriveCodec

  given Codec[StoreOnboardingRequest] = deriveCodec
  given Codec[StoreOnboardingRequestsResponse] = deriveCodec
  given Codec[AdminRefundRequestsResponse] = deriveCodec
  given Codec[PlatformPromotionsResponse] = deriveCodec

  given Codec[MerchantReview] = deriveCodec
  given Codec[RiderReview] = deriveCodec
  given Codec[ReviewSummary] = deriveCodec
  given Codec[MerchantReviewsResponse] = deriveCodec
  given Codec[RiderReviewsResponse] = deriveCodec

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

  given Codec[AIRecommendedProduct] = deriveCodec
  given Codec[AIRecommendedMerchant] = deriveCodec
  given Codec[AISearchResponse] = deriveCodec
  given Codec[AISearchRequest] = deriveCodec
  given Codec[AIMerchantStoreDescriptionRequest] = deriveCodec
  given Codec[AIMerchantStoreDescriptionResponse] = deriveCodec
  given Codec[AIMerchantBusinessSuggestionsResponse] = deriveCodec
  given Codec[AIMerchantProductDescriptionsRequest] = deriveCodec
  given Codec[AIGeneratedProductDescription] = deriveCodec
  given Codec[AIMerchantProductDescriptionsResponse] = deriveCodec
  given Codec[AIReviewSummaryResponse] = deriveCodec

end ApiJsonCodecs
