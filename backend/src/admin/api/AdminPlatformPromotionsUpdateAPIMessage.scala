package delivery.admin.api

import cats.effect.IO
import delivery.admin.tables.platformpromotion.PlatformPromotionTable
import delivery.platform.api.{APIWithRoleMessage, HttpApiError}
import delivery.domain.Promotion
import delivery.domain.apiTypes.OkResponse
import delivery.promotion.services.PromotionValidation

import java.sql.Connection

final case class AdminPlatformPromotionsUpdateAPIMessage(promotions: List[Promotion]) extends APIWithRoleMessage[OkResponse]:
  override def plan(connection: Connection, username: String): IO[OkResponse] =
    PromotionValidation.validate(promotions) match
      case Some(message) => IO.raiseError(HttpApiError.BadRequest(message))
      case None          => PlatformPromotionTable.set(connection, promotions.take(20)).as(OkResponse(ok = true))

