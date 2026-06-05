import { APIMessage } from '@/apis/shared/APIMessage'
import type { TaskIO } from '@/apis/shared/TaskIO'
import { sendAPI } from '@/apis/shared/sendAPI'
import { fileToBase64, getLocalImageFileError } from '@/lib/local-image-file'
import type { Product } from '@/objects/merchant/Product'
import type { ProductId } from '@/objects/shared/ids'

class MerchantProductImageFileAPI extends APIMessage<Product> {
  readonly apiName = 'merchantproductimagefileapi'
  readonly productId: ProductId
  readonly bytesBase64: string
  readonly contentTypeLower: string
  readonly filenameHint: string | null

  constructor(productId: ProductId, bytesBase64: string, contentTypeLower: string, filenameHint: string | null) {
    super()
    this.productId = productId
    this.bytesBase64 = bytesBase64
    this.contentTypeLower = contentTypeLower
    this.filenameHint = filenameHint
  }
}

export function uploadMerchantProductImageFileIO(productId: ProductId, file: File): TaskIO<Product> {
  return async () => {
    const fileError = getLocalImageFileError(file)
    if (fileError) throw new Error(fileError)
    const bytesBase64 = await fileToBase64(file)
    return sendAPI(new MerchantProductImageFileAPI(productId, bytesBase64, file.type.toLowerCase(), file.name || null))()
  }
}
