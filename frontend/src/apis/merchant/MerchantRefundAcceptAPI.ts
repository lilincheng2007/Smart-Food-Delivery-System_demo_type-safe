import { APIMessage } from '@/apis/shared/APIMessage'
import type { TaskIO } from '@/apis/shared/TaskIO'
import { sendAPI } from '@/apis/shared/sendAPI'
import type { OkResponse } from '@/objects/shared/apiTypes/OkResponse'
import type { OrderId } from '@/objects/shared/ids'

class MerchantRefundAcceptAPI extends APIMessage<OkResponse> {
  readonly apiName = 'merchantrefundacceptapi'
  readonly orderId: OrderId
  readonly reason: string | null

  constructor(orderId: OrderId, reason: string | null) {
    super()
    this.orderId = orderId
    this.reason = reason
  }
}

export function acceptMerchantRefundIO(orderId: OrderId, reason: string | null): TaskIO<OkResponse> {
  return sendAPI(new MerchantRefundAcceptAPI(orderId, reason))
}
