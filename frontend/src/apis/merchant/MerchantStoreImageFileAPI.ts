import { APIMessage } from '@/apis/shared/APIMessage'
import type { TaskIO } from '@/apis/shared/TaskIO'
import { sendAPI } from '@/apis/shared/sendAPI'
import { fileToBase64, getLocalImageFileError } from '@/lib/local-image-file'
import type { MerchantId } from '@/objects/shared/ids'

class MerchantStoreImageFileAPI extends APIMessage<string> {
  readonly apiName = 'merchantstoreimagefileapi'
  readonly merchantId: MerchantId
  readonly bytesBase64: string
  readonly contentTypeLower: string
  readonly filenameHint: string | null

  constructor(merchantId: MerchantId, bytesBase64: string, contentTypeLower: string, filenameHint: string | null) {
    super()
    this.merchantId = merchantId
    this.bytesBase64 = bytesBase64
    this.contentTypeLower = contentTypeLower
    this.filenameHint = filenameHint
  }
}

export function uploadMerchantStoreImageFileIO(merchantId: MerchantId, file: File): TaskIO<string> {
  return async () => {
    const fileError = getLocalImageFileError(file)
    if (fileError) throw new Error(fileError)
    const bytesBase64 = await fileToBase64(file)
    return sendAPI(new MerchantStoreImageFileAPI(merchantId, bytesBase64, file.type.toLowerCase(), file.name || null))()
  }
}
