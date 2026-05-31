import { APIMessage } from '@/api/shared/APIMessage'
import type { TaskIO } from '@/api/shared/TaskIO'
import { sendAPI } from '@/api/shared/sendAPI'
import type { RiderTimeoutCardRedeemResponse } from '@/objects/rider/RiderTimeoutCardRedeemResponse'

class RiderRedeemTimeoutCardAPI extends APIMessage<RiderTimeoutCardRedeemResponse> {
  readonly apiName = 'riderredeemtimeoutcardapi'
}

export function redeemRiderTimeoutCardIO(): TaskIO<RiderTimeoutCardRedeemResponse> {
  return sendAPI(new RiderRedeemTimeoutCardAPI())
}
