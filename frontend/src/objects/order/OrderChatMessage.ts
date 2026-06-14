import type { OrderId } from '@/objects/shared/ids'
import type { OrderChatMessageType } from './OrderChatMessageType'
import type { OrderChatRole } from './OrderChatRole'

export type { OrderChatMessageType } from './OrderChatMessageType'
export type { OrderChatRole } from './OrderChatRole'

export interface OrderChatMessage {
  id: string
  orderId: OrderId
  senderRole: OrderChatRole
  peerRole: OrderChatRole
  messageType: OrderChatMessageType
  content: string
  createdAt: string
}
