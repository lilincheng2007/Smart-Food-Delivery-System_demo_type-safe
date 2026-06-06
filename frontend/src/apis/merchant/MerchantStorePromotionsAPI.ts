import { APIMessage } from '@/apis/shared/APIMessage'
import type { TaskIO } from '@/apis/shared/TaskIO'
import { sendAPI } from '@/apis/shared/sendAPI'
import type { OkResponse } from '@/objects/shared/apiTypes/OkResponse'
import type { MerchantId } from '@/objects/shared/ids'
import type { Promotion } from '@/objects/shared/Promotion'

class MerchantStorePromotionsAPI extends APIMessage<OkResponse> {
  readonly apiName = 'merchantstorepromotionsapi'
  readonly merchantId: MerchantId
  readonly promotions: Promotion[]

  constructor(merchantId: MerchantId, promotions: Promotion[]) {
    super()
    this.merchantId = merchantId
    this.promotions = promotions
  }
}

export function updateMerchantStorePromotionsIO(merchantId: MerchantId, promotions: Promotion[]): TaskIO<OkResponse> {
  return sendAPI(new MerchantStorePromotionsAPI(merchantId, promotions))
}

