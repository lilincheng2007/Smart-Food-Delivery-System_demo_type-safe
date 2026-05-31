import { APIMessage } from '@/api/shared/APIMessage'
import type { TaskIO } from '@/api/shared/TaskIO'
import { sendAPI } from '@/api/shared/sendAPI'
import type { MerchantId } from '@/objects/shared/ids'
import type { OkResponse } from '@/objects/shared/OkResponse'

class MerchantStoreDescriptionAPI extends APIMessage<OkResponse> {
  readonly apiName = 'merchantstoredescriptionapi'
  readonly merchantId: MerchantId
  readonly description: string

  constructor(merchantId: MerchantId, description: string) {
    super()
    this.merchantId = merchantId
    this.description = description
  }
}

export function updateMerchantStoreDescriptionIO(merchantId: MerchantId, description: string): TaskIO<OkResponse> {
  return sendAPI(new MerchantStoreDescriptionAPI(merchantId, description))
}
