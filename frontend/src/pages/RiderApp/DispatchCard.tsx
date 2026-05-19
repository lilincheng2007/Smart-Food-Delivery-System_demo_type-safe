import { Bike } from 'lucide-react'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import type { Order } from '@/objects/order/Order'

interface DispatchCardProps {
  availableOrders: Order[]
  onGrabOrder: (orderId: string) => void
}

export function DispatchCard({ availableOrders, onGrabOrder }: DispatchCardProps) {
  return (
    <Card className="border-orange-100 bg-white/95">
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Bike className="size-5 text-orange-500" />
          抢单
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-3">
        {availableOrders.length === 0 ? (
          <div className="rounded-xl border border-dashed border-orange-100 p-4 text-sm text-slate-500">当前暂无可抢订单。</div>
        ) : (
          availableOrders.map((order) => (
            <div key={order.id} className="rounded-xl border border-orange-100 p-4">
              <div className="flex items-center justify-between">
                <p className="font-medium text-slate-900">订单 {order.id}</p>
                <Badge variant="outline">{order.status}</Badge>
              </div>
              <p className="mt-1 text-sm text-slate-600">配送地址：{order.deliveryAddress}</p>
              <p className="mt-1 text-sm text-slate-600">金额：{order.totalAmount} 元</p>
              <Button className="mt-3" size="sm" onClick={() => onGrabOrder(order.id)}>
                抢这一单
              </Button>
            </div>
          ))
        )}
      </CardContent>
    </Card>
  )
}
