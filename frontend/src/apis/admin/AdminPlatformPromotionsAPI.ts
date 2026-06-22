import { APIMessage } from '@/apis/shared/APIMessage'
import type { TaskIO } from '@/apis/shared/TaskIO'
import { sendAPI } from '@/apis/shared/sendAPI'
import type { PlatformPromotionsResponse } from '@/objects/admin/apiTypes/PlatformPromotionsResponse'

class AdminPlatformPromotionsAPI extends APIMessage<PlatformPromotionsResponse> {
  readonly apiName = 'adminplatformpromotionsapi'
}

export function fetchAdminPlatformPromotionsIO(): TaskIO<PlatformPromotionsResponse> {
  return sendAPI(new AdminPlatformPromotionsAPI())
}

