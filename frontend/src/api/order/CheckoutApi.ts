import { APIMessage } from '@/api/shared/APIMessage'
import type { TaskIO } from '@/api/shared/TaskIO'
import { sendAPI } from '@/api/shared/sendAPI'
import type { CheckoutLine } from '@/objects/order/CheckoutLine'
import type { CheckoutResponse } from '@/objects/order/CheckoutResponse'
import type { VoucherId } from '@/objects/shared/ids'

export type CheckoutDeliverySnapshot = {
  customerName: string
  customerPhone: string
  deliveryAddress: string
}

class CheckoutAPI extends APIMessage<CheckoutResponse> {
  readonly apiName = 'checkoutapi'
  readonly lines: CheckoutLine[]
  readonly customerName?: string
  readonly customerPhone?: string
  readonly deliveryAddress?: string
  readonly voucherId?: VoucherId

  constructor(lines: CheckoutLine[], customerName?: string, customerPhone?: string, deliveryAddress?: string, voucherId?: VoucherId) {
    super()
    this.lines = lines
    this.customerName = customerName
    this.customerPhone = customerPhone
    this.deliveryAddress = deliveryAddress
    this.voucherId = voucherId
  }
}

export function checkoutIO(lines: CheckoutLine[], delivery?: CheckoutDeliverySnapshot, voucherId?: VoucherId): TaskIO<CheckoutResponse> {
  return sendAPI(new CheckoutAPI(lines, delivery?.customerName, delivery?.customerPhone, delivery?.deliveryAddress, voucherId))
}
