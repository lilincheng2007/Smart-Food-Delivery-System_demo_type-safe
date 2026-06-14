import { useCallback, useEffect, useMemo, useState } from 'react'
import { Bell } from 'lucide-react'

import { markAllNotificationsReadIO } from '@/apis/order/NotificationMarkAllReadAPI'
import { markNotificationReadIO } from '@/apis/order/NotificationMarkReadAPI'
import { fetchNotificationFeedIO } from '@/apis/order/NotificationFeedAPI'
import { useAuthSession } from '@/hooks/useAuthSession'
import { cn } from '@/lib/utils'
import type { NotificationFeedItem } from '@/objects/order/apiTypes/NotificationFeedResponse'
import { UserRoles } from '@/objects/shared/ids'

const pollIntervalMs = 8000

function formatNotificationTime(createdAt: string) {
  const date = new Date(createdAt)
  if (Number.isNaN(date.getTime())) return createdAt
  return `${date.getMonth() + 1}/${date.getDate()} ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
}

export function GlobalNotificationCenter() {
  const session = useAuthSession()
  const [notifications, setNotifications] = useState<NotificationFeedItem[]>([])
  const [unreadCount, setUnreadCount] = useState(0)
  const [isCustomerPanelOpen, setIsCustomerPanelOpen] = useState(false)

  const activeSession = useMemo(() => (session && session.role ? session : null), [session])
  const isCustomerSession = activeSession?.role === UserRoles.customer

  const poll = useCallback(async () => {
    if (!activeSession) return
    try {
      const response = await fetchNotificationFeedIO(activeSession.role, undefined, 80)()
      setNotifications(response.items.filter((item) => !item.isRead))
      setUnreadCount(response.unreadCount)
    } catch {
      // 全局通知不能打断当前页面业务流程。
    }
  }, [activeSession])

  useEffect(() => {
    if (!activeSession) {
      setNotifications([])
      setUnreadCount(0)
      setIsCustomerPanelOpen(false)
      return
    }

    void poll()
    const timer = window.setInterval(() => {
      void poll()
    }, pollIntervalMs)

    return () => {
      window.clearInterval(timer)
    }
  }, [activeSession, poll])

  const handleClick = (notification: NotificationFeedItem) => {
    setNotifications((current) => current.filter((item) => item.id !== notification.id))
    setUnreadCount((current) => Math.max(0, current - 1))
    void markNotificationReadIO(notification.id)().catch(() => {})
    window.location.assign(notification.target)
  }

  const markAllRead = () => {
    if (notifications.length === 0) return
    const ids = notifications.map((item) => item.id)
    setNotifications([])
    setUnreadCount(0)
    void markAllNotificationsReadIO(ids)().catch(() => {})
  }

  if (!activeSession) return null

  if (isCustomerSession) {
    const recentNotifications = notifications.slice(0, 8)
    return (
      <div className="fixed right-4 top-4 z-[60] sm:right-6 sm:top-6">
        <div className="relative">
          <button
            type="button"
            className="relative flex size-11 items-center justify-center rounded-full border border-orange-200 bg-white/95 text-orange-600 shadow-[0_14px_34px_rgba(15,23,42,0.14)] backdrop-blur transition hover:border-orange-300 hover:bg-orange-50"
            aria-label="查看通知"
            onClick={() => setIsCustomerPanelOpen((open) => !open)}
          >
            <Bell className="size-5" />
            {unreadCount > 0 ? <span className="absolute right-2 top-2 size-2.5 rounded-full bg-rose-500 ring-2 ring-white" /> : null}
          </button>

          {isCustomerPanelOpen ? (
            <div className="absolute right-0 mt-3 w-[min(22rem,calc(100vw-2rem))] overflow-hidden rounded-2xl border border-orange-100 bg-white/95 shadow-[0_22px_70px_rgba(15,23,42,0.18)] backdrop-blur-xl">
              <div className="flex items-center justify-between border-b border-orange-100 px-4 py-3">
                <div>
                  <p className="font-semibold text-slate-950">最近通知</p>
                  <p className="text-xs text-slate-500">{unreadCount > 0 ? `${unreadCount} 条未读` : '暂无未读通知'}</p>
                </div>
                {notifications.length > 0 ? (
                  <button type="button" className="text-xs font-medium text-orange-600 hover:text-orange-700" onClick={markAllRead}>
                    全部已读
                  </button>
                ) : null}
              </div>
              <div className="max-h-[min(28rem,70vh)] overflow-y-auto p-2">
                {recentNotifications.length === 0 ? (
                  <p className="px-3 py-6 text-center text-sm text-slate-500">暂无新的订单或消息通知。</p>
                ) : null}
                {recentNotifications.map((notification) => (
                  <button
                    key={notification.id}
                    type="button"
                    className="w-full rounded-xl px-3 py-3 text-left transition hover:bg-orange-50"
                    onClick={() => handleClick(notification)}
                  >
                    <p className="line-clamp-3 text-sm leading-6 text-slate-800">{notification.message}</p>
                    <p className="mt-1 text-xs text-slate-400">{formatNotificationTime(notification.createdAt)}</p>
                  </button>
                ))}
              </div>
            </div>
          ) : null}
        </div>
      </div>
    )
  }

  if (notifications.length === 0) return null

  return (
    <div className="fixed inset-x-0 top-0 z-[40] flex justify-center px-3 pt-3">
      <div className="flex max-h-[34vh] w-full max-w-3xl flex-col gap-2 overflow-y-auto">
        {notifications.map((notification) => (
          <button
            key={notification.id}
            type="button"
            className={cn(
              'w-full rounded-xl border border-orange-200 bg-white/95 px-4 py-3 text-left text-sm text-slate-800 shadow-[0_12px_32px_rgba(15,23,42,0.12)] backdrop-blur transition hover:border-orange-300 hover:bg-orange-50',
            )}
            onClick={() => handleClick(notification)}
          >
            {notification.message}
          </button>
        ))}
      </div>
    </div>
  )
}
