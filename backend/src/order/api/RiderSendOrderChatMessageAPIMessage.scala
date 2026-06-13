package delivery.order.api

import delivery.order.services.OrderChatAccessService
import cats.effect.IO
import delivery.order.objects.OrderChatMessage
import delivery.order.objects.apiTypes.OrderChatMessagesResponse
import delivery.order.tables.orderchat.OrderChatMessageTable
import delivery.platform.api.APIWithRoleMessage
import delivery.domain.OrderId

import java.sql.Connection
import java.time.Instant
import java.util.UUID

final case class RiderSendOrderChatMessageAPIMessage(orderId: OrderId, peerRole: String, messageType: String, content: String) extends APIWithRoleMessage[OrderChatMessagesResponse]:
  override def plan(connection: Connection, username: String): IO[OrderChatMessagesResponse] =
    for
      _ <- OrderChatAccessService.requireOrderForRole(connection, username, "rider", orderId, peerRole)
      _ <- OrderChatAccessService.validateMessage(messageType, content)
      _ <- OrderChatMessageTable.create(connection, OrderChatMessage(UUID.randomUUID().toString, orderId, "rider", peerRole, messageType, content.trim, Instant.now().toString))
      messages <- OrderChatMessageTable.listForPair(connection, orderId, "rider", peerRole)
    yield OrderChatMessagesResponse(messages)
