import { Clock3, Route } from 'lucide-react'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import type { Order } from '@/objects/order'

interface OrderQueueCardProps {
  orders: Order[]
  onAdvanceStatus: () => void
}

export function OrderQueueCard({ orders, onAdvanceStatus }: OrderQueueCardProps) {
  return (
    <Card className="border-orange-100 bg-white/95">
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Clock3 className="size-5 text-orange-500" />
          订单队列
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-3">
        {orders.map((order) => (
          <div key={order.id} className="rounded-xl border border-orange-100 p-4">
            <div className="flex items-center justify-between">
              <p className="font-medium text-slate-900">订单 {order.id}</p>
              <Badge variant="outline">{order.status}</Badge>
            </div>
            <p className="mt-1 text-sm text-slate-600">金额 {order.totalAmount} 元 · 地址 {order.deliveryAddress}</p>
            <Button size="sm" variant="outline" className="mt-3" onClick={onAdvanceStatus}>
              <Route className="size-4" />
              推进状态
            </Button>
          </div>
        ))}
      </CardContent>
    </Card>
  )
}
