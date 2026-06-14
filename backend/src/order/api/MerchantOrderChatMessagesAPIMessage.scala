package delivery.order.api

import delivery.order.services.OrderChatAccessService
import cats.effect.IO
import delivery.order.objects.OrderChatRole
import delivery.order.objects.apiTypes.OrderChatMessagesResponse
import delivery.order.tables.orderchat.OrderChatMessageTable
import delivery.platform.api.APIWithRoleMessage
import delivery.domain.OrderId

import java.sql.Connection

final case class MerchantOrderChatMessagesAPIMessage(orderId: OrderId, peerRole: OrderChatRole) extends APIWithRoleMessage[OrderChatMessagesResponse]:
  override def plan(connection: Connection, username: String): IO[OrderChatMessagesResponse] =
    for
      _ <- OrderChatAccessService.requireOrderForRole(connection, username, OrderChatRole.merchant, orderId, peerRole)
      _ <- OrderChatMessageTable.markReadForPair(connection, orderId, OrderChatRole.merchant, peerRole)
      messages <- OrderChatMessageTable.listForPair(connection, orderId, OrderChatRole.merchant, peerRole)
    yield OrderChatMessagesResponse(messages)
