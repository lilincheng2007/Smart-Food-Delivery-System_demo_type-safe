import { useEffect } from 'react'

import { DeliveryLogoutBar } from '@/components/DeliveryLogoutBar'
import { DeliveryPageShell } from '@/components/DeliveryPageShell'
import { Card, CardContent } from '@/components/ui/card'
import { useAppChrome } from '@/hooks/useAppChrome'
import { useOrderChatUnreadCounts } from '@/hooks/useOrderChatUnreadCounts'
import { useRiderAppStore } from '@/pages/RiderApp/stores/use-rider-app-store'

import { DispatchCard } from './components/DispatchCard'
import { EnergyTimeoutCard } from './components/EnergyTimeoutCard'
import { RiderOverview } from './components/RiderOverview'
import { SalaryCard } from './components/SalaryCard'
import { TaskListCard } from './components/TaskListCard'

export default function RiderApp() {
  const { showNotice } = useAppChrome()
  const bootstrapDone = useRiderAppStore((state) => state.bootstrapDone)
  const loadError = useRiderAppStore((state) => state.loadError)
  const riderAccount = useRiderAppStore((state) => state.riderAccount)
  const availableOrders = useRiderAppStore((state) => state.availableOrders)
  const deliveryStatuses = useRiderAppStore((state) => state.deliveryStatuses)
  const reviewSummary = useRiderAppStore((state) => state.reviewSummary)
  const reviews = useRiderAppStore((state) => state.reviews)
  const resetPage = useRiderAppStore((state) => state.resetPage)
  const bootstrap = useRiderAppStore((state) => state.bootstrap)
  const refreshRider = useRiderAppStore((state) => state.refreshRider)
  const grabOrder = useRiderAppStore((state) => state.grabOrder)
  const updateOrderStatus = useRiderAppStore((state) => state.updateOrderStatus)
  const redeemTimeoutCard = useRiderAppStore((state) => state.redeemTimeoutCard)
  const applyTimeoutCard = useRiderAppStore((state) => state.useTimeoutCard)
  const { unreadFor } = useOrderChatUnreadCounts(Boolean(riderAccount))

  useEffect(() => {
    resetPage()
    void bootstrap()
  }, [bootstrap, resetPage])

  useEffect(() => {
    const timer = window.setInterval(() => {
      void refreshRider().catch(() => {})
    }, 5000)

    return () => {
      window.clearInterval(timer)
    }
  }, [refreshRider])

  const rider = riderAccount?.profile.rider ?? null
  const assignedOrders = riderAccount ? riderAccount.profile.pendingOrders : []
  const historyOrders = riderAccount ? riderAccount.profile.historyOrders : []

  if (!bootstrapDone) {
    return (
      <DeliveryPageShell>
        <Card className="border-orange-100 bg-white/95">
          <CardContent className="p-6 text-sm text-slate-600">加载中…</CardContent>
        </Card>
        <DeliveryLogoutBar />
      </DeliveryPageShell>
    )
  }

  if (loadError) {
    return (
      <DeliveryPageShell>
        <Card className="border-orange-100 bg-white/95">
          <CardContent className="p-6 text-sm text-rose-600">{loadError}</CardContent>
        </Card>
        <DeliveryLogoutBar />
      </DeliveryPageShell>
    )
  }

  if (!riderAccount || !rider) {
    return (
      <DeliveryPageShell>
        <Card className="border-orange-100 bg-white/95">
          <CardContent className="p-6 text-sm text-slate-600">未找到当前骑手账号档案。</CardContent>
        </Card>
        <DeliveryLogoutBar />
      </DeliveryPageShell>
    )
  }

  return (
    <DeliveryPageShell>
      <RiderOverview rider={rider} />
      <EnergyTimeoutCard
        rider={rider}
        onRedeem={() => {
          void redeemTimeoutCard()
            .then(() => showNotice('兑换成功，已获得 1 张超时免责卡。', 'success'))
            .catch((error) => showNotice(error instanceof Error ? error.message : '兑换失败', 'error'))
        }}
      />
      <DispatchCard
        availableOrders={availableOrders}
        unreadFor={unreadFor}
        onGrabOrder={(orderId) => {
          void grabOrder(orderId)
            .then(() => showNotice('抢单成功，订单已进入你的配送任务。', 'success'))
            .catch((error) => showNotice(error instanceof Error ? error.message : '抢单失败', 'error'))
        }}
      />
      <TaskListCard
        orders={assignedOrders}
        historyOrders={historyOrders}
        deliveryStatuses={deliveryStatuses}
        unreadFor={unreadFor}
        onUpdateStatus={(orderId) => {
          void updateOrderStatus(orderId)
            .then((result) => {
              if (result.wasTimeout && result.timeoutCardUsed) {
                showNotice('订单已送达；本单超时不奖励能量，已自动使用免责卡。', 'success')
                return
              }
              if (result.wasTimeout) {
                showNotice('订单已送达；本单超时不奖励能量，且未免责。', 'error')
                return
              }
              showNotice(`订单已送达，获得 ${result.earnedEnergy} 能量。`, 'success')
            })
            .catch((error) => showNotice(error instanceof Error ? error.message : '更新状态失败', 'error'))
        }}
        onUseTimeoutCard={(orderId) => {
          void applyTimeoutCard(orderId)
            .then(() => showNotice('免责卡已使用，本单超时已免责。', 'success'))
            .catch((error) => showNotice(error instanceof Error ? error.message : '使用免责卡失败', 'error'))
        }}
      />
      <SalaryCard salary={rider.salary} reviewSummary={reviewSummary} reviews={reviews} />
      <DeliveryLogoutBar />
    </DeliveryPageShell>
  )
}
