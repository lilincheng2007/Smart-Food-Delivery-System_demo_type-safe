package delivery.merchant.validators

import delivery.merchant.objects.{MerchantHolidayBusinessHour, MerchantWeeklyBusinessHour}

import java.time.{LocalDate, LocalTime}
import scala.util.Try

object MerchantBusinessHoursValidator:
  val Open = "open"
  val Resting = "resting"
  val ClosedToday = "closedToday"
  val Paused = "paused"

  private val AllowedStatuses = Set(Open, Resting, ClosedToday, Paused)
  private val MaxWeeklyHours = 28
  private val MaxHolidayHours = 40

  final case class NormalizedBusinessHours(
      businessStatus: String,
      weeklyBusinessHours: List[MerchantWeeklyBusinessHour],
      holidayBusinessHours: List[MerchantHolidayBusinessHour]
  )

  def normalizeStatus(value: String): String =
    val trimmed = value.trim
    if AllowedStatuses.contains(trimmed) then trimmed else Open

  def validateAndNormalize(
      businessStatus: String,
      weeklyBusinessHours: List[MerchantWeeklyBusinessHour],
      holidayBusinessHours: List[MerchantHolidayBusinessHour]
  ): Either[String, NormalizedBusinessHours] =
    val normalizedWeekly = weeklyBusinessHours.take(MaxWeeklyHours)
    val normalizedHoliday = holidayBusinessHours.take(MaxHolidayHours).map(item => item.copy(businessStatus = normalizeStatus(item.businessStatus)))
    for
      _ <- validateWeeklyHours(normalizedWeekly)
      _ <- validateHolidayHours(normalizedHoliday)
    yield NormalizedBusinessHours(normalizeStatus(businessStatus), normalizedWeekly, normalizedHoliday)

  private def validateWeeklyHours(hours: List[MerchantWeeklyBusinessHour]): Either[String, Unit] =
    hours.zipWithIndex.collectFirst {
      case (hour, index) if hour.dayOfWeek < 1 || hour.dayOfWeek > 7 =>
        s"第 ${index + 1} 条每周营业时间的星期不合法"
      case (hour, index) if parseTime(hour.startTime).isEmpty || parseTime(hour.endTime).isEmpty =>
        s"第 ${index + 1} 条每周营业时间格式不合法"
      case (hour, index) if hour.startTime == hour.endTime =>
        s"第 ${index + 1} 条每周营业时间开始和结束不能相同"
    }.toLeft(())

  private def validateHolidayHours(hours: List[MerchantHolidayBusinessHour]): Either[String, Unit] =
    hours.zipWithIndex.collectFirst {
      case (hour, index) if parseDate(hour.date).isEmpty =>
        s"第 ${index + 1} 条节假日营业日期不合法"
      case (hour, index) if !AllowedStatuses.contains(hour.businessStatus.trim) =>
        s"第 ${index + 1} 条节假日营业状态不合法"
      case (hour, index) if hour.startTime.exists(parseTime(_).isEmpty) || hour.endTime.exists(parseTime(_).isEmpty) =>
        s"第 ${index + 1} 条节假日营业时间格式不合法"
      case (hour, index) if hour.startTime.exists(_.nonEmpty) != hour.endTime.exists(_.nonEmpty) =>
        s"第 ${index + 1} 条节假日营业时间需要同时填写开始和结束"
      case (hour, index) if hour.startTime.zip(hour.endTime).exists((start, end) => start == end) =>
        s"第 ${index + 1} 条节假日营业时间开始和结束不能相同"
    }.toLeft(())

  private def parseDate(value: String): Option[LocalDate] =
    Try(LocalDate.parse(value.trim)).toOption

  private def parseTime(value: String): Option[LocalTime] =
    Try(LocalTime.parse(value.trim)).toOption

end MerchantBusinessHoursValidator
