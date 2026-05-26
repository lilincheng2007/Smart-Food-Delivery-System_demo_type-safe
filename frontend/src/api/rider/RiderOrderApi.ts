import { APIMessage } from '@/api/shared/APIMessage'
import type { TaskIO } from '@/api/shared/TaskIO'
import { sendAPI } from '@/api/shared/sendAPI'
import type { OkResponse } from '@/objects/shared/OkResponse'

class RiderGrabOrderAPI extends APIMessage<OkResponse> {
  readonly orderId: string

  constructor(orderId: string) {
    super()
    this.orderId = orderId
  }
}

class RiderUpdateOrderStatusAPI extends APIMessage<OkResponse> {
  readonly orderId: string

  constructor(orderId: string) {
    super()
    this.orderId = orderId
  }
}

export function grabRiderOrderIO(orderId: string): TaskIO<OkResponse> {
  return sendAPI(new RiderGrabOrderAPI(orderId))
}

export function updateRiderOrderStatusIO(orderId: string): TaskIO<OkResponse> {
  return sendAPI(new RiderUpdateOrderStatusAPI(orderId))
}
