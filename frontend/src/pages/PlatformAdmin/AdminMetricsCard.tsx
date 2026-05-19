import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import type { ComplaintTicket } from '@/objects/admin/ComplaintTicket'
import type { MerchantApplication } from '@/objects/admin/MerchantApplication'
import type { PromotionCampaign } from '@/objects/admin/PromotionCampaign'

interface AdminMetricsCardProps {
  merchantApplications: MerchantApplication[]
  complaintTickets: ComplaintTicket[]
  campaigns: PromotionCampaign[]
}

export function AdminMetricsCard({
  merchantApplications,
  complaintTickets,
  campaigns,
}: AdminMetricsCardProps) {
  return (
    <Card className="border-orange-100 bg-white/95">
      <CardHeader>
        <CardTitle>基础管理后台数据</CardTitle>
        <CardDescription>商家申请、投诉工单、活动计划</CardDescription>
      </CardHeader>
      <CardContent className="space-y-3 text-sm text-slate-700">
        <div className="flex items-center justify-between rounded-xl border border-orange-100 p-3">
          <span>待审核商家</span>
          <Badge variant="outline">{merchantApplications.filter((item) => item.status === '待审核').length}</Badge>
        </div>
        <div className="flex items-center justify-between rounded-xl border border-orange-100 p-3">
          <span>未解决工单</span>
          <Badge variant="outline">{complaintTickets.filter((item) => item.status !== '已解决').length}</Badge>
        </div>
        <div className="flex items-center justify-between rounded-xl border border-orange-100 p-3">
          <span>进行中活动</span>
          <Badge variant="outline">{campaigns.filter((item) => item.status === '进行中').length}</Badge>
        </div>
      </CardContent>
    </Card>
  )
}
