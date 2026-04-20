import { apiPutIO } from '@/api/shared/client'
import type { OkResponse } from '@/objects/shared/OkResponse'
import type { UpdateStoreImageRequest } from '@/objects/merchant/UpdateStoreImageRequest'
import type { TaskIO } from '@/api/shared/TaskIO'

export function updateMerchantStoreImageIO(
  merchantId: string,
  input: UpdateStoreImageRequest,
): TaskIO<OkResponse> {
  return apiPutIO(`/merchant/me/stores/${merchantId}/image`, input)
}
