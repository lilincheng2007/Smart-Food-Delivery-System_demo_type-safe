package delivery.promotion.services

import delivery.domain.Promotion

object PromotionUsage:
  def decrement(promotions: List[Promotion], usedIds: Set[String]): List[Promotion] =
    promotions.map { promotion =>
      if usedIds.contains(promotion.id) && promotion.usageLimit.nonEmpty then
        promotion.copy(remainingUses = Some(math.max(0, promotion.remainingUses.getOrElse(promotion.usageLimit.getOrElse(0)) - 1)))
      else promotion
    }

