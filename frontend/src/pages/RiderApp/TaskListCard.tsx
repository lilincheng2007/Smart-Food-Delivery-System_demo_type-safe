import { MapPinned, Navigation, Route } from 'lucide-react'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import type { Order } from '@/objects/order'

interface TaskListCardProps {
  orders: Order[]
  onNavigate: () => void
  onUpdateStatus: () => void
}

export function TaskListCard({ orders, onNavigate, onUpdateStatus }: TaskListCardProps) {
  return (
    <Card className="border-orange-100 bg-white/95">
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <MapPinned className="size-5 text-orange-500" />
          配送任务
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-3">
        {orders.map((order) => (
          <div key={order.id} className="space-y-2 rounded-xl border border-orange-100 p-4">
            <div className="flex items-center justify-between">
              <p className="font-medium text-slate-900">订单 {order.id}</p>
              <Badge variant="outline">{order.status}</Badge>
            </div>
            <p className="text-sm text-slate-600">配送地址：{order.deliveryAddress}</p>
            <div className="flex gap-2">
              <Button size="sm" variant="outline" onClick={onNavigate}>
                <Navigation className="size-4" />
                去导航
              </Button>
              <Button size="sm" onClick={onUpdateStatus}>
                <Route className="size-4" />
                更新状态
              </Button>
            </div>
          </div>
        ))}
      </CardContent>
    </Card>
  )
}
