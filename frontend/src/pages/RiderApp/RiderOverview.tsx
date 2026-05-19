import { Card, CardHeader, CardDescription, CardTitle } from '@/components/ui/card'
import type { Rider } from '@/objects/rider/Rider'

interface RiderOverviewProps {
  rider: Rider
}

export function RiderOverview({ rider }: RiderOverviewProps) {
  return (
    <section className="grid gap-4 md:grid-cols-3">
      <Card className="border-orange-100 bg-white/95 py-0">
        <CardHeader className="pb-2">
          <CardDescription>骑手姓名</CardDescription>
          <CardTitle>{rider.name}</CardTitle>
        </CardHeader>
      </Card>
      <Card className="border-orange-100 bg-white/95 py-0">
        <CardHeader className="pb-2">
          <CardDescription>当前状态</CardDescription>
          <CardTitle>{rider.status}</CardTitle>
        </CardHeader>
      </Card>
      <Card className="border-orange-100 bg-white/95 py-0">
        <CardHeader className="pb-2">
          <CardDescription>累计接单</CardDescription>
          <CardTitle>{rider.totalOrders}</CardTitle>
        </CardHeader>
      </Card>
    </section>
  )
}
