package delivery.order.api

import cats.effect.IO
import cats.syntax.all.*
import delivery.order.objects.{Order, OrderChatMessage}
import delivery.order.tables.order.OrderTable
import delivery.order.tables.orderchat.OrderChatMessageTable
import delivery.platform.api.HttpApiError
import delivery.domain.OrderStatus

import java.sql.Connection
import java.time.Instant
import java.util.UUID

object OrderStatusTransitionService:
  def transition(
      connection: Connection,
      order: Order,
      targetStatus: OrderStatus,
      actorRole: String,
      patch: Order => Order = identity
  ): IO[Order] =
    for
      _ <- validate(order.status, targetStatus, actorRole)
      now <- IO.realTime.map(duration => Instant.ofEpochMilli(duration.toMillis).toString)
      updated = OrderStatusTimelineSupport.appendTransitionEvents(patch(order.copy(status = targetStatus)), order.status, targetStatus, actorRole, now)
      _ <- OrderTable.upsert(connection, updated)
      _ <- emitTransitionMessages(connection, order, updated, actorRole)
    yield updated

  def canTransition(from: OrderStatus, to: OrderStatus, actorRole: String): Boolean =
    OrderStatusTransitionRules.canTransition(from, to, actorRole)

  private def validate(from: OrderStatus, to: OrderStatus, actorRole: String): IO[Unit] =
    if canTransition(from, to, actorRole) then IO.unit
    else IO.raiseError(HttpApiError.BadRequest(OrderStatusTransitionRules.invalidTransitionMessage(from, to, actorRole)))

  private def emitTransitionMessages(connection: Connection, previous: Order, updated: Order, actorRole: String): IO[Unit] =
    (previous.status, updated.status, actorRole) match
      case (OrderStatus.待商家接单, OrderStatus.制作中, "merchant") =>
        createSystemChatMessage(
          connection,
          updated,
          senderRole = "merchant",
          peerRole = "customer",
          content = OrderChatNotificationTemplates.merchantOrderAccepted(updated)
        )
      case (OrderStatus.制作中, OrderStatus.待骑手接单, "merchant") =>
        createSystemChatMessage(
          connection,
          updated,
          senderRole = "merchant",
          peerRole = "customer",
          content = OrderChatNotificationTemplates.merchantOrderReady(updated)
        )
      case (OrderStatus.配送中, OrderStatus.已送达, "rider") =>
        createSystemChatMessage(
          connection,
          updated,
          senderRole = "rider",
          peerRole = "customer",
          content = OrderChatNotificationTemplates.riderOrderDelivered(updated)
        )
      case _ => IO.unit

  private def createSystemChatMessage(
      connection: Connection,
      order: Order,
      senderRole: String,
      peerRole: String,
      content: String
  ): IO[Unit] =
    OrderChatMessageTable.create(
      connection,
      OrderChatMessage(
        id = UUID.randomUUID().toString,
        orderId = order.id,
        senderRole = senderRole,
        peerRole = peerRole,
        messageType = "text",
        content = content,
        createdAt = Instant.now().toString
      )
    ).void

end OrderStatusTransitionService
