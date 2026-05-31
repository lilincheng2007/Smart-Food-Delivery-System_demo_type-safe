import type { MerchantId } from '@/objects/shared/ids'

export interface AIMerchantStoreDescriptionRequest {
  merchantId: MerchantId
  keywords: string
}
