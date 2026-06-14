package delivery.promotion.objects

enum PromotionTriggerType derives CanEqual:
  case none, amount, items
end PromotionTriggerType

object PromotionTriggerType:
  def fromString(value: String): Option[PromotionTriggerType] =
    values.find(_.toString == value.trim)

end PromotionTriggerType
