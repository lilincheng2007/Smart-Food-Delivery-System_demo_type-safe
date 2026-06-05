import { APIMessage } from '@/apis/shared/APIMessage'
import type { TaskIO } from '@/apis/shared/TaskIO'
import { sendAPI } from '@/apis/shared/sendAPI'
import type { OkResponse } from '@/objects/shared/apiTypes/OkResponse'
import type { OrderId } from '@/objects/shared/ids'

class MerchantRefundRejectAPI extends APIMessage<OkResponse> {
  readonly apiName = 'merchantrefundrejectapi'
  readonly orderId: OrderId
  readonly reason: string

  constructor(orderId: OrderId, reason: string) {
    super()
    this.orderId = orderId
    this.reason = reason
  }
}

export function rejectMerchantRefundIO(orderId: OrderId, reason: string): TaskIO<OkResponse> {
  return sendAPI(new MerchantRefundRejectAPI(orderId, reason))
}
