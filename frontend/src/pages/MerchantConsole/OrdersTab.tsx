import { Workflow } from 'lucide-react'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import type { MerchantStoreProfile } from '@/objects/merchant'

type OrdersTabProps = {
  selectedStore: MerchantStoreProfile | null
  onAcceptOrder: () => void
  onFinishCooking: () => void
}

export function OrdersTab({ selectedStore, onAcceptOrder, onFinishCooking }: OrdersTabProps) {
  const merchantPendingOrders = selectedStore?.pendingOrders ?? []
  const merchantHistoryOrders = selectedStore?.historyOrders ?? []
  const merchantOrders = [...merchantPendingOrders, ...merchantHistoryOrders]

  if (!selectedStore) {
    return (
      <Card className="border-orange-100 bg-white/95">
        <CardContent className="p-6 text-sm text-slate-600">请先选择店铺后查看接单与出餐处理内容。</CardContent>
      </Card>
    )
  }

  return (
    <Card className="border-orange-100 bg-white/95">
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Workflow className="size-5 text-orange-500" />
          接单与出餐处理
        </CardTitle>
        <CardDescription>处理待接单订单并推进履约流程</CardDescription>
      </CardHeader>
      <CardContent className="space-y-3">
        {merchantOrders.length === 0 ? (
          <p className="text-sm text-slate-500">当前店铺暂无订单。</p>
        ) : (
          merchantOrders.map((order) => (
            <div key={order.id} className="rounded-xl border border-orange-100 p-4">
              <div className="flex items-center justify-between">
                <p className="font-medium text-slate-900">订单 {order.id}</p>
                <Badge variant="outline">{order.status}</Badge>
              </div>
              <p className="mt-1 text-sm text-slate-600">总金额 {order.totalAmount} 元</p>
              <div className="mt-3 flex gap-2">
                <Button size="sm" onClick={onAcceptOrder}>
                  接单
                </Button>
                <Button size="sm" variant="outline" onClick={onFinishCooking}>
                  出餐完成
                </Button>
              </div>
            </div>
          ))
        )}
      </CardContent>
    </Card>
  )
}
