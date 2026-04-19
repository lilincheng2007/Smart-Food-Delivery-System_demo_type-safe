import { Card, CardHeader, CardDescription, CardTitle } from '@/components/ui/card'
import type { ComplaintTicket } from '@/objects/admin'

interface OverviewStatsProps {
  merchantCount: number
  busyRiderCount: number
  orderCount: number
  complaintTickets: ComplaintTicket[]
}

export function OverviewStats({
  merchantCount,
  busyRiderCount,
  orderCount,
  complaintTickets,
}: OverviewStatsProps) {
  return (
    <section className="grid gap-4 md:grid-cols-4">
      <Card className="border-orange-100 bg-white/95 py-0">
        <CardHeader className="pb-2">
          <CardDescription>商家数量</CardDescription>
          <CardTitle>{merchantCount}</CardTitle>
        </CardHeader>
      </Card>
      <Card className="border-orange-100 bg-white/95 py-0">
        <CardHeader className="pb-2">
          <CardDescription>骑手在线</CardDescription>
          <CardTitle>{busyRiderCount}</CardTitle>
        </CardHeader>
      </Card>
      <Card className="border-orange-100 bg-white/95 py-0">
        <CardHeader className="pb-2">
          <CardDescription>今日订单</CardDescription>
          <CardTitle>{orderCount}</CardTitle>
        </CardHeader>
      </Card>
      <Card className="border-orange-100 bg-white/95 py-0">
        <CardHeader className="pb-2">
          <CardDescription>待处理投诉</CardDescription>
          <CardTitle>{complaintTickets.filter((item) => item.status !== '已解决').length}</CardTitle>
        </CardHeader>
      </Card>
    </section>
  )
}
