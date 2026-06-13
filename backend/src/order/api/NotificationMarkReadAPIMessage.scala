package delivery.order.api

import cats.effect.IO
import delivery.order.tables.notificationreadstate.NotificationReadStateTable
import delivery.platform.api.APIWithRoleMessage
import delivery.domain.apiTypes.OkResponse

import java.sql.Connection

final case class NotificationMarkReadAPIMessage(notificationId: String) extends APIWithRoleMessage[OkResponse]:
  override def plan(connection: Connection, username: String): IO[OkResponse] =
    NotificationReadStateTable.markRead(connection, username, notificationId).as(OkResponse(ok = true))
