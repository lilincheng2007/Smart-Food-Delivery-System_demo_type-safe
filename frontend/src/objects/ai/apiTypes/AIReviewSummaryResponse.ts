import type { MerchantId } from '@/objects/shared/ids'

export interface AIReviewSummaryResponse {
  merchantId: MerchantId
  storeName: string
  summary: string
  highlights: string[]
  reviewCount: number
}
