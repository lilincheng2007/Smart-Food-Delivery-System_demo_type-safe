package delivery.order.objects

import delivery.shared.objects.VoucherId

final case class CheckoutRequest(
    lines: List[CheckoutLine],
    customerName: Option[String] = None,
    customerPhone: Option[String] = None,
    deliveryAddress: Option[String] = None,
    voucherId: Option[VoucherId] = None
)
