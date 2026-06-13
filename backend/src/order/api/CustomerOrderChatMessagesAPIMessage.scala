package delivery.order.api

import delivery.order.services.OrderChatAccessService
import cats.effect.IO
import delivery.order.objects.apiTypes.OrderChatMessagesResponse
import delivery.order.tables.orderchat.OrderChatMessageTable
import delivery.platform.api.APIWithRoleMessage
import delivery.domain.OrderId

import java.sql.Connection

final case class CustomerOrderChatMessagesAPIMessage(orderId: OrderId, peerRole: String) extends APIWithRoleMessage[OrderChatMessagesResponse]:
  override def plan(connection: Connection, username: String): IO[OrderChatMessagesResponse] =
    for
      _ <- OrderChatAccessService.requireOrderForRole(connection, username, "customer", orderId, peerRole)
      _ <- OrderChatMessageTable.markReadForPair(connection, orderId, "customer", peerRole)
      messages <- OrderChatMessageTable.listForPair(connection, orderId, "customer", peerRole)
    yield OrderChatMessagesResponse(messages)
