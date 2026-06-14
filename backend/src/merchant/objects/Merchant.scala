package delivery.merchant.objects

import delivery.domain.{MerchantCategory, MerchantId, ProductId, Promotion}

final case class Merchant(
    id: MerchantId,
    storeName: String,
    category: MerchantCategory,
    address: String,
    phone: String,
    rating: Double,
    tags: List[String],
    featuredProductIds: List[ProductId],
    imageUrl: Option[String],
    description: String = "",
    announcement: String = "",
    promotions: List[Promotion] = Nil,
    businessStatus: MerchantBusinessStatus = MerchantBusinessStatus.open,
    weeklyBusinessHours: List[MerchantWeeklyBusinessHour] = Nil,
    holidayBusinessHours: List[MerchantHolidayBusinessHour] = Nil
)
