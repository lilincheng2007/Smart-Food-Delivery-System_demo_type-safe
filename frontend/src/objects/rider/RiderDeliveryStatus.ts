import type { OrderId } from '@/objects/shared/ids'

export interface RiderDeliveryStatus {
  orderId: OrderId
  assignedAt: string
  completedAt?: string
  deadlineAt: string
  wasTimeout: boolean
  timeoutExempted: boolean
  timeoutCardUsed: boolean
  overtimeSeconds: number
  canUseTimeoutCard: boolean
}
