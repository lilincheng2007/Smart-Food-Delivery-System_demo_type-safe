package delivery.merchant.objects

enum MerchantBusinessStatus derives CanEqual:
  case open, resting, closedToday, paused
end MerchantBusinessStatus

object MerchantBusinessStatus:
  def fromString(value: String): Option[MerchantBusinessStatus] =
    values.find(_.toString == value.trim)

  def normalize(value: String): MerchantBusinessStatus =
    fromString(value).getOrElse(open)

end MerchantBusinessStatus
