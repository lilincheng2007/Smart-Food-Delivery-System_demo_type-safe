package delivery.merchant.objects.apiTypes

import delivery.order.objects.Order

final case class MerchantRefundRequestsResponse(requests: List[Order])
