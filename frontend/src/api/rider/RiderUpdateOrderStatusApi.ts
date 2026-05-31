import { APIMessage } from '@/api/shared/APIMessage'
import type { TaskIO } from '@/api/shared/TaskIO'
import { sendAPI } from '@/api/shared/sendAPI'
import type { RiderDeliverySettlement } from '@/objects/rider/RiderDeliverySettlement'
import type { OrderId, OrderStatus } from '@/objects/shared/ids'

class RiderUpdateOrderStatusAPI extends APIMessage<RiderDeliverySettlement> {
  readonly apiName = 'riderupdateorderstatusapi'
  readonly orderId: OrderId
  readonly targetStatus: OrderStatus

  constructor(orderId: OrderId, targetStatus: OrderStatus) {
    super()
    this.orderId = orderId
    this.targetStatus = targetStatus
  }
}

export function updateRiderOrderStatusIO(orderId: OrderId, targetStatus: OrderStatus): TaskIO<RiderDeliverySettlement> {
  return sendAPI(new RiderUpdateOrderStatusAPI(orderId, targetStatus))
}
