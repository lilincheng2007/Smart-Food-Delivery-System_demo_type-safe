import type { ProductId } from '@/objects/shared/ids'

export interface AIGeneratedProductDescription {
  productId: ProductId
  productName: string
  description: string
}
