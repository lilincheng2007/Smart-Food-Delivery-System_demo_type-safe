import { APIMessage } from '@/api/shared/APIMessage'
import type { TaskIO } from '@/api/shared/TaskIO'
import { sendAPI } from '@/api/shared/sendAPI'
import type { OkResponse } from '@/objects/shared/OkResponse'

class MerchantOrderReadyAPI extends APIMessage<OkResponse> {
  readonly orderId: string

  constructor(orderId: string) {
    super()
    this.orderId = orderId
  }
}

export function finishMerchantOrderCookingIO(orderId: string): TaskIO<OkResponse> {
  return sendAPI(new MerchantOrderReadyAPI(orderId))
}
