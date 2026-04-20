import { useEffect } from 'react'

import { DeliveryLogoutBar } from '@/components/DeliveryLogoutBar'
import { DeliveryPageShell } from '@/components/DeliveryPageShell'
import { FloatingPageTools } from '@/components/FloatingPageTools'
import { Card, CardContent } from '@/components/ui/card'
import type { PromotionCampaign } from '@/objects/admin'
import { useAppChrome } from '@/hooks/useAppChrome'
import { useDeliveryDashboardStore } from '@/stores/pages/use-delivery-dashboard-store'

import { CampaignList } from './CampaignList'
import { OverviewStats } from './OverviewStats'
import { ModuleEntryGrid } from './ModuleEntryGrid'
import { pageEvents } from './constants'

export default function DeliveryDashboard() {
  const { showNotice } = useAppChrome()
  const overview = useDeliveryDashboardStore((state) => state.overview)
  const overviewError = useDeliveryDashboardStore((state) => state.overviewError)
  const resetPage = useDeliveryDashboardStore((state) => state.resetPage)
  const loadOverview = useDeliveryDashboardStore((state) => state.loadOverview)

  useEffect(() => {
    resetPage()
    void loadOverview()
  }, [loadOverview, resetPage])

  const merchants = overview?.merchants ?? []
  const orders = overview?.orders ?? []
  const riders = overview?.riders ?? []
  const campaigns = overview?.campaigns ?? []
  const complaintTickets = overview?.complaintTickets ?? []

  return (
    <DeliveryPageShell>
      <FloatingPageTools
        events={pageEvents}
        onEventSelect={(event) => showNotice(`${event.label}：${event.description}`, 'info')}
      />

      {overviewError ? (
        <Card className="border-rose-200 bg-rose-50/90">
          <CardContent className="p-4 text-sm text-rose-800">{overviewError}</CardContent>
        </Card>
      ) : null}

      <OverviewStats
        merchantCount={merchants.length}
        busyRiderCount={riders.filter((item) => item.status !== '空闲').length}
        orderCount={orders.length}
        complaintTickets={complaintTickets}
      />
      <ModuleEntryGrid />
      <CampaignList
        campaigns={campaigns as PromotionCampaign[]}
        onCreateCampaign={() => showNotice('新增活动由后端 API 提供后再接线。', 'info')}
      />

      <DeliveryLogoutBar />
    </DeliveryPageShell>
  )
}
