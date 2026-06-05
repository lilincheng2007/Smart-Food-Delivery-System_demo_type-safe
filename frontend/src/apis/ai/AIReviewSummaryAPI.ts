import { APIMessage } from '@/apis/shared/APIMessage'
import type { TaskIO } from '@/apis/shared/TaskIO'
import { sendAPI } from '@/apis/shared/sendAPI'
import type { AIReviewSummaryResponse } from '@/objects/ai/apiTypes/AIReviewSummaryResponse'
import type { MerchantId } from '@/objects/shared/ids'

class AIReviewSummaryAPI extends APIMessage<AIReviewSummaryResponse> {
  readonly apiName = 'aireviewsummaryapi'
  readonly merchantId: MerchantId

  constructor(merchantId: MerchantId) {
    super()
    this.merchantId = merchantId
  }
}

export function aiReviewSummaryIO(merchantId: MerchantId): TaskIO<AIReviewSummaryResponse> {
  return sendAPI(new AIReviewSummaryAPI(merchantId))
}
