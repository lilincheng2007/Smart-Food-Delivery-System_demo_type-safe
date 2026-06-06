import { APIMessage } from '@/apis/shared/APIMessage'
import type { TaskIO } from '@/apis/shared/TaskIO'
import { sendAPI } from '@/apis/shared/sendAPI'
import type { AIMerchantBusinessSuggestionsResponse } from '@/objects/ai/apiTypes/AIMerchantBusinessSuggestionsResponse'
import type { MerchantId } from '@/objects/shared/ids'

class AIMerchantBusinessSuggestionsAPI extends APIMessage<AIMerchantBusinessSuggestionsResponse> {
  readonly apiName = 'aimerchantbusinesssuggestionsapi'
  readonly merchantId: MerchantId

  constructor(merchantId: MerchantId) {
    super()
    this.merchantId = merchantId
  }
}

export function aiMerchantBusinessSuggestionsIO(merchantId: MerchantId): TaskIO<AIMerchantBusinessSuggestionsResponse> {
  return sendAPI(new AIMerchantBusinessSuggestionsAPI(merchantId))
}

