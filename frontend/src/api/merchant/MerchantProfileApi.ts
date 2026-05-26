import { APIMessage } from '@/api/shared/APIMessage'
import type { TaskIO } from '@/api/shared/TaskIO'
import { sendAPI } from '@/api/shared/sendAPI'
import type { MerchantProfile } from '@/objects/merchant/MerchantProfile'
import type { OkResponse } from '@/objects/shared/OkResponse'

class MerchantProfileAPI extends APIMessage<OkResponse> {
  readonly profile: MerchantProfile

  constructor(profile: MerchantProfile) {
    super()
    this.profile = profile
  }
}

export function putMerchantProfileIO(profile: MerchantProfile): TaskIO<OkResponse> {
  return sendAPI(new MerchantProfileAPI(profile))
}
