package delivery.promotion.validators

import delivery.merchant.objects.Product
import delivery.domain.Promotion

object PromotionValidator:
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

  def validateMerchantProductPromotions(promotions: List[Promotion], products: List[Product]): Option[String] =
    promotions.collectFirst {
      case promotion if promotion.discountType == "productAmount" =>
        val matchedProducts = products.filter(product => promotion.productIds.contains(product.id))
        if matchedProducts.isEmpty then Some("菜品优惠必须关联本店菜品")
        else matchedProducts.collectFirst {
          case product if roundMoney(product.price - promotion.discountValue) <= 0 =>
            s"${product.name} 的优惠后价格必须大于 0 元"
        }
    }.flatten

  private def roundMoney(value: Double): Double =
    BigDecimal(value).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble

end PromotionValidator
