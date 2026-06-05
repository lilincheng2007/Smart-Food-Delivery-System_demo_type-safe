import { APIMessage } from '@/apis/shared/APIMessage'
import type { TaskIO } from '@/apis/shared/TaskIO'
import { sendAPI } from '@/apis/shared/sendAPI'
import type { OkResponse } from '@/objects/shared/apiTypes/OkResponse'
import type { MerchantId } from '@/objects/shared/ids'

class MerchantReviewReplyAPI extends APIMessage<OkResponse> {
  readonly apiName = 'merchantreviewreplyapi'
  readonly reviewId: string
  readonly merchantId: MerchantId
  readonly replyContent: string

  constructor(reviewId: string, merchantId: MerchantId, replyContent: string) {
    super()
    this.reviewId = reviewId
    this.merchantId = merchantId
    this.replyContent = replyContent
  }
}

export function replyMerchantReviewIO(reviewId: string, merchantId: MerchantId, replyContent: string): TaskIO<OkResponse> {
  return sendAPI(new MerchantReviewReplyAPI(reviewId, merchantId, replyContent))
}
