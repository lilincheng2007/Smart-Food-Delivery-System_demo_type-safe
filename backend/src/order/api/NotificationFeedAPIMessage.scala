package delivery.order.api

import cats.effect.IO
import delivery.domain.UserRole
import delivery.order.objects.apiTypes.NotificationFeedResponse
import delivery.order.services.NotificationFeedService
import delivery.platform.api.{APIWithRoleMessage, HttpApiError}

import java.sql.Connection

final case class NotificationFeedAPIMessage(
    role: String,
    cursor: Option[String] = None,
    limit: Option[Int] = None
) extends APIWithRoleMessage[NotificationFeedResponse]:
  override def plan(connection: Connection, username: String): IO[NotificationFeedResponse] =
    UserRole.fromString(role.trim) match
      case Some(value) => NotificationFeedService.feed(connection, username, value, cursor, limit)
      case None        => IO.raiseError(HttpApiError.BadRequest("无效的通知角色"))
