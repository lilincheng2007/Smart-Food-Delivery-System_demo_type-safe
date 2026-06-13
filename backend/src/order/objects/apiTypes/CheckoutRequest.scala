package delivery.order.objects.apiTypes

import delivery.order.objects.CheckoutLine
import delivery.domain.VoucherId

final case class CheckoutRequest(
    lines: List[CheckoutLine],
    customerName: Option[String] = None,
    customerPhone: Option[String] = None,
    deliveryAddress: Option[String] = None,
    voucherId: Option[VoucherId] = None,
    merchantNotes: List[OrderMerchantNote] = Nil
)
