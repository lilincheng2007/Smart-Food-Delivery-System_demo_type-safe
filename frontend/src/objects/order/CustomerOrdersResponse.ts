import type { Order } from './Order'

export interface CustomerOrdersResponse {
  pendingOrders: Order[]
  historyOrders: Order[]
}
