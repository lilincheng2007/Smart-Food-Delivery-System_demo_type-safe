import type { Order } from '@/objects/order/Order'
import type { RiderAccountPublic } from './RiderAccountPublic'

export interface RiderMeResponse {
  username: string
  role: 'rider'
  riderAccount: RiderAccountPublic
  availableOrders: Order[]
}
