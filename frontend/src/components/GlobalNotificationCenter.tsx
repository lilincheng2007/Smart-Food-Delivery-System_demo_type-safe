import { useCallback, useEffect, useMemo, useState } from 'react'

import { fetchAdminRefundRequestsIO } from '@/apis/admin/AdminRefundRequestsAPI'
import { fetchAdminStoreOnboardingRequestsIO } from '@/apis/admin/AdminStoreOnboardingRequestsAPI'
import { fetchCatalogIO } from '@/apis/merchant/CatalogAPI'
import { fetchMerchantMeIO } from '@/apis/merchant/MerchantMeAPI'
import { fetchMerchantRefundRequestsIO } from '@/apis/merchant/MerchantRefundRequestsAPI'
import { fetchCustomerOrdersIO } from '@/apis/order/CustomerOrdersAPI'
import { fetchOrderChatUnreadCountsIO } from '@/apis/order/OrderChatAPI'
import { fetchMerchantReviewsIO } from '@/apis/review/MerchantReviewsAPI'
import { fetchRiderMeIO } from '@/apis/rider/RiderMeAPI'
import { fetchCustomerMeIO } from '@/apis/user/CustomerMeAPI'
import { useAuthSession } from '@/hooks/useAuthSession'
import type { AuthSession } from '@/lib/auth-session'
import { cn } from '@/lib/utils'
import type { Merchant } from '@/objects/merchant/Merchant'
import type { Order } from '@/objects/order/Order'
import type { OrderChatRole } from '@/objects/order/OrderChatMessage'
import type { OrderChatUnreadCount } from '@/objects/order/OrderChatUnreadCount'
import type { Rider } from '@/objects/rider/Rider'
import type { Promotion } from '@/objects/shared/Promotion'
import { OrderStatuses, RefundStatuses, UserRoles } from '@/objects/shared/ids'

type GlobalNotification = {
  id: string
  message: string
  target: string
  createdAt: number
}

type NotificationSnapshot = {
  initialized: boolean
  seenEvents: string[]
  chatCounts: Record<string, number>
}

type NotificationContext = {
  orders: Order[]
  merchants: Merchant[]
  currentRider?: Rider | null
}

const emptySnapshot: NotificationSnapshot = {
  initialized: false,
  seenEvents: [],
  chatCounts: {},
}

const pollIntervalMs = 8000

function storageKey(session: AuthSession, suffix: string) {
  return `delivery-global-notifications:${session.role}:${session.account}:${suffix}`
}

function safeReadJson<T>(key: string, fallback: T): T {
  try {
    const raw = window.localStorage.getItem(key)
    return raw ? (JSON.parse(raw) as T) : fallback
  } catch {
    return fallback
  }
}

function readSnapshot(session: AuthSession): NotificationSnapshot {
  const value = safeReadJson<Partial<NotificationSnapshot>>(storageKey(session, 'snapshot'), emptySnapshot)
  return {
    initialized: Boolean(value.initialized),
    seenEvents: Array.isArray(value.seenEvents) ? value.seenEvents : [],
    chatCounts: value.chatCounts && typeof value.chatCounts === 'object' ? value.chatCounts : {},
  }
}

function writeSnapshot(session: AuthSession, snapshot: NotificationSnapshot) {
  window.localStorage.setItem(storageKey(session, 'snapshot'), JSON.stringify(snapshot))
}

function readNotifications(session: AuthSession): GlobalNotification[] {
  const value = safeReadJson<GlobalNotification[]>(storageKey(session, 'items'), [])
  return Array.isArray(value) ? value : []
}

function writeNotifications(session: AuthSession, notifications: GlobalNotification[]) {
  window.localStorage.setItem(storageKey(session, 'items'), JSON.stringify(notifications))
}

function roleLabel(role: string) {
  if (role === UserRoles.customer) return '顾客'
  if (role === UserRoles.merchant) return '商家'
  if (role === UserRoles.rider) return '骑手'
  return role
}

function merchantName(order: Order | undefined, context: NotificationContext) {
  if (!order) return '未知店铺'
  return context.merchants.find((merchant) => merchant.id === order.merchantId)?.storeName ?? `店铺 ${order.merchantId}`
}

function peerDisplayName(order: Order | undefined, peerRole: OrderChatRole | string, context: NotificationContext) {
  if (peerRole === UserRoles.customer) return order?.customerName || order?.customerId || '顾客'
  if (peerRole === UserRoles.merchant) return merchantName(order, context)
  if (peerRole === UserRoles.rider) {
    if (context.currentRider && (!order?.riderId || context.currentRider.id === order.riderId)) return context.currentRider.name
    return order?.riderId ? `骑手 ${order.riderId}` : '骑手'
  }
  return roleLabel(peerRole)
}

