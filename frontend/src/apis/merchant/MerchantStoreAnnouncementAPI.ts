import { APIMessage } from '@/apis/shared/APIMessage'
import type { TaskIO } from '@/apis/shared/TaskIO'
import { sendAPI } from '@/apis/shared/sendAPI'
import type { OkResponse } from '@/objects/shared/apiTypes/OkResponse'
import type { MerchantId } from '@/objects/shared/ids'

class MerchantStoreAnnouncementAPI extends APIMessage<OkResponse> {
  readonly apiName = 'merchantstoreannouncementapi'
  readonly merchantId: MerchantId
  readonly announcement: string

  constructor(merchantId: MerchantId, announcement: string) {
    super()
    this.merchantId = merchantId
    this.announcement = announcement
  }
}

export function updateMerchantStoreAnnouncementIO(merchantId: MerchantId, announcement: string): TaskIO<OkResponse> {
  return sendAPI(new MerchantStoreAnnouncementAPI(merchantId, announcement))
}
