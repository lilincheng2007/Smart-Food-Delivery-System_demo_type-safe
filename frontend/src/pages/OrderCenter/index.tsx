import { useEffect } from 'react'

import { DeliveryLogoutBar } from '@/components/DeliveryLogoutBar'
import { DeliveryPageShell } from '@/components/DeliveryPageShell'
import { Card, CardContent } from '@/components/ui/card'
import { useAppChrome } from '@/hooks/useAppChrome'
import { useOrderCenterStore } from '@/stores/pages/use-order-center-store'

import { DispatchCard } from './DispatchCard'
import { OrderQueueCard } from './OrderQueueCard'
import { StatusFlowCard } from './StatusFlowCard'

export default function OrderCenter() {
  const { showNotice } = useAppChrome()
  const panel = useOrderCenterStore((state) => state.panel)
  const error = useOrderCenterStore((state) => state.error)
  const resetPage = useOrderCenterStore((state) => state.resetPage)
  const loadPanel = useOrderCenterStore((state) => state.loadPanel)

  useEffect(() => {
    resetPage()
    void loadPanel()
  }, [loadPanel, resetPage])

  const orders = panel?.orders ?? []
  const riders = panel?.riders ?? []

  return (
    <DeliveryPageShell>
      {error ? (
        <Card className="border-rose-200 bg-rose-50/90">
          <CardContent className="p-4 text-sm text-rose-800">{error}</CardContent>
        </Card>
      ) : null}

      <StatusFlowCard />
      <DispatchCard riders={riders} onDispatch={() => showNotice('自动派单由后端 API 提供后再接线。', 'info')} />
      <OrderQueueCard
        orders={orders}
        onAdvanceStatus={() => showNotice('订单状态推进由后端 API 提供后再接线。', 'info')}
      />

      <DeliveryLogoutBar />
    </DeliveryPageShell>
  )
}
