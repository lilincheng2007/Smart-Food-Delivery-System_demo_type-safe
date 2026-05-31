export interface AIRecommendedProduct {
  productId: string
  productName: string
  price: number
  reason: string
}

export interface AIRecommendedMerchant {
  merchantId: string
  storeName: string
  category: string
  reason: string
  products: AIRecommendedProduct[]
}

export interface AISearchResponse {
  query: string
  merchants: AIRecommendedMerchant[]
  summary: string
}
