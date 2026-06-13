package delivery.admin.api

import cats.effect.IO
import delivery.admin.objects.apiTypes.PlatformPromotionsResponse
import delivery.admin.tables.platformpromotion.PlatformPromotionTable
import delivery.platform.api.APIWithRoleMessage

import java.sql.Connection

final case class AdminPlatformPromotionsAPIMessage() extends APIWithRoleMessage[PlatformPromotionsResponse]:
  override def plan(connection: Connection, username: String): IO[PlatformPromotionsResponse] =
    PlatformPromotionTable.get(connection).map(PlatformPromotionsResponse.apply)

