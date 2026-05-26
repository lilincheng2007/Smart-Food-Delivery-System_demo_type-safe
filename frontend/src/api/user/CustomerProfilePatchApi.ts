import { APIMessage } from '@/api/shared/APIMessage'
import type { TaskIO } from '@/api/shared/TaskIO'
import { sendAPI } from '@/api/shared/sendAPI'
import type { CustomerProfilePatch } from '@/objects/user/CustomerProfilePatch'
import type { OkResponse } from '@/objects/shared/OkResponse'

class CustomerProfilePatchAPI extends APIMessage<OkResponse> {
  readonly patch: CustomerProfilePatch

  constructor(patch: CustomerProfilePatch) {
    super()
    this.patch = patch
  }
}

export function patchCustomerProfileIO(patch: CustomerProfilePatch): TaskIO<OkResponse> {
  return sendAPI(new CustomerProfilePatchAPI(patch))
}
