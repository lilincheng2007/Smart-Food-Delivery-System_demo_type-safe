package delivery.order.tables

import delivery.order.objects.Order

final case class OrderServiceState(
    orders: List[Order]
)
