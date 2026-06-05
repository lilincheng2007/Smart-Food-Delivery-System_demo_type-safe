import type { MerchantCategory, ProductId, MerchantId } from '@/objects/shared/ids'

export interface Merchant {
  id: MerchantId
  storeName: string
  category: MerchantCategory
  address: string
  phone: string
  rating: number
  tags: string[]
  featuredProductIds: ProductId[]
  /** 店铺头图，http(s) 链接；未设置时后端可能省略或为 null */
  imageUrl?: string | null
  description: string
  announcement: string
}
