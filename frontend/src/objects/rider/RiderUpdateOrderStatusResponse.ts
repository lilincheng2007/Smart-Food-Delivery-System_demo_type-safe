import type { Order } from '@/objects/order/Order'

export interface RiderUpdateOrderStatusResponse {
  ok: boolean
  order: Order
}
