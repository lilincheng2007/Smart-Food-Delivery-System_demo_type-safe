package delivery.merchant.api

import cats.effect.IO
import delivery.merchant.objects.{MerchantHolidayBusinessHour, MerchantWeeklyBusinessHour}
import delivery.merchant.tables.merchantstore.MerchantStoreTable
import delivery.merchant.validators.{MerchantBusinessHoursValidator, MerchantStoreOwnershipValidator}
import delivery.platform.api.{APIWithRoleMessage, HttpApiError}
import delivery.domain.MerchantId
import delivery.domain.apiTypes.OkResponse

import java.sql.Connection

final case class MerchantBusinessHoursAPIMessage(
    merchantId: MerchantId,
    businessStatus: String,
    weeklyBusinessHours: List[MerchantWeeklyBusinessHour],
    holidayBusinessHours: List[MerchantHolidayBusinessHour]
) extends APIWithRoleMessage[OkResponse]:
  override def plan(connection: Connection, username: String): IO[OkResponse] =
    for
      normalized <- MerchantBusinessHoursValidator.validateAndNormalize(businessStatus, weeklyBusinessHours, holidayBusinessHours) match
        case Left(message) => IO.raiseError(HttpApiError.BadRequest(message))
        case Right(value)  => IO.pure(value)
      merchant <- MerchantStoreOwnershipValidator.requireOwnedStore(connection, username, merchantId)
      updated = merchant.copy(
        businessStatus = normalized.businessStatus,
        weeklyBusinessHours = normalized.weeklyBusinessHours,
        holidayBusinessHours = normalized.holidayBusinessHours
      )
      _ <- MerchantStoreTable.upsert(connection, username, updated)
    yield OkResponse(ok = true)
