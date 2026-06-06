import { APIMessage } from '@/apis/shared/APIMessage'
import type { TaskIO } from '@/apis/shared/TaskIO'
import { sendAPI } from '@/apis/shared/sendAPI'
import type { Promotion } from '@/objects/shared/Promotion'

export interface PlatformPromotionsResponse {
  promotions: Promotion[]
}

class AdminPlatformPromotionsAPI extends APIMessage<PlatformPromotionsResponse> {
  readonly apiName = 'adminplatformpromotionsapi'
}

export function fetchAdminPlatformPromotionsIO(): TaskIO<PlatformPromotionsResponse> {
  return sendAPI(new AdminPlatformPromotionsAPI())
}

