import { APIMessage } from '@/apis/shared/APIMessage'
import type { TaskIO } from '@/apis/shared/TaskIO'
import { sendAPI } from '@/apis/shared/sendAPI'
import { fileToBase64, getLocalImageFileError } from '@/lib/local-image-file'

class CustomerRefundImageFileAPI extends APIMessage<string> {
  readonly apiName = 'customerrefundimagefileapi'
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

export function uploadRefundImageFileIO(file: File): TaskIO<string> {
  return async () => {
    const fileError = getLocalImageFileError(file)
    if (fileError) throw new Error(fileError)
    const contentTypeLower = file.type.toLowerCase()
    const bytesBase64 = await fileToBase64(file)
    return sendAPI(new CustomerRefundImageFileAPI(bytesBase64, contentTypeLower, file.name || null))()
  }
}
