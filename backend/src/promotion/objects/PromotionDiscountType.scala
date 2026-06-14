package delivery.promotion.objects

enum PromotionDiscountType derives CanEqual:
  case amount, percent, productAmount
end PromotionDiscountType

object PromotionDiscountType:
  def fromString(value: String): Option[PromotionDiscountType] =
    values.find(_.toString == value.trim)

end PromotionDiscountType
