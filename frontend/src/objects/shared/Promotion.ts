import type { PromotionDiscountType } from './PromotionDiscountType'
import type { PromotionTriggerType } from './PromotionTriggerType'

export type { PromotionDiscountType } from './PromotionDiscountType'
export type { PromotionTriggerType } from './PromotionTriggerType'

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
