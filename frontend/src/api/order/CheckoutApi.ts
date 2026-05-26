import { APIMessage } from '@/api/shared/APIMessage'
import type { TaskIO } from '@/api/shared/TaskIO'
import { sendAPI } from '@/api/shared/sendAPI'
import type { CheckoutLine } from '@/objects/order/CheckoutLine'
import type { CheckoutResponse } from '@/objects/order/CheckoutResponse'

export type CheckoutDeliverySnapshot = {
  customerName: string
  customerPhone: string
  deliveryAddress: string
}

class CheckoutAPI extends APIMessage<CheckoutResponse> {
  readonly lines: CheckoutLine[]
  readonly customerName?: string
  readonly customerPhone?: string
  readonly deliveryAddress?: string

  constructor(lines: CheckoutLine[], delivery?: CheckoutDeliverySnapshot) {
    super()
    this.lines = lines
    this.customerName = delivery?.customerName
    this.customerPhone = delivery?.customerPhone
    this.deliveryAddress = delivery?.deliveryAddress
  }
}

export function checkoutIO(lines: CheckoutLine[], delivery?: CheckoutDeliverySnapshot): TaskIO<CheckoutResponse> {
  return sendAPI(new CheckoutAPI(lines, delivery))
}
