import type { MerchantId } from '@/objects/shared/ids'

export interface AIMerchantProductDescriptionsRequest {
  merchantId: MerchantId
  keywords: string
}
