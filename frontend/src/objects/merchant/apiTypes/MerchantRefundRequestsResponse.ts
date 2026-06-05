import type { Order } from '@/objects/order/Order'

export interface MerchantRefundRequestsResponse {
  requests: Order[]
}
