import type { Merchant } from '../Merchant'
import type { Product } from '../Product'
import type { Promotion } from '@/objects/shared/Promotion'

export interface CatalogResponse {
  merchants: Merchant[]
  products: Product[]
  platformPromotions?: Promotion[]
}
