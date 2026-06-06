import type { Promotion } from '@/objects/shared/Promotion'

export type AppliedPromotion = {
  promotion: Promotion
  discountAmount: number
}

export type PromotionLine = {
  productId: string
  unitPrice: number
  quantity: number
}

export function roundMoney(value: number) {
  return Math.round((value + Number.EPSILON) * 100) / 100
}

export function isPromotionActive(promotion: Promotion, now = new Date()) {
  if (!promotion.enabled) return false
  if (promotion.remainingUses !== null && promotion.remainingUses !== undefined && promotion.remainingUses <= 0) return false
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()
  if (promotion.startsAt) {
    const start = Date.parse(`${promotion.startsAt}T00:00:00`)
    if (!Number.isNaN(start) && start > today) return false
  }
  if (promotion.endsAt) {
    const end = Date.parse(`${promotion.endsAt}T00:00:00`)
    if (!Number.isNaN(end) && end < today) return false
  }
  if (promotion.dailyStartTime && promotion.dailyEndTime) {
    const current = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}`
    const start = promotion.dailyStartTime
    const end = promotion.dailyEndTime
    if (start !== end) {
      const inWindow = start < end ? current >= start && current < end : current >= start || current < end
      if (!inWindow) return false
    }
  }
  return true
}

export function bestPromotion(promotions: Promotion[] | undefined, amount: number, itemCount: number, lines: PromotionLine[] = []): AppliedPromotion | null {
  const candidates = (promotions ?? [])
    .filter((promotion) => isPromotionActive(promotion))
    .filter((promotion) => {
      if (promotion.triggerType === 'none') return true
      if (promotion.triggerType === 'amount') return amount >= promotion.triggerValue
      if (promotion.triggerType === 'items') return itemCount >= promotion.triggerValue
      return false
    })
    .map((promotion) => {
      const rawDiscount =
        promotion.discountType === 'amount'
          ? promotion.discountValue
          : promotion.discountType === 'percent'
            ? amount * (10 - promotion.discountValue) / 10
            : Math.min(
                promotion.discountValue,
                lines
                  .filter((line) => (promotion.productIds ?? []).includes(line.productId))
                  .reduce((sum, line) => sum + line.unitPrice * line.quantity, 0),
              )
      return {
        promotion,
        discountAmount: roundMoney(Math.max(0, Math.min(rawDiscount, amount))),
      }
    })
    .filter((item) => item.discountAmount > 0)
    .sort((a, b) => b.discountAmount - a.discountAmount || a.promotion.title.localeCompare(b.promotion.title))

  return candidates[0] ?? null
}

export function promotionSummary(promotion: Promotion) {
  const discount =
    promotion.discountType === 'amount'
      ? `减 ¥${promotion.discountValue.toFixed(0)}`
      : promotion.discountType === 'percent'
        ? `${promotion.discountValue.toFixed(1)} 折`
        : `指定菜品减 ¥${promotion.discountValue.toFixed(0)}`
  const trigger =
    promotion.triggerType === 'none'
      ? '无门槛'
      : promotion.triggerType === 'amount'
        ? `满 ¥${promotion.triggerValue.toFixed(0)}`
        : `满 ${promotion.triggerValue.toFixed(0)} 件`
  const time =
    promotion.startsAt || promotion.endsAt
      ? `${promotion.startsAt ?? '不限'} 至 ${promotion.endsAt ?? '不限'}`
      : '不限时'
  const daily = promotion.dailyStartTime && promotion.dailyEndTime ? ` · 每天 ${promotion.dailyStartTime}-${promotion.dailyEndTime}` : ''
  return `${trigger} ${discount} · ${time}${daily}`
}

export function promotionDisplayName(promotion: Promotion) {
  if (promotion.remainingUses !== null && promotion.remainingUses !== undefined) {
    return `${promotion.remainingUses}张优惠券`
  }
  if (promotion.usageLimit !== null && promotion.usageLimit !== undefined) {
    return `${promotion.usageLimit}张优惠券`
  }
  return '优惠'
}
