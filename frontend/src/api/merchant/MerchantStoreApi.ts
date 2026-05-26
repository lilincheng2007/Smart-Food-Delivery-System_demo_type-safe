import { APIMessage } from '@/api/shared/APIMessage'
import type { TaskIO } from '@/api/shared/TaskIO'
import { sendAPI } from '@/api/shared/sendAPI'
import type { CreateStoreRequest } from '@/objects/merchant/CreateStoreRequest'

class MerchantStoreAPI extends APIMessage<string> {
  readonly storeName: string
  readonly address: string

  constructor(storeName: string, address: string) {
    super()
    this.storeName = storeName
    this.address = address
  }
}

export function createMerchantStoreIO(input: CreateStoreRequest): TaskIO<string> {
  return sendAPI(new MerchantStoreAPI(input.storeName, input.address))
}
