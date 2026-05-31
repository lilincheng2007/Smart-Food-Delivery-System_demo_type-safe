import { APIMessage } from '@/api/shared/APIMessage'
import type { TaskIO } from '@/api/shared/TaskIO'
import { sendAPI } from '@/api/shared/sendAPI'
import type { AIMerchantStoreDescriptionRequest } from '@/objects/ai/AIMerchantStoreDescriptionRequest'
import type { AIMerchantStoreDescriptionResponse } from '@/objects/ai/AIMerchantStoreDescriptionResponse'
import type { MerchantId } from '@/objects/shared/ids'

class AIMerchantStoreDescriptionAPI extends APIMessage<AIMerchantStoreDescriptionResponse> {
  readonly apiName = 'aimerchantstoredescriptionapi'
  readonly merchantId: MerchantId
  readonly keywords: string

  constructor(request: AIMerchantStoreDescriptionRequest) {
    super()
    this.merchantId = request.merchantId
    this.keywords = request.keywords
  }
}

export function aiMerchantStoreDescriptionIO(
  request: AIMerchantStoreDescriptionRequest,
): TaskIO<AIMerchantStoreDescriptionResponse> {
  return sendAPI(new AIMerchantStoreDescriptionAPI(request))
}
