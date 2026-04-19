import { useEffect } from 'react'

import { DeliveryPageShell } from '@/components/DeliveryPageShell'
import { Card, CardContent } from '@/components/ui/card'
import { useAppChrome } from '@/hooks/useAppChrome'
import { useRiderAppStore } from '@/stores/pages/use-rider-app-store'

import { DispatchCard } from './DispatchCard'
import { RiderOverview } from './RiderOverview'
import { SalaryCard } from './SalaryCard'
import { TaskListCard } from './TaskListCard'

export default function RiderApp() {
  const { showNotice } = useAppChrome()
  const bootstrapDone = useRiderAppStore((state) => state.bootstrapDone)
  const loadError = useRiderAppStore((state) => state.loadError)
  const riderAccount = useRiderAppStore((state) => state.riderAccount)
  const resetPage = useRiderAppStore((state) => state.resetPage)
  const bootstrap = useRiderAppStore((state) => state.bootstrap)

  useEffect(() => {
    resetPage()
    void bootstrap()
  }, [bootstrap, resetPage])

  const rider = riderAccount?.profile.rider ?? null
  const assignedOrders = riderAccount ? riderAccount.profile.pendingOrders : []

  if (!bootstrapDone) {
    return (
      <DeliveryPageShell title="骑手端核心功能" description="正在加载…" roleBadge="骑手 APP">
        <Card className="border-orange-100 bg-white/95">
          <CardContent className="p-6 text-sm text-slate-600">加载中…</CardContent>
        </Card>
      </DeliveryPageShell>
    )
  }

  if (loadError) {
    return (
      <DeliveryPageShell title="骑手端核心功能" description="加载失败" roleBadge="骑手 APP">
        <Card className="border-orange-100 bg-white/95">
          <CardContent className="p-6 text-sm text-rose-600">{loadError}</CardContent>
        </Card>
      </DeliveryPageShell>
    )
  }

  if (!riderAccount || !rider) {
    return (
      <DeliveryPageShell
        title="骑手端核心功能"
        description="当前账号未绑定骑手信息，请先完成骑手注册资料。"
        roleBadge="骑手 APP"
      >
        <Card className="border-orange-100 bg-white/95">
          <CardContent className="p-6 text-sm text-slate-600">未找到当前骑手账号档案。</CardContent>
        </Card>
      </DeliveryPageShell>
    )
  }

  return (
    <DeliveryPageShell
      title="骑手端核心功能"
      description="支持骑手注册登录、抢单/派单、查看订单详情与导航、更新配送状态。"
      roleBadge="骑手 APP"
    >
      <RiderOverview rider={rider} />
      <DispatchCard rider={rider} onGrabOrder={() => showNotice('抢单由后端 API 提供后再接线。', 'info')} />
      <TaskListCard
        orders={assignedOrders}
        onNavigate={() => showNotice('地图导航为端能力；与订单联动由后端 API 提供后再接线。', 'info')}
        onUpdateStatus={() => showNotice('配送状态更新由后端 API 提供后再接线。', 'info')}
      />
      <SalaryCard walletBalance={riderAccount.profile.walletBalance} salary={rider.salary} />
    </DeliveryPageShell>
  )
}
