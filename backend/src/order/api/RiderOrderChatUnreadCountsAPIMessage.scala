package delivery.order.api

import delivery.order.services.OrderChatUnreadService
import cats.effect.IO
import delivery.order.objects.OrderChatRole
import delivery.order.objects.apiTypes.OrderChatUnreadCountsResponse
import delivery.platform.api.APIWithRoleMessage

import java.sql.Connection

final case class RiderOrderChatUnreadCountsAPIMessage() extends APIWithRoleMessage[OrderChatUnreadCountsResponse]:
  override def plan(connection: Connection, username: String): IO[OrderChatUnreadCountsResponse] =
    OrderChatUnreadService.countsForRole(connection, username, OrderChatRole.rider)
