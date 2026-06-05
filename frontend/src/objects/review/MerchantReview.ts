import type { MerchantId, OrderId, UserId } from '@/objects/shared/ids'

export interface MerchantReview {
  id: string
  orderId: OrderId
  merchantId: MerchantId
  customerId: UserId
  customerName: string
  rating: number
  description: string
  imageUrl?: string | null
  upvotes: number
  downvotes: number
  createdAt: string
  merchantReply?: string | null
  merchantReplyAt?: string | null
  orderItemNames?: string[]
}
