package delivery.order.objects

final case class OrderCancelResponse(order: Order, walletBalance: Double)
