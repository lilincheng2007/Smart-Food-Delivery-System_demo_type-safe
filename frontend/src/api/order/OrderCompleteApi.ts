import { APIMessage } from '@/api/shared/APIMessage'
import type { TaskIO } from '@/api/shared/TaskIO'
import { sendAPI } from '@/api/shared/sendAPI'
import type { Order } from '@/objects/order/Order'
import type { OrderId } from '@/objects/shared/ids'

class OrderCompleteAPI extends APIMessage<Order> {
  readonly apiName = 'ordercompleteapi'
  readonly orderId: OrderId

  constructor(orderId: OrderId) {
    super()
    this.orderId = orderId
  }
}

export function completeOrderIO(orderId: OrderId): TaskIO<Order> {
  return sendAPI(new OrderCompleteAPI(orderId))
}
