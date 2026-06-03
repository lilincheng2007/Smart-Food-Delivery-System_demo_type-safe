import { APIMessage } from '@/apis/shared/APIMessage'
import type { TaskIO } from '@/apis/shared/TaskIO'
import { sendAPI } from '@/apis/shared/sendAPI'
import type { OrderId } from '@/objects/shared/ids'
import type { OkResponse } from '@/objects/shared/apiTypes/OkResponse'

class MerchantOrderRejectAPI extends APIMessage<OkResponse> {
  readonly apiName = 'merchantorderrejectapi'
  readonly orderId: OrderId

  constructor(orderId: OrderId) {
    super()
    this.orderId = orderId
  }
}

export function rejectMerchantOrderIO(orderId: OrderId): TaskIO<OkResponse> {
  return sendAPI(new MerchantOrderRejectAPI(orderId))
}
