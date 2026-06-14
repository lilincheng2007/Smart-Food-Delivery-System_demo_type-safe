package delivery.promotion.objects

import delivery.domain.ProductId

final case class Promotion(
    id: String,
    title: String,
    discountType: PromotionDiscountType,
    discountValue: Double,
    triggerType: PromotionTriggerType,
    triggerValue: Double,
    startsAt: Option[String],
    endsAt: Option[String],
    dailyStartTime: Option[String] = None,
    dailyEndTime: Option[String] = None,
    productIds: List[ProductId] = Nil,
    usageLimit: Option[Int] = None,
    remainingUses: Option[Int] = None,
    enabled: Boolean
)
