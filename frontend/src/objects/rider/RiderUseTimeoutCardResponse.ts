import type { OrderId } from '@/objects/shared/ids'

export interface RiderUseTimeoutCardResponse {
  ok: boolean
  orderId: OrderId
  currentTimeoutCardCount: number
  timeoutExempted: boolean
}