function latestMessageSummary(count: OrderChatUnreadCount) {
  if (count.latestMessageType === 'image') return '发送了图片'
  const content = count.latestContent?.trim()
  if (!content) return `发送了 ${count.unreadCount} 条新消息`
  return `发送了：“${content.length > 48 ? `${content.slice(0, 48)}...` : content}”`
}

function refundFeedbackMessage(order: Order, context: NotificationContext) {
  const storeName = merchantName(order, context)
  if (order.refundStatus === RefundStatuses.accepted) return `店铺「${storeName}」的订单 ${order.id} 退款申请已通过`
  if (order.refundStatus === RefundStatuses.rejected) return `店铺「${storeName}」的订单 ${order.id} 退款申请已被平台驳回`
  if (order.refundStatus === RefundStatuses.merchantRejected) return `店铺「${storeName}」驳回了订单 ${order.id} 的退款申请`
  if (order.refundStatus === RefundStatuses.adminPending) return `你已将店铺「${storeName}」的订单 ${order.id} 退款申请提交平台仲裁`
  return null
}

function enabledPromotionKeys(merchants: Merchant[], platformPromotions: Promotion[]) {
  const platform = platformPromotions
    .filter((promotion) => promotion.enabled)
    .map((promotion) => `platform:${promotion.id}:${promotion.title}`)
  const merchant = merchants.flatMap((store) =>
    (store.promotions ?? [])
      .filter((promotion) => promotion.enabled)
      .map((promotion) => `merchant:${store.id}:${promotion.id}:${promotion.title}`),
  )
  return [...platform, ...merchant]
}

function makeNotification(id: string, message: string, target: string): GlobalNotification {
  return { id, message, target, createdAt: Date.now() }
}

function mergeNotifications(current: GlobalNotification[], incoming: GlobalNotification[]) {
  const byId = new Map(current.map((item) => [item.id, item]))
  incoming.forEach((item) => {
    byId.set(item.id, byId.get(item.id) ?? item)
  })
  return [...byId.values()].sort((a, b) => b.createdAt - a.createdAt)
}

async function customerEvents() {
  const [me, ordersResponse, catalog, unreadResponse] = await Promise.all([
    fetchCustomerMeIO()(),
    fetchCustomerOrdersIO()(),
    fetchCatalogIO()(),
    fetchOrderChatUnreadCountsIO()(),
  ])
  const orders = [...ordersResponse.pendingOrders, ...ordersResponse.historyOrders]
  const context: NotificationContext = { orders, merchants: catalog.merchants }
  const customerId = me.customerAccount.profile.id
  const reviewMerchantIds = [...new Set(orders.map((order) => order.merchantId))]
  const reviewResponses = await Promise.all(reviewMerchantIds.map((merchantId) => fetchMerchantReviewsIO(merchantId)().catch(() => null)))
  const reviewReplyEvents = reviewResponses.flatMap((response) =>
    (response?.reviews ?? [])
      .filter((review) => review.customerId === customerId && Boolean(review.merchantReply?.trim()))
      .map((review) => ({
        key: `customer-review-reply:${review.id}:${review.merchantReplyAt ?? review.merchantReply}`,
        message: `店铺「${catalog.merchants.find((merchant) => merchant.id === review.merchantId)?.storeName ?? review.merchantId}」回复了你对订单 ${review.orderId} 的评价`,
        target: `/delivery/customer/m/${encodeURIComponent(review.merchantId)}`,
      })),
  )

  return {
    chatCounts: unreadResponse.counts,
    context,
    events: [
      ...orders.map((order) => ({
        key: `customer-order-created:${order.id}`,
        message: `你已向店铺「${merchantName(order, context)}」下单，订单 ${order.id} 已创建`,
        target: '/delivery/customer?tab=profile',
      })),
      ...orders
        .filter((order) => order.status === OrderStatuses.delivered)
        .map((order) => ({
          key: `customer-order-delivered:${order.id}`,
          message: `店铺「${merchantName(order, context)}」的订单 ${order.id} 已送达`,
          target: '/delivery/customer?tab=profile',
        })),
      ...orders.flatMap((order) => {
        const message = refundFeedbackMessage(order, context)
        return message && order.refundStatus
          ? [{ key: `customer-refund:${order.id}:${order.refundStatus}:${order.refundMerchantReason ?? ''}:${order.refundAdminReason ?? ''}`, message, target: '/delivery/customer?tab=profile' }]
          : []
      }),
      ...enabledPromotionKeys(catalog.merchants, catalog.platformPromotions ?? []).map((key) => ({
        key: `customer-promotion:${key}`,
        message: key.startsWith('platform:')
          ? `平台推出了新的优惠：${key.split(':')[2] ?? '优惠'}`
          : `店铺「${catalog.merchants.find((merchant) => merchant.id === key.split(':')[1])?.storeName ?? key.split(':')[1] ?? '商家'}」推出了新的优惠：${key.split(':')[3] ?? '优惠'}`,
        target: '/delivery/customer?tab=home',
      })),
      ...reviewReplyEvents,
    ],
  }
}

