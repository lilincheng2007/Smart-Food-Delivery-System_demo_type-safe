package delivery.order.objects

import delivery.domain.OrderId

final case class OrderChatMessage(
    id: String,
    orderId: OrderId,
    senderRole: OrderChatRole,
    peerRole: OrderChatRole,
    messageType: OrderChatMessageType,
    content: String,
    createdAt: String,
    readAt: Option[String] = None
)
