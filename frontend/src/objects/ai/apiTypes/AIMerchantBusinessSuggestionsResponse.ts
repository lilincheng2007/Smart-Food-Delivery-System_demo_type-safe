import type { MerchantId } from '@/objects/shared/ids'

export interface AIMerchantBusinessSuggestionsResponse {
  merchantId: MerchantId
  summary: string
  suggestions: string[]
  generatedAt: string
}

