import { APIMessage } from '@/api/shared/APIMessage'
import type { TaskIO } from '@/api/shared/TaskIO'
import { sendAPI } from '@/api/shared/sendAPI'
import type { OkResponse } from '@/objects/shared/OkResponse'
import type { UpdateStoreImageRequest } from '@/objects/merchant/UpdateStoreImageRequest'

class MerchantStoreImageAPI extends APIMessage<OkResponse> {
  readonly merchantId: string
  readonly imageUrl: string

  constructor(merchantId: string, imageUrl: string) {
    super()
    this.merchantId = merchantId
    this.imageUrl = imageUrl
  }
}

export function updateMerchantStoreImageIO(
  merchantId: string,
  input: UpdateStoreImageRequest,
): TaskIO<OkResponse> {
  return sendAPI(new MerchantStoreImageAPI(merchantId, input.imageUrl))
}
