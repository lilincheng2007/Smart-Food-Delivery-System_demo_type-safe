import { APIMessage } from '@/apis/shared/APIMessage'
import type { TaskIO } from '@/apis/shared/TaskIO'
import { sendAPI } from '@/apis/shared/sendAPI'
import type { NotificationFeedResponse } from '@/objects/order/apiTypes/NotificationFeedResponse'

class NotificationFeedAPI extends APIMessage<NotificationFeedResponse> {
  readonly apiName = 'notificationfeedapi'
  readonly role: 'customer' | 'merchant' | 'rider' | 'admin'
  readonly cursor?: string
  readonly limit?: number

  constructor(role: 'customer' | 'merchant' | 'rider' | 'admin', cursor?: string, limit?: number) {
    super()
    this.role = role
    this.cursor = cursor
    this.limit = limit
  }
}

export function fetchNotificationFeedIO(
  role: 'customer' | 'merchant' | 'rider' | 'admin',
  cursor?: string,
  limit?: number,
): TaskIO<NotificationFeedResponse> {
  return sendAPI(new NotificationFeedAPI(role, cursor, limit))
}
