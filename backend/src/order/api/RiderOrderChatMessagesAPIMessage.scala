package delivery.order.api

import delivery.order.services.OrderChatAccessService
import cats.effect.IO
import delivery.order.objects.OrderChatRole
import delivery.order.objects.apiTypes.OrderChatMessagesResponse
import delivery.order.tables.orderchat.OrderChatMessageTable
import delivery.platform.api.APIWithRoleMessage
import delivery.domain.OrderId

import java.sql.Connection

final case class RiderOrderChatMessagesAPIMessage(orderId: OrderId, peerRole: OrderChatRole) extends APIWithRoleMessage[OrderChatMessagesResponse]:
  override def plan(connection: Connection, username: String): IO[OrderChatMessagesResponse] =
    for
      _ <- OrderChatAccessService.requireOrderForRole(connection, username, OrderChatRole.rider, orderId, peerRole)
      _ <- OrderChatMessageTable.markReadForPair(connection, orderId, OrderChatRole.rider, peerRole)
      messages <- OrderChatMessageTable.listForPair(connection, orderId, OrderChatRole.rider, peerRole)
    yield OrderChatMessagesResponse(messages)
