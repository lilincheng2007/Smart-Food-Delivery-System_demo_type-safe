import type { Voucher } from '@/objects/shared/Voucher'

import type { Order } from './Order'

export interface CheckoutResponse {
  orders: Order[]
  walletBalance: number
  originalAmount: number
  discountAmount: number
  payableAmount: number
  usedVoucher?: Voucher | null
}
