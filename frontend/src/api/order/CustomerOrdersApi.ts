import { APIMessage } from '@/api/shared/APIMessage'
import type { TaskIO } from '@/api/shared/TaskIO'
import { sendAPI } from '@/api/shared/sendAPI'
import type { CustomerOrdersResponse } from '@/objects/order/CustomerOrdersResponse'

class CustomerOrdersAPI extends APIMessage<CustomerOrdersResponse> {}

export function fetchCustomerOrdersIO(): TaskIO<CustomerOrdersResponse> {
  return sendAPI(new CustomerOrdersAPI())
}
