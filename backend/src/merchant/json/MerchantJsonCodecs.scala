package delivery.merchant.json

import delivery.admin.json.AdminJsonCodecs.given
import delivery.domain.*
import delivery.merchant.api.*
import delivery.merchant.objects.*
import delivery.merchant.objects.apiTypes.*
import delivery.merchant.tables.*
import delivery.order.json.OrderJsonCodecs.given
import delivery.platform.json.CommonJsonCodecs.given
import delivery.platform.json.CommonJsonCodecs.enumCodec
import delivery.promotion.json.PromotionJsonCodecs.given
import delivery.promotion.objects.*
import io.circe.Codec
import io.circe.Decoder
import io.circe.Encoder
import io.circe.Json
import io.circe.generic.semiauto.*
import io.circe.syntax.*

object MerchantJsonCodecs:

  given Codec[CatalogAPIMessage] = deriveCodec
  given Codec[MerchantMeAPIMessage] = deriveCodec
  given Codec[MerchantCreateStoreOnboardingRequestAPIMessage] = deriveCodec
  given Codec[MerchantStoreAPIMessage] = deriveCodec
  given Codec[MerchantStoreOnboardingRequestsAPIMessage] = deriveCodec
  given Codec[MerchantStoreDescriptionAPIMessage] = deriveCodec
  given Codec[MerchantStoreAnnouncementAPIMessage] = deriveCodec
  given Codec[MerchantBusinessHoursAPIMessage] = deriveCodec
  given Codec[MerchantStorePromotionsAPIMessage] = deriveCodec
  given Codec[MerchantStoreImageAPIMessage] = deriveCodec
  given Codec[MerchantStoreImageFileAPIMessage] = deriveCodec
  given Codec[MerchantCreateProductAPIMessage] = deriveCodec
  given Codec[MerchantProductAPIMessage] = deriveCodec
  given Codec[MerchantProductImageFileAPIMessage] = deriveCodec
  given Codec[MerchantProductDescriptionsAPIMessage] = deriveCodec
  given Codec[MerchantOrderAcceptAPIMessage] = deriveCodec
  given Codec[MerchantOrderRejectAPIMessage] = deriveCodec
  given Codec[MerchantOrderReadyAPIMessage] = deriveCodec
  given Codec[MerchantOrderPrepDelayAPIMessage] = deriveCodec
  given Codec[MerchantRefundRequestsAPIMessage] = deriveCodec
  given Codec[MerchantRefundAcceptAPIMessage] = deriveCodec
  given Codec[MerchantRefundRejectAPIMessage] = deriveCodec

  given Codec[MerchantBusinessStatus] = enumCodec("商家营业状态", MerchantBusinessStatus.values)
  given Codec[ProductInventoryMode] = enumCodec("商品库存模式", ProductInventoryMode.values)
  given Codec[ProductBundleSelectionType] = enumCodec("套餐选择类型", ProductBundleSelectionType.values)

  private val productBundleOptionDecoder: Decoder[ProductBundleOption] = Decoder.instance { c =>
    for
      productId <- c.downField("productId").as[ProductId]
      recommended <- c.downField("recommended").as[Option[Boolean]]
      extraPrice <- c.downField("extraPrice").as[Option[Double]]
      customExtraPrice <- c.downField("customExtraPrice").as[Option[Boolean]]
    yield ProductBundleOption(productId, recommended.getOrElse(false), math.max(0.0, extraPrice.getOrElse(0.0)), customExtraPrice.getOrElse(false))
  }

  private val productBundleOptionEncoder: Encoder[ProductBundleOption] = Encoder.instance { option =>
    Json.obj(
      "productId" -> option.productId.asJson,
      "recommended" -> option.recommended.asJson,
      "extraPrice" -> option.extraPrice.asJson,
      "customExtraPrice" -> option.customExtraPrice.asJson
    )
  }

  given Codec[ProductBundleOption] = Codec.from(productBundleOptionDecoder, productBundleOptionEncoder)

  private val productBundleGroupDecoder: Decoder[ProductBundleGroup] = Decoder.instance { c =>
    for
      id <- c.downField("id").as[String]
      name <- c.downField("name").as[String]
      quantity <- c.downField("quantity").as[Int]
      selectionType <- c.downField("selectionType").as[Option[ProductBundleSelectionType]]
      includedPrice <- c.downField("includedPrice").as[Option[Double]]
      options <- c.downField("options").as[Option[List[ProductBundleOption]]]
    yield ProductBundleGroup(id, name, quantity, selectionType.getOrElse(ProductBundleSelectionType.repeatable), math.max(0.0, includedPrice.getOrElse(0.0)), options.getOrElse(Nil))
  }

  private val productBundleGroupEncoder: Encoder[ProductBundleGroup] = Encoder.instance { group =>
    Json.obj(
      "id" -> group.id.asJson,
      "name" -> group.name.asJson,
      "quantity" -> group.quantity.asJson,
      "selectionType" -> group.selectionType.asJson,
      "includedPrice" -> group.includedPrice.asJson,
      "options" -> group.options.asJson
    )
  }

  given Codec[ProductBundleGroup] = Codec.from(productBundleGroupDecoder, productBundleGroupEncoder)
  given Codec[Product] = deriveCodec
  given Codec[MerchantWeeklyBusinessHour] = deriveCodec
  given Codec[MerchantHolidayBusinessHour] = deriveCodec

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
      businessStatus <- c.downField("businessStatus").as[Option[MerchantBusinessStatus]]
      weeklyBusinessHours <- c.downField("weeklyBusinessHours").as[Option[List[MerchantWeeklyBusinessHour]]]
      holidayBusinessHours <- c.downField("holidayBusinessHours").as[Option[List[MerchantHolidayBusinessHour]]]
    yield Merchant(id, storeName, category, address, phone, rating, tags, featuredProductIds, imageUrl, description.getOrElse(""), announcement.getOrElse(""), promotions.getOrElse(Nil), businessStatus.getOrElse(MerchantBusinessStatus.open), weeklyBusinessHours.getOrElse(Nil), holidayBusinessHours.getOrElse(Nil))
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
      "promotions" -> m.promotions.asJson,
      "businessStatus" -> m.businessStatus.asJson,
      "weeklyBusinessHours" -> m.weeklyBusinessHours.asJson,
      "holidayBusinessHours" -> m.holidayBusinessHours.asJson
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

end MerchantJsonCodecs
