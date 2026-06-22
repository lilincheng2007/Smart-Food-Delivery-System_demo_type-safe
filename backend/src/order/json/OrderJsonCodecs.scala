package delivery.order.json

import delivery.domain.*
import delivery.order.api.*
import delivery.order.objects.*
import delivery.order.objects.apiTypes.*
import delivery.order.tables.*
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

object OrderJsonCodecs:

  given Codec[CustomerOrdersAPIMessage] = deriveCodec
  given Codec[OrderDetailAPIMessage] = deriveCodec
  given Codec[OrderCancelAPIMessage] = deriveCodec
  given Codec[OrderCompleteAPIMessage] = deriveCodec
  given Codec[OrderRefundRequestAPIMessage] = deriveCodec
  given Codec[OrderRefundAppealAPIMessage] = deriveCodec
  given Codec[CustomerRefundImageFileAPIMessage] = deriveCodec
  given Codec[CustomerOrderImageFileAPIMessage] = deriveCodec
  given Codec[MerchantOrderImageFileAPIMessage] = deriveCodec
  given Codec[RiderOrderImageFileAPIMessage] = deriveCodec
  given Codec[CustomerOrderChatMessagesAPIMessage] = deriveCodec
  given Codec[CustomerSendOrderChatMessageAPIMessage] = deriveCodec
  given Codec[CustomerOrderChatUnreadCountsAPIMessage] = deriveCodec
  given Codec[MerchantOrderChatMessagesAPIMessage] = deriveCodec
  given Codec[MerchantSendOrderChatMessageAPIMessage] = deriveCodec
  given Codec[MerchantOrderChatUnreadCountsAPIMessage] = deriveCodec
  given Codec[RiderOrderChatMessagesAPIMessage] = deriveCodec
  given Codec[RiderSendOrderChatMessageAPIMessage] = deriveCodec
  given Codec[RiderOrderChatUnreadCountsAPIMessage] = deriveCodec
  given Codec[NotificationFeedAPIMessage] = deriveCodec
  given Codec[NotificationReadStatesAPIMessage] = deriveCodec
  given Codec[NotificationMarkReadAPIMessage] = deriveCodec
  given Codec[NotificationMarkAllReadAPIMessage] = deriveCodec
  given Codec[CheckoutQuoteAPIMessage] = deriveCodec
  given Codec[CheckoutAPIMessage] = deriveCodec

  given Codec[OrderChatRole] = enumCodec("订单聊天角色", OrderChatRole.values)
  given Codec[OrderChatMessageType] = enumCodec("订单聊天消息类型", OrderChatMessageType.values)

  given Codec[OrderItem] = deriveCodec
  given Codec[OrderPriceSnapshotItem] = deriveCodec
  given Codec[OrderPriceSnapshot] = deriveCodec
  given Codec[OrderPriceBreakdownLine] = deriveCodec
  given Codec[OrderPriceBreakdown] = deriveCodec
  given Codec[OrderTimelineEvent] = deriveCodec

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
      priceSnapshot <- c.downField("priceSnapshot").as[Option[OrderPriceSnapshot]]
      priceBreakdown <- c.downField("priceBreakdown").as[Option[OrderPriceBreakdown]]
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
      statusTimeline <- c.downField("statusTimeline").as[Option[List[OrderTimelineEvent]]]
      estimatedPrepMinutes <- c.downField("estimatedPrepMinutes").as[Option[Int]]
      estimatedReadyAt <- c.downField("estimatedReadyAt").as[Option[String]]
      prepDelayReason <- c.downField("prepDelayReason").as[Option[String]]
      prepDelayedAt <- c.downField("prepDelayedAt").as[Option[String]]
      prepTimeoutNotifiedAt <- c.downField("prepTimeoutNotifiedAt").as[Option[String]]
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
      priceSnapshot,
      priceBreakdown,
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
      customerNoteImageUrl,
      statusTimeline.getOrElse(Nil),
      estimatedPrepMinutes,
      estimatedReadyAt,
      prepDelayReason,
      prepDelayedAt,
      prepTimeoutNotifiedAt
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
    o.priceSnapshot.foreach(value => fields += "priceSnapshot" -> value.asJson)
    o.priceBreakdown.foreach(value => fields += "priceBreakdown" -> value.asJson)
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
    fields += "statusTimeline" -> o.statusTimeline.asJson
    o.estimatedPrepMinutes.foreach(value => fields += "estimatedPrepMinutes" -> value.asJson)
    o.estimatedReadyAt.foreach(value => fields += "estimatedReadyAt" -> value.asJson)
    o.prepDelayReason.foreach(value => fields += "prepDelayReason" -> value.asJson)
    o.prepDelayedAt.foreach(value => fields += "prepDelayedAt" -> value.asJson)
    o.prepTimeoutNotifiedAt.foreach(value => fields += "prepTimeoutNotifiedAt" -> value.asJson)
    Json.obj(fields.result()*)
  }

  given Decoder[Order] = orderDecoder0
  given Encoder[Order] = orderEncoder0
  given Codec[Order] = Codec.from(orderDecoder0, orderEncoder0)
  given Codec[OrderChatMessage] = deriveCodec
  given Codec[OrderChatUnreadCount] = deriveCodec

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
  given Codec[CheckoutQuoteResponse] = deriveCodec
  given Codec[CheckoutResponse] = deriveCodec
  given Codec[NotificationFeedItem] = deriveCodec
  given Codec[NotificationFeedResponse] = deriveCodec
  given Codec[NotificationReadStatesResponse] = deriveCodec
  given Codec[CustomerOrdersResponse] = deriveCodec
  given Codec[OrderCancelResponse] = deriveCodec
  given Codec[OrderRefundRequestResponse] = deriveCodec
  given Codec[OrderChatMessagesResponse] = deriveCodec
  given Codec[OrderChatUnreadCountsResponse] = deriveCodec

end OrderJsonCodecs
