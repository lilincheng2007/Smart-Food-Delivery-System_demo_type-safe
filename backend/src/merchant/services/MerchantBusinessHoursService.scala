package delivery.merchant.services

import delivery.merchant.objects.{Merchant, MerchantHolidayBusinessHour, MerchantWeeklyBusinessHour}
import delivery.merchant.validators.MerchantBusinessHoursValidator

import java.time.{LocalDate, LocalTime, ZonedDateTime}
import scala.util.Try

object MerchantBusinessHoursService:
  val Open = MerchantBusinessHoursValidator.Open
  val Resting = MerchantBusinessHoursValidator.Resting
  val ClosedToday = MerchantBusinessHoursValidator.ClosedToday
  val Paused = MerchantBusinessHoursValidator.Paused

  def normalizeStatus(value: String): String =
    MerchantBusinessHoursValidator.normalizeStatus(value)

  def isAcceptingOrders(merchant: Merchant, now: ZonedDateTime = ZonedDateTime.now()): Boolean =
    availability(merchant, now).isOpen

  def unavailableMessage(merchant: Merchant, now: ZonedDateTime = ZonedDateTime.now()): String =
    val value = availability(merchant, now)
    if value.isOpen then "店铺营业中"
    else value.nextOpenText.fold(s"店铺${statusLabel(value.status)}，暂不可下单")(next => s"店铺${statusLabel(value.status)}，预计 $next 开始营业")

  def availability(merchant: Merchant, now: ZonedDateTime = ZonedDateTime.now()): Availability =
    val today = now.toLocalDate
    val currentTime = now.toLocalTime
    merchant.holidayBusinessHours.find(_.date == today.toString) match
      case Some(holiday) => holidayAvailability(merchant, holiday, today, currentTime)
      case None => regularAvailability(merchant, today, currentTime, normalizeStatus(merchant.businessStatus))

  private def holidayAvailability(merchant: Merchant, holiday: MerchantHolidayBusinessHour, today: LocalDate, currentTime: LocalTime): Availability =
    val status = normalizeStatus(holiday.businessStatus)
    if status != Open then Availability(false, status, nextOpenText(merchant, today, currentTime))
    else
      (holiday.startTime.flatMap(parseTime), holiday.endTime.flatMap(parseTime)) match
        case (Some(start), Some(end)) => Availability(inWindow(currentTime, start, end), Open, if inWindow(currentTime, start, end) then None else Some(formatTime(start)))
        case _ => regularAvailability(merchant, today, currentTime, Open)

  private def regularAvailability(merchant: Merchant, today: LocalDate, currentTime: LocalTime, status: String): Availability =
    if status != Open then Availability(false, status, nextOpenText(merchant, today, currentTime))
    else
      val todayHours = merchant.weeklyBusinessHours.filter(hour => hour.enabled && hour.dayOfWeek == today.getDayOfWeek.getValue)
      if merchant.weeklyBusinessHours.isEmpty then Availability(true, Open, None)
      else if todayHours.exists(hour => parseTime(hour.startTime).zip(parseTime(hour.endTime)).exists((start, end) => inWindow(currentTime, start, end))) then Availability(true, Open, None)
      else Availability(false, Resting, nextOpenText(merchant, today, currentTime))

  private def nextOpenText(merchant: Merchant, today: LocalDate, currentTime: LocalTime): Option[String] =
    if merchant.weeklyBusinessHours.isEmpty then None
    else
      (0 to 7).toList.flatMap { offset =>
        val date = today.plusDays(offset.toLong)
        val hours = merchant.weeklyBusinessHours
          .filter(hour => hour.enabled && hour.dayOfWeek == date.getDayOfWeek.getValue)
          .flatMap(hour => parseTime(hour.startTime).map(time => (date, time)))
          .filter { case (date, time) => offset > 0 || time.isAfter(currentTime) }
          .sortBy(_._2)
        hours.headOption
      }.headOption.map { case (date, time) =>
        if date == today then formatTime(time) else s"${date.toString} ${formatTime(time)}"
      }

  private def parseTime(value: String): Option[LocalTime] = Try(LocalTime.parse(value)).toOption

  private def inWindow(current: LocalTime, start: LocalTime, end: LocalTime): Boolean =
    if start == end then false
    else if start.isBefore(end) then !current.isBefore(start) && current.isBefore(end)
    else !current.isBefore(start) || current.isBefore(end)

  private def formatTime(value: LocalTime): String = f"${value.getHour}%02d:${value.getMinute}%02d"

  private def statusLabel(status: String): String =
    normalizeStatus(status) match
      case Open => "营业中"
      case Resting => "休息中"
      case ClosedToday => "今日打烊"
      case Paused => "临时暂停接单"
      case _ => "休息中"

  final case class Availability(isOpen: Boolean, status: String, nextOpenText: Option[String])

end MerchantBusinessHoursService
