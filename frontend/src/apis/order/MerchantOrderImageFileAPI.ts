import { APIMessage } from '@/apis/shared/APIMessage'
import type { TaskIO } from '@/apis/shared/TaskIO'
import { sendAPI } from '@/apis/shared/sendAPI'
import { fileToBase64, getLocalImageFileError } from '@/lib/local-image-file'

class MerchantOrderImageFileAPI extends APIMessage<string> {
  readonly apiName = 'merchantorderimagefileapi'
  readonly bytesBase64: string
  readonly contentTypeLower: string
  readonly filenameHint: string | null

  constructor(bytesBase64: string, contentTypeLower: string, filenameHint: string | null) {
    super()
    this.bytesBase64 = bytesBase64
    this.contentTypeLower = contentTypeLower
    this.filenameHint = filenameHint
  }
}

export function uploadMerchantOrderImageFileIO(file: File): TaskIO<string> {
  return async () => {
    const fileError = getLocalImageFileError(file)
    if (fileError) throw new Error(fileError)
    const bytesBase64 = await fileToBase64(file)
    return sendAPI(new MerchantOrderImageFileAPI(bytesBase64, file.type.toLowerCase(), file.name || null))()
  }
}
