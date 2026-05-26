import { APIMessage } from '@/api/shared/APIMessage'
import type { TaskIO } from '@/api/shared/TaskIO'
import { sendAPI } from '@/api/shared/sendAPI'
import type { RiderMeResponse } from '@/objects/rider/RiderMeResponse'

class RiderMeAPI extends APIMessage<RiderMeResponse> {}

export function fetchRiderMeIO(): TaskIO<RiderMeResponse> {
  return sendAPI(new RiderMeAPI())
}
