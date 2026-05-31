import { UserRoles } from '@/objects/shared/ids'
import type { UserRole } from '@/objects/shared/ids'
import type { Order } from '@/objects/order/Order'
import type { RiderAccountPublic } from './RiderAccountPublic'
import type { RiderDeliveryStatus } from './RiderDeliveryStatus'

export interface RiderMeResponse {
  username: string
  role: typeof UserRoles.rider & UserRole
  riderAccount: RiderAccountPublic
  availableOrders: Order[]
  deliveryStatuses: RiderDeliveryStatus[]
}
