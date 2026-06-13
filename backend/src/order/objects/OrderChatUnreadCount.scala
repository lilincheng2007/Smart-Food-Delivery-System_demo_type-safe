package delivery.order.objects

import delivery.domain.OrderId

final case class OrderChatUnreadCount(
    orderId: OrderId,
    peerRole: String,
    unreadCount: Int,
    latestMessageType: Option[String] = None,
    latestContent: Option[String] = None
)
