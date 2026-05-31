import type { MerchantId } from '@/objects/shared/ids'
import type { AIGeneratedProductDescription } from './AIGeneratedProductDescription'

export interface AIMerchantProductDescriptionsResponse {
  merchantId: MerchantId
  products: AIGeneratedProductDescription[]
  generatedAt: string
}
