import type { VoucherId } from '@/objects/shared/ids'

import type { CheckoutLine } from './CheckoutLine'

export interface CheckoutRequest {
  lines: CheckoutLine[]
  customerName?: string
  customerPhone?: string
  deliveryAddress?: string
  voucherId?: VoucherId
}