async function merchantEvents() {
  const [me, refunds, unreadResponse] = await Promise.all([
    fetchMerchantMeIO()(),
    fetchMerchantRefundRequestsIO()().catch(() => ({ requests: [] })),
    fetchOrderChatUnreadCountsIO()(),
  ])
  const stores = me.merchantAccount.profile.stores ?? []
  const orders = stores.flatMap((store) => [...store.pendingOrders, ...store.historyOrders])
  const merchants = stores.map((store) => store.merchant)
  const context: NotificationContext = { orders, merchants }
  const onboardingEvents = (me.onboardingRequests ?? [])
    .filter((request) => request.status !== 'pending')
    .map((request) => ({
      key: `merchant-onboarding:${request.id}:${request.status}:${request.reviewedAt ?? ''}`,
      message: request.status === 'accepted' ? `店铺「${request.storeName}」入驻申请已通过` : `店铺「${request.storeName}」入驻申请已驳回`,
      target: '/delivery/merchant?tab=profile',
    }))

  return {
    chatCounts: unreadResponse.counts,
    context,
    events: [
      ...onboardingEvents,
      ...refunds.requests
        .filter((order) => order.refundStatus === RefundStatuses.pending || order.refundStatus === RefundStatuses.legacyPending)
        .map((order) => ({
          key: `merchant-refund-request:${order.id}:${order.refundRequestedAt ?? ''}`,
          message: `顾客「${order.customerName || order.customerId}」向店铺「${merchantName(order, context)}」提出订单 ${order.id} 的退款申请：${order.refundReason || '未填写原因'}`,
          target: '/delivery/merchant?tab=reviews',
        })),
      ...refunds.requests
        .filter((order) => order.refundStatus === RefundStatuses.accepted && Boolean(order.refundAdminReason?.trim()))
        .map((order) => ({
          key: `merchant-refund-forced:${order.id}:${order.refundedAt ?? order.refundAdminReason ?? ''}`,
          message: `平台已强制通过顾客「${order.customerName || order.customerId}」对店铺「${merchantName(order, context)}」订单 ${order.id} 的退款申请`,
          target: '/delivery/merchant?tab=reviews',
        })),
    ],
  }
}

async function adminEvents() {
  const [onboarding, refunds] = await Promise.all([
    fetchAdminStoreOnboardingRequestsIO()(),
    fetchAdminRefundRequestsIO()().catch(() => ({ requests: [] })),
  ])
  return {
    chatCounts: [],
    events: [
      ...onboarding.requests
        .filter((request) => request.status === 'pending')
        .map((request) => ({
          key: `admin-onboarding:${request.id}:${request.createdAt}`,
          message: `商家账号「${request.ownerUsername}」提交了店铺「${request.storeName}」入驻申请`,
          target: '/delivery/admin',
        })),
      ...refunds.requests
        .filter((order) => order.refundStatus === RefundStatuses.adminPending)
        .map((order) => ({
          key: `admin-refund:${order.id}:${order.refundRequestedAt ?? ''}:${order.refundMerchantReviewedAt ?? ''}`,
          message: `顾客「${order.customerName || order.customerId}」提交了订单 ${order.id} 的退款仲裁申请`,
          target: '/delivery/admin',
        })),
    ],
  }
}

