import type { TaskIO } from '@/api/shared/TaskIO'
import type { OkResponse } from '@/objects/shared/OkResponse'
import type { RiderUpdateOrderStatusResponse } from '@/objects/rider/RiderUpdateOrderStatusResponse'
import { apiPostIO } from '@/api/shared/client'

export function grabRiderOrderIO(orderId: string): TaskIO<OkResponse> {
  return apiPostIO(`/rider/me/orders/${orderId}/grab`)
}

export function updateRiderOrderStatusIO(orderId: string): TaskIO<RiderUpdateOrderStatusResponse> {
  return apiPostIO(`/rider/me/orders/${orderId}/status`)
}
