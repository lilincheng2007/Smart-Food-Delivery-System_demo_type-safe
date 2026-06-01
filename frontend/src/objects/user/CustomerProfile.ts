import type { Order } from '@/objects/order/Order'
import type { UserId } from '@/objects/shared/ids'
import type { Voucher } from '@/objects/shared/Voucher'

import type { CustomerDeliveryContact } from './CustomerDeliveryContact'

export interface CustomerProfile {
  id: UserId
  name: string
  phone: string
  defaultAddress: string
  vouchers: Voucher[]
  walletBalance: number
  pendingOrders: Order[]
  historyOrders: Order[]
  /** 收货用联系人组；旧数据可能缺省，前端按 name/phone/defaultAddress 兜底 */
  deliveryContacts?: CustomerDeliveryContact[] | null
  foodiePoints: number
  foodieLevel: number
}
