import type { ListingStatus } from '@/objects/shared/ids'
import type { ProductBundleGroup } from '@/objects/merchant/Product'

export interface UpdateProductRequest {
  name: string
  description: string
  imageUrl: string
  categoryName: string
  price: number
  remainingStock: number
  listingStatus: ListingStatus
  bundleGroups?: ProductBundleGroup[]
}
