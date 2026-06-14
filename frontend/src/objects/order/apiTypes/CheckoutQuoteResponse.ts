import type { OrderPriceBreakdown } from '@/objects/order/OrderPriceBreakdown'
import type { Voucher } from '@/objects/shared/Voucher'

export interface CheckoutQuoteResponse {
  canCheckout: boolean
  failureReason?: string | null
  walletBalance: number
  originalAmount: number
  discountAmount: number
  payableAmount: number
  usedVoucher?: Voucher | null
  priceBreakdown: OrderPriceBreakdown
}
