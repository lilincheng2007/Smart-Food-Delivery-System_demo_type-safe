import type { MerchantBusinessStatus } from './MerchantBusinessStatus'

export type { MerchantBusinessStatus } from './MerchantBusinessStatus'

export interface MerchantWeeklyBusinessHour {
  dayOfWeek: number
  startTime: string
  endTime: string
  enabled: boolean
}

export interface MerchantHolidayBusinessHour {
  date: string
  businessStatus: MerchantBusinessStatus
  startTime?: string | null
  endTime?: string | null
}
