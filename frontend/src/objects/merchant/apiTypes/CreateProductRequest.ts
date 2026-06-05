import type { ListingStatus, MerchantId } from '@/objects/shared/ids'

export interface CreateProductRequest {
  merchantId: MerchantId
  name: string
  description: string
  imageUrl: string
  categoryName: string
  price: number
  remainingStock: number
  listingStatus: ListingStatus
}
