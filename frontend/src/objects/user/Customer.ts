import type { OrderId, UserId } from '@/objects/shared/ids'
import type { Voucher } from '../shared/Voucher'

export interface Customer {
  id: UserId
  name: string
  phone: string
  defaultAddress: string
  walletBalance: number
  orderHistoryIds: OrderId[]
  vouchers: Voucher[]
  foodiePoints: number
  foodieLevel: number
}
