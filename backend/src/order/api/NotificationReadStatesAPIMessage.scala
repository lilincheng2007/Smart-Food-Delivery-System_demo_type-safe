package delivery.order.api

import cats.effect.IO
import delivery.order.objects.apiTypes.NotificationReadStatesResponse
import delivery.order.tables.notificationreadstate.NotificationReadStateTable
import delivery.platform.api.APIWithRoleMessage

import java.sql.Connection

final case class NotificationReadStatesAPIMessage() extends APIWithRoleMessage[NotificationReadStatesResponse]:
  override def plan(connection: Connection, username: String): IO[NotificationReadStatesResponse] =
    NotificationReadStateTable.listReadIds(connection, username).map(NotificationReadStatesResponse(_))
