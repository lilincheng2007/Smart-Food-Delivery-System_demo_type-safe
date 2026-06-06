package delivery.shared.utils

import delivery.shared.objects.Promotion

object PromotionValidation:
  private val DiscountTypes = Set("amount", "percent", "productAmount")
  private val TriggerTypes = Set("none", "amount", "items")

  def validate(promotions: List[Promotion]): Option[String] =
    if promotions.size > 20 then Some("优惠最多可设置 20 条")
    else
      promotions.zipWithIndex.collectFirst {
        case (promotion, index) if promotion.title.trim.isEmpty =>
          s"第 ${index + 1} 条优惠名称不能为空"
        case (promotion, index) if !DiscountTypes.contains(promotion.discountType) =>
          s"第 ${index + 1} 条优惠类型不合法"
        case (promotion, index) if promotion.discountValue <= 0 =>
          s"第 ${index + 1} 条优惠力度必须大于 0"
        case (promotion, index) if promotion.discountType == "percent" && (promotion.discountValue <= 0 || promotion.discountValue >= 10) =>
          s"第 ${index + 1} 条折扣需大于 0 且小于 10"
        case (promotion, index) if promotion.discountType == "productAmount" && promotion.productIds.isEmpty =>
          s"第 ${index + 1} 条指定菜品优惠至少选择 1 个菜品"
        case (promotion, index) if !TriggerTypes.contains(promotion.triggerType) =>
          s"第 ${index + 1} 条触发条件不合法"
        case (promotion, index) if promotion.triggerType != "none" && promotion.triggerValue <= 0 =>
          s"第 ${index + 1} 条触发门槛必须大于 0"
        case (promotion, index) if promotion.usageLimit.exists(_ <= 0) =>
          s"第 ${index + 1} 条可使用次数必须大于 0"
        case (promotion, index) if promotion.remainingUses.exists(_ < 0) =>
          s"第 ${index + 1} 条剩余次数不能小于 0"
      }
