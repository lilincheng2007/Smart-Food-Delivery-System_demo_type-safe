package delivery.order.objects

final case class CustomerOrdersResponse(pendingOrders: List[Order], historyOrders: List[Order])
