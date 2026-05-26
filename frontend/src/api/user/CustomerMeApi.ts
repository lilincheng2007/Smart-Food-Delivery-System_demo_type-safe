import { APIMessage } from '@/api/shared/APIMessage'
import type { TaskIO } from '@/api/shared/TaskIO'
import { sendAPI } from '@/api/shared/sendAPI'
import type { CustomerMeResponse } from '@/objects/user/CustomerMeResponse'

class CustomerMeAPI extends APIMessage<CustomerMeResponse> {}

export function fetchCustomerMeIO(): TaskIO<CustomerMeResponse> {
  return sendAPI(new CustomerMeAPI())
}
