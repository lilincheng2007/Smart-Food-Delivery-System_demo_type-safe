import { APIMessage } from '@/api/shared/APIMessage'
import type { TaskIO } from '@/api/shared/TaskIO'
import { sendAPI } from '@/api/shared/sendAPI'
import type { OrderCancelResponse } from '@/objects/order/OrderCancelResponse'
import type { OrderId } from '@/objects/shared/ids'

class OrderCancelAPI extends APIMessage<OrderCancelResponse> {
  readonly orderId: OrderId

  constructor(orderId: OrderId) {
    super()
    this.orderId = orderId
  }
}

export function cancelOrderIO(orderId: OrderId): TaskIO<OrderCancelResponse> {
  return sendAPI(new OrderCancelAPI(orderId))
}
