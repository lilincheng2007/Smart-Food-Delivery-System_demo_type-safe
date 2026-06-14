export interface NotificationFeedItem {
  id: string
  message: string
  target: string
  createdAt: string
  isRead: boolean
}

export interface NotificationFeedResponse {
  items: NotificationFeedItem[]
  nextCursor?: string | null
  unreadCount: number
}
