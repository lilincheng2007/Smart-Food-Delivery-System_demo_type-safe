import { MapPinned, Route } from 'lucide-react'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import type { Order } from '@/objects/order/Order'

interface TaskListCardProps {
  orders: Order[]
  historyOrders: Order[]
  onUpdateStatus: (orderId: string) => void
}

export function TaskListCard({ orders, historyOrders, onUpdateStatus }: TaskListCardProps) {
  return (
    <div className="space-y-4">
      <Card className="border-orange-100 bg-white/95">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <MapPinned className="size-5 text-orange-500" />
            配送任务
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          {orders.length === 0 ? (
            <div className="rounded-xl border border-dashed border-orange-100 p-4 text-sm text-slate-500">当前暂无配送任务。</div>
          ) : (
            orders.map((order) => (
              <div key={order.id} className="space-y-2 rounded-xl border border-orange-100 p-4">
                <div className="flex items-center justify-between">
                  <p className="font-medium text-slate-900">订单 {order.id}</p>
                  <Badge variant="outline">{order.status}</Badge>
                </div>
                <p className="text-sm text-slate-600">顾客姓名：{order.customerName || '未填写'}</p>
                <p className="text-sm text-slate-600">联系电话：{order.customerPhone || '未填写'}</p>
                <p className="text-sm text-slate-600">配送地址：{order.deliveryAddress}</p>
                <div className="flex gap-2">
                  <Button size="sm" onClick={() => onUpdateStatus(order.id)}>
                    <Route className="size-4" />
                    已送达
                  </Button>
                </div>
              </div>
            ))
          )}
        </CardContent>
      </Card>

      <Card className="border-orange-100 bg-white/95">
        <CardHeader>
          <CardTitle>历史配送</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          {historyOrders.length === 0 ? (
            <div className="rounded-xl border border-dashed border-orange-100 p-4 text-sm text-slate-500">当前暂无历史配送。</div>
          ) : (
            historyOrders.map((order) => (
              <div key={order.id} className="space-y-2 rounded-xl border border-orange-100 p-4">
                <div className="flex items-center justify-between">
                  <p className="font-medium text-slate-900">订单 {order.id}</p>
                  <Badge variant="outline">{order.status}</Badge>
                </div>
                <p className="text-sm text-slate-600">顾客姓名：{order.customerName || '未填写'}</p>
                <p className="text-sm text-slate-600">联系电话：{order.customerPhone || '未填写'}</p>
                <p className="text-sm text-slate-600">配送地址：{order.deliveryAddress}</p>
              </div>
            ))
          )}
        </CardContent>
      </Card>
    </div>
  )
}
