import { APIMessage } from '@/apis/shared/APIMessage'
import type { TaskIO } from '@/apis/shared/TaskIO'
import { sendAPI } from '@/apis/shared/sendAPI'
import type { CheckoutDeliverySnapshot } from '@/apis/order/CheckoutAPI'
import type { CheckoutLine } from '@/objects/order/CheckoutLine'
import type { OrderMerchantNote } from '@/objects/order/apiTypes/CheckoutRequest'
import type { CheckoutQuoteResponse } from '@/objects/order/apiTypes/CheckoutQuoteResponse'
import type { VoucherId } from '@/objects/shared/ids'

class CheckoutQuoteAPI extends APIMessage<CheckoutQuoteResponse> {
  readonly apiName = 'checkoutquoteapi'
  readonly lines: CheckoutLine[]
  readonly customerName?: string
  readonly customerPhone?: string
  readonly deliveryAddress?: string
  readonly voucherId?: VoucherId
  readonly merchantNotes: OrderMerchantNote[]

  constructor(lines: CheckoutLine[], delivery?: CheckoutDeliverySnapshot, voucherId?: VoucherId, merchantNotes: OrderMerchantNote[] = []) {
    super()
    this.lines = lines
    this.customerName = delivery?.customerName
    this.customerPhone = delivery?.customerPhone
    this.deliveryAddress = delivery?.deliveryAddress
    this.voucherId = voucherId
    this.merchantNotes = merchantNotes
  }
}

export function checkoutQuoteIO(
  lines: CheckoutLine[],
  delivery?: CheckoutDeliverySnapshot,
  voucherId?: VoucherId,
  merchantNotes: OrderMerchantNote[] = [],
): TaskIO<CheckoutQuoteResponse> {
  return sendAPI(new CheckoutQuoteAPI(lines, delivery, voucherId, merchantNotes))
}
