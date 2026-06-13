package delivery.merchant.api

import cats.effect.IO
import delivery.merchant.objects.{MerchantHolidayBusinessHour, MerchantWeeklyBusinessHour}
import delivery.merchant.services.MerchantBusinessHoursService
import delivery.merchant.tables.merchantstore.MerchantStoreTable
import delivery.merchant.utils.MerchantApiSupport
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
    val status = MerchantBusinessHoursService.normalizeStatus(businessStatus)
    val normalizedWeekly = weeklyBusinessHours
      .filter(hour => hour.dayOfWeek >= 1 && hour.dayOfWeek <= 7 && validTime(hour.startTime) && validTime(hour.endTime) && hour.startTime != hour.endTime)
      .take(28)
    val normalizedHoliday = holidayBusinessHours
      .filter(item => item.date.matches("\\d{4}-\\d{2}-\\d{2}"))
      .map(item => item.copy(businessStatus = MerchantBusinessHoursService.normalizeStatus(item.businessStatus)))
      .take(40)

    for
      _ <- if weeklyBusinessHours.length != normalizedWeekly.length then IO.raiseError(HttpApiError.BadRequest("每周营业时间包含非法日期或时间")) else IO.unit
      merchant <- MerchantApiSupport.requireOwnedStore(connection, username, merchantId)
      updated = merchant.copy(businessStatus = status, weeklyBusinessHours = normalizedWeekly, holidayBusinessHours = normalizedHoliday)
      _ <- MerchantStoreTable.upsert(connection, username, updated)
    yield OkResponse(ok = true)

  private def validTime(value: String): Boolean =
    value.matches("\\d{2}:\\d{2}")
