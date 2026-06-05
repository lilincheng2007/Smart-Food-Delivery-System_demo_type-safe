import { APIMessage } from '@/apis/shared/APIMessage'
import type { TaskIO } from '@/apis/shared/TaskIO'
import { sendAPI } from '@/apis/shared/sendAPI'
import type { OrderRefundRequestResponse } from '@/objects/order/apiTypes/OrderRefundRequestResponse'
import type { OrderId } from '@/objects/shared/ids'

class OrderRefundAppealAPI extends APIMessage<OrderRefundRequestResponse> {
  readonly apiName = 'orderrefundappealapi'
  readonly orderId: OrderId

  constructor(orderId: OrderId) {
    super()
    this.orderId = orderId
  }
}

export function appealOrderRefundIO(orderId: OrderId): TaskIO<OrderRefundRequestResponse> {
  return sendAPI(new OrderRefundAppealAPI(orderId))
}
