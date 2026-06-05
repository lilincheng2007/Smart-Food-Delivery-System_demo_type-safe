import type { ListingStatus } from '@/objects/shared/ids'

export interface UpdateProductRequest {
  name: string
  description: string
  imageUrl: string
  categoryName: string
  price: number
  remainingStock: number
  listingStatus: ListingStatus
}
