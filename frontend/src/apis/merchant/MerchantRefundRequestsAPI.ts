import { APIMessage } from '@/apis/shared/APIMessage'
import type { TaskIO } from '@/apis/shared/TaskIO'
import { sendAPI } from '@/apis/shared/sendAPI'
import type { MerchantRefundRequestsResponse } from '@/objects/merchant/apiTypes/MerchantRefundRequestsResponse'

class MerchantRefundRequestsAPI extends APIMessage<MerchantRefundRequestsResponse> {
  readonly apiName = 'merchantrefundrequestsapi'
}

export function fetchMerchantRefundRequestsIO(): TaskIO<MerchantRefundRequestsResponse> {
  return sendAPI(new MerchantRefundRequestsAPI())
}
