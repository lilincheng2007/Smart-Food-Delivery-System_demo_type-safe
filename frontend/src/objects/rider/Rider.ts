import type { RiderId, RiderStatus } from '@/objects/shared/ids'

export interface Rider {
  id: RiderId
  name: string
  phone: string
  realtimeLocation: string
  status: RiderStatus
  totalOrders: number
  rating: number
  station: string
  salary: number
  energyPoints: number
  timeoutCardCount: number
  timeoutCount: number
  timeoutExemptedCount: number
}
