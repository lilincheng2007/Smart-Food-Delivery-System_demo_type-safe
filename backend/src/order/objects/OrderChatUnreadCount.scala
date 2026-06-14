package delivery.order.objects

import delivery.domain.OrderId

final case class OrderChatUnreadCount(
    orderId: OrderId,
    peerRole: OrderChatRole,
    unreadCount: Int,
    latestMessageType: Option[OrderChatMessageType] = None,
    latestContent: Option[String] = None,
    latestCreatedAt: Option[String] = None
)
