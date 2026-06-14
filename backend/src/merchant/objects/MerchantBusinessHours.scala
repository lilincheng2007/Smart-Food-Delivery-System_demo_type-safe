package delivery.merchant.objects

final case class MerchantWeeklyBusinessHour(
    dayOfWeek: Int,
    startTime: String,
    endTime: String,
    enabled: Boolean = true
)

final case class MerchantHolidayBusinessHour(
    date: String,
    businessStatus: MerchantBusinessStatus,
    startTime: Option[String] = None,
    endTime: Option[String] = None
)
