import { useEffect } from 'react'

import { DeliveryPageShell } from '@/components/DeliveryPageShell'
import { Card, CardContent } from '@/components/ui/card'
import { useAppChrome } from '@/hooks/useAppChrome'
import { usePlatformAdminStore } from '@/stores/pages/use-platform-admin-store'

import { AdminMetricsCard } from './AdminMetricsCard'
import { CustomerServiceCard } from './CustomerServiceCard'
import { FeeCollectionCard } from './FeeCollectionCard'
import { OperationsManagerCard } from './OperationsManagerCard'

export default function PlatformAdmin() {
  const { showNotice } = useAppChrome()
  const meta = usePlatformAdminStore((state) => state.meta)
  const error = usePlatformAdminStore((state) => state.error)
  const adminName = usePlatformAdminStore((state) => state.adminName)
  const resetPage = usePlatformAdminStore((state) => state.resetPage)
  const bootstrap = usePlatformAdminStore((state) => state.bootstrap)

  useEffect(() => {
    resetPage()
    void bootstrap()
  }, [bootstrap, resetPage])

  const operationsManagers = meta?.operationsManagers ?? []
  const serviceAgents = meta?.serviceAgents ?? []
  const merchantApplications = meta?.merchantApplications ?? []
  const complaintTickets = meta?.complaintTickets ?? []
  const campaigns = meta?.campaigns ?? []

  return (
    <DeliveryPageShell
      title="平台后端与管理系统"
      description="包含运营经理后台、客服后台和基础管理能力：商家审核、活动发放、加盟费收取、投诉处理等。"
      roleBadge="管理后台"
    >
      {adminName ? (
        <Card className="border-orange-100 bg-white/95">
          <CardContent className="p-4 text-sm text-slate-700">当前管理员：{adminName}</CardContent>
        </Card>
      ) : null}
      {error ? (
        <Card className="border-rose-200 bg-rose-50/90">
          <CardContent className="p-4 text-sm text-rose-800">{error}</CardContent>
        </Card>
      ) : null}

      <section className="grid gap-4 md:grid-cols-2">
        <OperationsManagerCard
          manager={operationsManagers[0]}
          onReviewMerchant={() => showNotice('商家入驻审核由后端 API 提供后再接线。', 'info')}
          onCreateCampaign={() => showNotice('活动发放由后端 API 提供后再接线。', 'info')}
        />
        <CustomerServiceCard
          agent={serviceAgents[0]}
          onResolveTicket={() => showNotice('投诉工单处理由后端 API 提供后再接线。', 'info')}
        />
      </section>

      <section className="grid gap-4 md:grid-cols-2">
        <FeeCollectionCard merchantApplicationCount={merchantApplications.length} />
        <AdminMetricsCard
          merchantApplications={merchantApplications}
          complaintTickets={complaintTickets}
          campaigns={campaigns}
        />
      </section>
    </DeliveryPageShell>
  )
}
