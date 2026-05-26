import type { Order } from './Order'

export interface OrderCancelResponse {
  order: Order
  walletBalance: number
}
