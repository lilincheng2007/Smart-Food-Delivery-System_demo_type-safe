import type { OrderChatRole } from '@/objects/order/OrderChatMessage'
import type { OrderId } from '@/objects/shared/ids'

export interface OrderChatUnreadCount {
  orderId: OrderId
  peerRole: OrderChatRole
  unreadCount: number
  latestMessageType?: 'text' | 'image' | string | null
  latestContent?: string | null
  latestCreatedAt?: string | null
}
