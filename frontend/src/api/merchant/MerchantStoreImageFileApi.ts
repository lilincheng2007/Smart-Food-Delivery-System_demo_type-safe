import { apiPostFormDataIO } from '@/api/shared/client'
import type { TaskIO } from '@/api/shared/TaskIO'
import type { StoreImageUploadResponse } from '@/objects/merchant/StoreImageUploadResponse'

export function uploadMerchantStoreImageFileIO(merchantId: string, file: File): TaskIO<StoreImageUploadResponse> {
  const formData = new FormData()
  formData.append('file', file)
  return apiPostFormDataIO(`/merchant/me/stores/${merchantId}/image-file`, formData)
}
