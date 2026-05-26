import type { MerchantMeResponse } from '@/objects/merchant/MerchantMeResponse'
import type { RiderMeResponse } from '@/objects/rider/RiderMeResponse'
import type { CustomerMeResponse } from './CustomerMeResponse'

export type MeResponse = CustomerMeResponse | MerchantMeResponse | RiderMeResponse
