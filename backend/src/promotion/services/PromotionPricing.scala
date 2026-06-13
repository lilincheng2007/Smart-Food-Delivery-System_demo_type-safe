package delivery.promotion.services

import delivery.domain.Promotion

import java.time.{LocalDate, LocalTime}
import scala.util.Try

object PromotionPricing:
  final case class AppliedPromotion(promotion: Promotion, discountAmount: Double)
  final case class PromotionItem(productId: String, unitPrice: Double, quantity: Int)

  def roundMoney(value: Double): Double =
    BigDecimal(value).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble

  def best(promotions: List[Promotion], amount: Double, itemCount: Int, today: LocalDate = LocalDate.now()): Option[AppliedPromotion] =
    bestForItems(promotions, amount, itemCount, Nil, today, LocalTime.now())

  def bestForItems(
      promotions: List[Promotion],
      amount: Double,
      itemCount: Int,
      items: List[PromotionItem],
      today: LocalDate = LocalDate.now(),
      nowTime: LocalTime = LocalTime.now()
  ): Option[AppliedPromotion] =
    promotions
      .filter(isActive(_, today, nowTime))
      .filter(meetsTrigger(_, amount, itemCount))
      .flatMap { promotion =>
        val discount =
          promotion.discountType match
            case "amount"  => math.min(promotion.discountValue, amount)
            case "percent" => math.max(0, amount * (10 - promotion.discountValue) / 10)
            case "productAmount" =>
              val eligibleItems = items.filter(item => promotion.productIds.contains(item.productId))
              val eligibleAmount = eligibleItems.map(item => item.unitPrice * item.quantity).sum
              val eligibleQuantity = eligibleItems.map(_.quantity).sum
              if eligibleAmount > 0 then math.min(promotion.discountValue * eligibleQuantity, eligibleAmount) else 0
            case _         => 0
        if discount > 0 then Some(AppliedPromotion(promotion, roundMoney(discount))) else None
      }
      .sortBy(promotion => (-promotion.discountAmount, promotion.promotion.title))
      .headOption

  def isActive(promotion: Promotion, today: LocalDate = LocalDate.now(), nowTime: LocalTime = LocalTime.now()): Boolean =
    promotion.enabled &&
      promotion.remainingUses.forall(_ > 0) &&
      promotion.startsAt.flatMap(parseDate).forall(!_.isAfter(today)) &&
      promotion.endsAt.flatMap(parseDate).forall(!_.isBefore(today)) &&
      isWithinDailyWindow(promotion, nowTime)

  def meetsTrigger(promotion: Promotion, amount: Double, itemCount: Int): Boolean =
    promotion.triggerType match
      case "none"   => true
      case "amount" => amount >= promotion.triggerValue
      case "items"  => itemCount >= promotion.triggerValue
      case _        => false

  private def parseDate(value: String): Option[LocalDate] =
    Try(LocalDate.parse(value)).toOption

  private def parseTime(value: String): Option[LocalTime] =
    Try(LocalTime.parse(value)).toOption

  private def isWithinDailyWindow(promotion: Promotion, nowTime: LocalTime): Boolean =
    (promotion.dailyStartTime.flatMap(parseTime), promotion.dailyEndTime.flatMap(parseTime)) match
      case (Some(start), Some(end)) if start == end => false
      case (Some(start), Some(end)) if start.isBefore(end) =>
        !nowTime.isBefore(start) && nowTime.isBefore(end)
      case (Some(start), Some(end)) =>
        !nowTime.isBefore(start) || nowTime.isBefore(end)
      case _ => true
