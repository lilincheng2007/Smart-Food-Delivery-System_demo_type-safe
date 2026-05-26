import { APIMessage } from '@/api/shared/APIMessage'
import type { TaskIO } from '@/api/shared/TaskIO'
import { sendAPI } from '@/api/shared/sendAPI'
import { runTask } from '@/api/shared/TaskIO'

class MerchantStoreImageFileAPI extends APIMessage<string> {
  readonly merchantId: string
  readonly bytesBase64: string
  readonly contentTypeLower: string
  readonly filenameHint?: string

  constructor(merchantId: string, bytesBase64: string, contentTypeLower: string, filenameHint?: string) {
    super()
    this.merchantId = merchantId
    this.bytesBase64 = bytesBase64
    this.contentTypeLower = contentTypeLower
    this.filenameHint = filenameHint
  }
}

export function uploadMerchantStoreImageFileIO(merchantId: string, file: File): TaskIO<string> {
  return async () => {
    const bytesBase64 = await fileToBase64(file)
    return runTask(
      sendAPI(
        new MerchantStoreImageFileAPI(
          merchantId,
          bytesBase64,
          file.type.toLowerCase(),
          file.name || undefined,
        ),
      ),
    )
  }
}

async function fileToBase64(file: File): Promise<string> {
  const bytes = new Uint8Array(await file.arrayBuffer())
  let binary = ''
  const chunkSize = 0x8000
  for (let index = 0; index < bytes.length; index += chunkSize) {
    binary += String.fromCharCode(...bytes.subarray(index, index + chunkSize))
  }
  return window.btoa(binary)
}
