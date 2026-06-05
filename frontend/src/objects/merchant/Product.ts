import type { InventoryStatus, ListingStatus, MerchantId, ProductId } from '@/objects/shared/ids'

export interface Product {
  id: ProductId
  merchantId: MerchantId
  name: string
  price: number
  description: string
  imageUrl: string
  categoryName?: string
  monthlySales: number
  remainingStock: number
  listingStatus: ListingStatus
  inventoryStatus: InventoryStatus
  discountText?: string
}
