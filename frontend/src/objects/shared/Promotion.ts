export type PromotionDiscountType = 'amount' | 'percent' | 'productAmount'
export type PromotionTriggerType = 'none' | 'amount' | 'items'

export interface Promotion {
  id: string
  title: string
  discountType: PromotionDiscountType
  discountValue: number
  triggerType: PromotionTriggerType
  triggerValue: number
  startsAt?: string | null
  endsAt?: string | null
  dailyStartTime?: string | null
  dailyEndTime?: string | null
  productIds?: string[]
  usageLimit?: number | null
  remainingUses?: number | null
  enabled: boolean
}
