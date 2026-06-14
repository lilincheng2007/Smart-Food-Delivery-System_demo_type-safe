package delivery.merchant.api

import delivery.merchant.services.MerchantBusinessService
import cats.effect.IO
import delivery.order.services.OrderChatNotificationTemplateService
import delivery.order.objects.{OrderChatMessage, OrderChatMessageType, OrderChatRole, OrderTimelineEvent}
import delivery.order.tables.order.OrderTable
import delivery.order.tables.orderchat.OrderChatMessageTable
import delivery.platform.api.{APIWithRoleMessage, HttpApiError}
import delivery.domain.{OrderId, OrderStatus}
import delivery.domain.apiTypes.OkResponse

import java.sql.Connection
import java.time.{Instant, ZoneId}
import java.time.format.DateTimeFormatter
import java.util.UUID

final case class MerchantOrderPrepDelayAPIMessage(orderId: OrderId, extraMinutes: Int, reason: String) extends APIWithRoleMessage[OkResponse]:
  override def plan(connection: Connection, username: String): IO[OkResponse] =
    val normalizedReason = reason.trim
    val safeExtraMinutes = math.max(1, math.min(180, extraMinutes))
    if normalizedReason.isEmpty then IO.raiseError(HttpApiError.BadRequest("请填写延迟备餐原因"))
    else
      for
        order <- OrderTable.findById(connection, orderId).flatMap {
          case Some(value) => IO.pure(value)
          case None        => IO.raiseError(HttpApiError.BadRequest("未找到订单"))
        }
        _ <- MerchantBusinessService.requireOwnedStore(connection, username, order.merchantId)
        _ <- if order.status == OrderStatus.制作中 then IO.unit else IO.raiseError(HttpApiError.BadRequest("只有制作中的订单可以延迟备餐"))
        now <- IO.realTime.map(duration => Instant.ofEpochMilli(duration.toMillis))
        readyAt = now.plusSeconds(safeExtraMinutes.toLong * 60)
        readyAtText = formatTime(readyAt)
        event = OrderTimelineEvent("prepDelayed", "商家延迟备餐", now.toString, Some(normalizedReason))
        updated = order.copy(
          estimatedPrepMinutes = order.estimatedPrepMinutes.map(_ + safeExtraMinutes).orElse(Some(safeExtraMinutes)),
          estimatedReadyAt = Some(readyAtText),
          prepDelayReason = Some(normalizedReason),
          prepDelayedAt = Some(now.toString),
          statusTimeline = order.statusTimeline :+ event
        )
        _ <- OrderTable.upsert(connection, updated)
        _ <- createSystemChatMessage(connection, updated, normalizedReason)
      yield OkResponse(ok = true)

  private def formatTime(instant: Instant): String =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault()).format(instant)

  private def createSystemChatMessage(connection: Connection, order: delivery.order.objects.Order, reason: String): IO[Unit] =
    OrderChatMessageTable.create(
      connection,
      OrderChatMessage(
        id = UUID.randomUUID().toString,
        orderId = order.id,
        senderRole = OrderChatRole.merchant,
        peerRole = OrderChatRole.customer,
        messageType = OrderChatMessageType.text,
        content = OrderChatNotificationTemplateService.merchantPrepDelayed(order, reason),
        createdAt = Instant.now().toString
      )
    ).void
