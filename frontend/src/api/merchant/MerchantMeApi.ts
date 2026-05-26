import { APIMessage } from '@/api/shared/APIMessage'
import type { TaskIO } from '@/api/shared/TaskIO'
import { sendAPI } from '@/api/shared/sendAPI'
import type { MerchantMeResponse } from '@/objects/merchant/MerchantMeResponse'

class MerchantMeAPI extends APIMessage<MerchantMeResponse> {}

export function fetchMerchantMeIO(): TaskIO<MerchantMeResponse> {
  return sendAPI(new MerchantMeAPI())
}
