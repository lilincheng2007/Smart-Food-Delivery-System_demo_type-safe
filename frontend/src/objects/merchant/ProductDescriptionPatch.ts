import type { ProductId } from '@/objects/shared/ids'

export interface ProductDescriptionPatch {
  productId: ProductId
  description: string
}
