import { APIMessage } from '@/apis/shared/APIMessage'
import type { TaskIO } from '@/apis/shared/TaskIO'
import { sendAPI } from '@/apis/shared/sendAPI'
import type { OkResponse } from '@/objects/shared/apiTypes/OkResponse'
import type { Promotion } from '@/objects/shared/Promotion'

class AdminPlatformPromotionsUpdateAPI extends APIMessage<OkResponse> {
  readonly apiName = 'adminplatformpromotionsupdateapi'
  readonly promotions: Promotion[]

  constructor(promotions: Promotion[]) {
    super()
    this.promotions = promotions
  }
}

export function updateAdminPlatformPromotionsIO(promotions: Promotion[]): TaskIO<OkResponse> {
  return sendAPI(new AdminPlatformPromotionsUpdateAPI(promotions))
}

