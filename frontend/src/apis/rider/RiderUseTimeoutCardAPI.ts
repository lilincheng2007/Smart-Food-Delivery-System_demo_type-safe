import { APIMessage } from '@/apis/shared/APIMessage'
import type { TaskIO } from '@/apis/shared/TaskIO'
import { sendAPI } from '@/apis/shared/sendAPI'
import type { RiderUseTimeoutCardResponse } from '@/objects/rider/apiTypes/RiderUseTimeoutCardResponse'
import type { OrderId } from '@/objects/shared/ids'

class RiderUseTimeoutCardAPI extends APIMessage<RiderUseTimeoutCardResponse> {
  readonly apiName = 'riderusetimeoutcardapi'
  readonly orderId: OrderId

  constructor(orderId: OrderId) {
    super()
    this.orderId = orderId
  }
}

export function riderUseTimeoutCardIO(orderId: OrderId): TaskIO<RiderUseTimeoutCardResponse> {
  return sendAPI(new RiderUseTimeoutCardAPI(orderId))
}
