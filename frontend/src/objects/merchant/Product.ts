import type { InventoryStatus, ListingStatus, MerchantId, ProductId } from '@/objects/shared/ids'
import type { ProductBundleSelectionType } from './ProductBundleSelectionType'
import type { ProductInventoryMode } from './ProductInventoryMode'

export type { ProductInventoryMode } from './ProductInventoryMode'
export type ProductBundleGroupSelectionType = ProductBundleSelectionType

export interface ProductBundleOption {
  productId: ProductId
  recommended: boolean
  extraPrice: number
  customExtraPrice?: boolean
}

export interface ProductBundleGroup {
  id: string
  name: string
  quantity: number
  selectionType: ProductBundleGroupSelectionType
  includedPrice: number
  options: ProductBundleOption[]
}

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
  inventoryMode?: ProductInventoryMode
  maxPerOrder?: number | null
  discountText?: string
  bundleGroups?: ProductBundleGroup[]
}
