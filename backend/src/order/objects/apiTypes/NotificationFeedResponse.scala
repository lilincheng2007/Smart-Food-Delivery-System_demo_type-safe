package delivery.order.objects.apiTypes

final case class NotificationFeedItem(
    id: String,
    message: String,
    target: String,
    createdAt: String,
    isRead: Boolean
)

final case class NotificationFeedResponse(
    items: List[NotificationFeedItem],
    nextCursor: Option[String],
    unreadCount: Int
)