async function riderEvents() {
  const [me, catalog, unreadResponse] = await Promise.all([
    fetchRiderMeIO()(),
    fetchCatalogIO()().catch(() => ({ merchants: [], products: [], platformPromotions: [] })),
    fetchOrderChatUnreadCountsIO()(),
  ])
  const orders = [
    ...(me.riderAccount.profile.pendingOrders ?? []),
    ...(me.riderAccount.profile.historyOrders ?? []),
    ...(me.availableOrders ?? []),
  ]
  return {
    chatCounts: unreadResponse.counts,
    context: {
      orders,
      merchants: catalog.merchants,
      currentRider: me.riderAccount.profile.rider,
    } satisfies NotificationContext,
    events: [],
  }
}

async function collectEvents(session: AuthSession): Promise<{ chatCounts: OrderChatUnreadCount[]; events: Array<{ key: string; message: string; target: string }>; context: NotificationContext }> {
  if (session.role === UserRoles.customer) return customerEvents()
  if (session.role === UserRoles.merchant) return merchantEvents()
  if (session.role === UserRoles.admin) return { ...(await adminEvents()), context: { orders: [], merchants: [] } }
  if (session.role === UserRoles.rider) return riderEvents()
  return { chatCounts: [], events: [], context: { orders: [], merchants: [] } }
}

function chatEvents(counts: OrderChatUnreadCount[], snapshot: NotificationSnapshot, context: NotificationContext) {
  const nextChatCounts = { ...snapshot.chatCounts }
  const notifications: GlobalNotification[] = []
  counts.forEach((count) => {
    const key = `${count.orderId}:${count.peerRole}`
    const previous = snapshot.chatCounts[key] ?? 0
    nextChatCounts[key] = count.unreadCount
    if (count.unreadCount > previous && count.unreadCount > 0) {
      const order = context.orders.find((item) => item.id === count.orderId)
      const peerName = peerDisplayName(order, count.peerRole, context)
      const storeName = merchantName(order, context)
      notifications.push(
        makeNotification(
          `chat:${key}:${count.unreadCount}:${count.latestMessageType ?? ''}:${count.latestContent ?? ''}`,
          `${roleLabel(count.peerRole)}「${peerName}」在店铺「${storeName}」的订单 ${count.orderId} 中${latestMessageSummary(count)}（${count.unreadCount} 条未读）`,
          `/delivery/chat/${encodeURIComponent(count.orderId)}?peer=${encodeURIComponent(count.peerRole)}`,
        ),
      )
    }
  })
  return { nextChatCounts, notifications }
}

export function GlobalNotificationCenter() {
  const session = useAuthSession()
  const [notifications, setNotifications] = useState<GlobalNotification[]>([])

  const activeSession = useMemo(() => (session && session.role ? session : null), [session])

  useEffect(() => {
    const timer = window.setTimeout(() => {
      if (!activeSession) {
        setNotifications([])
        return
      }
      setNotifications(readNotifications(activeSession))
    }, 0)
    return () => window.clearTimeout(timer)
  }, [activeSession])

  const poll = useCallback(async () => {
    if (!activeSession) return
    try {
      const snapshot = readSnapshot(activeSession)
      const seenEvents = new Set(snapshot.seenEvents)
      const result = await collectEvents(activeSession)
      const { nextChatCounts, notifications: chatNotifications } = chatEvents(result.chatCounts, snapshot, result.context)
      const eventNotifications = result.events.flatMap((event) => {
        if (seenEvents.has(event.key)) return []
        seenEvents.add(event.key)
        return snapshot.initialized ? [makeNotification(event.key, event.message, event.target)] : []
      })
      const nextSnapshot = {
        initialized: true,
        seenEvents: [...seenEvents].slice(-600),
        chatCounts: nextChatCounts,
      }
      writeSnapshot(activeSession, nextSnapshot)
      const nextNotifications = mergeNotifications(readNotifications(activeSession), [...chatNotifications, ...eventNotifications])
      writeNotifications(activeSession, nextNotifications)
      setNotifications(nextNotifications)
    } catch {
      // 全局通知不能打断当前页面的正常业务流程。
    }
  }, [activeSession])

  useEffect(() => {
    if (!activeSession) return
    const initialTimer = window.setTimeout(() => {
      void poll()
    }, 0)
    const timer = window.setInterval(() => {
      void poll()
    }, pollIntervalMs)
    return () => {
      window.clearTimeout(initialTimer)
      window.clearInterval(timer)
    }
  }, [activeSession, poll])

  const handleClick = (notification: GlobalNotification) => {
    if (!activeSession) return
    const next = notifications.filter((item) => item.id !== notification.id)
    writeNotifications(activeSession, next)
    setNotifications(next)
    window.location.assign(notification.target)
  }

  if (!activeSession || notifications.length === 0) return null

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
