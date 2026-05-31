import type { OrderId } from '@/objects/shared/ids'

export interface RiderDeliverySettlement {
  ok: boolean
  orderId: OrderId
  earnedEnergy: number
  currentEnergyPoints: number
  currentTimeoutCardCount: number
  wasTimeout: boolean
  timeoutCardUsed: boolean
  timeoutExempted: boolean
  overtimeSeconds: number
}
