package delivery.order.objects.apiTypes

import delivery.domain.MerchantId

final case class OrderMerchantNote(
    merchantId: MerchantId,
    text: Option[String] = None,
    imageUrl: Option[String] = None
)
