import { useEffect, useMemo, useState } from 'react'

import { fetchOrderChatUnreadCountsIO } from '@/apis/order/OrderChatAPI'
import type { OrderChatRole } from '@/objects/order/OrderChatMessage'
import type { OrderChatUnreadCount } from '@/objects/order/OrderChatUnreadCount'
import type { OrderId } from '@/objects/shared/ids'

export type OrderChatUnreadLookup = (orderId: OrderId, peerRole: OrderChatRole) => number

function keyOf(orderId: OrderId, peerRole: OrderChatRole): string {
  return `${orderId}::${peerRole}`
}

export function useOrderChatUnreadCounts(enabled = true) {
  const [counts, setCounts] = useState<OrderChatUnreadCount[]>([])

  const refresh = async () => {
    if (!enabled) return
    try {
      const response = await fetchOrderChatUnreadCountsIO()()
      setCounts(response.counts)
    } catch {
      // Auth or backend availability errors are already surfaced by normal page actions.
    }
  }

  useEffect(() => {
    if (!enabled) return
    void refresh()
    const timer = window.setInterval(() => {
      void refresh()
    }, 5000)
    return () => window.clearInterval(timer)
  }, [enabled])

  const lookup = useMemo(() => {
    const map = new Map(counts.map((item) => [keyOf(item.orderId, item.peerRole), item.unreadCount]))
    return ((orderId: OrderId, peerRole: OrderChatRole) => map.get(keyOf(orderId, peerRole)) ?? 0) satisfies OrderChatUnreadLookup
  }, [counts])

  return { counts, unreadFor: lookup, refreshUnreadCounts: refresh }
}
