import type { MerchantId } from '@/objects/shared/ids'

export interface AIMerchantStoreDescriptionResponse {
  merchantId: MerchantId
  description: string
  generatedAt: string
}
