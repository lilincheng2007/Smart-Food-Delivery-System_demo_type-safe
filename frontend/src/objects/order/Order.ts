import type { MerchantId, OrderId, RiderId, UserId, OrderStatus } from '@/objects/shared/ids'
import type { Voucher } from '@/objects/shared/Voucher'

import type { OrderItem } from './OrderItem'

export interface Order {
  id: OrderId
  customerId: UserId
  customerName: string
  customerPhone: string
  merchantId: MerchantId
  riderId?: RiderId
  items: OrderItem[]
  totalAmount: number
  deliveryAddress: string
  status: OrderStatus
  placedAt: string
  originalAmount: number
  discountAmount: number
  payableAmount: number
  usedVoucher?: Voucher
  pointsAwarded: number
}
