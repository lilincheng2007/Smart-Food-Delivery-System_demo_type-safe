import { APIMessage } from '@/api/shared/APIMessage'
import type { TaskIO } from '@/api/shared/TaskIO'
import { sendAPI } from '@/api/shared/sendAPI'
import type { ProductDescriptionPatch } from '@/objects/merchant/ProductDescriptionPatch'
import type { MerchantId } from '@/objects/shared/ids'
import type { OkResponse } from '@/objects/shared/OkResponse'

class MerchantProductDescriptionsAPI extends APIMessage<OkResponse> {
  readonly apiName = 'merchantproductdescriptionsapi'
  readonly merchantId: MerchantId
  readonly descriptions: ProductDescriptionPatch[]

  constructor(merchantId: MerchantId, descriptions: ProductDescriptionPatch[]) {
    super()
    this.merchantId = merchantId
    this.descriptions = descriptions
  }
}

export function updateMerchantProductDescriptionsIO(
  merchantId: MerchantId,
  descriptions: ProductDescriptionPatch[],
): TaskIO<OkResponse> {
  return sendAPI(new MerchantProductDescriptionsAPI(merchantId, descriptions))
}
