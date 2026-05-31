import { APIMessage } from '@/api/shared/APIMessage'
import type { TaskIO } from '@/api/shared/TaskIO'
import { sendAPI } from '@/api/shared/sendAPI'
import type { RiderUseTimeoutCardResponse } from '@/objects/rider/RiderUseTimeoutCardResponse'
import type { OrderId } from '@/objects/shared/ids'

class RiderUseTimeoutCardAPI extends APIMessage<RiderUseTimeoutCardResponse> {
  readonly apiName = 'riderusetimeoutcardapi'
  readonly orderId: OrderId

  constructor(orderId: OrderId) {
    super()
    this.orderId = orderId
  }
}

export function useRiderTimeoutCardIO(orderId: OrderId): TaskIO<RiderUseTimeoutCardResponse> {
  return sendAPI(new RiderUseTimeoutCardAPI(orderId))
}
