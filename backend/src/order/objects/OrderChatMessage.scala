package delivery.order.objects

import delivery.domain.OrderId

final case class OrderChatMessage(
    id: String,
    orderId: OrderId,
    senderRole: String,
    peerRole: String,
    messageType: String,
    content: String,
    createdAt: String,
    readAt: Option[String] = None
)
