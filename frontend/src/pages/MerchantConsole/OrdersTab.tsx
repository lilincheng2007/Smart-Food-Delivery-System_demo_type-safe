import { useState } from 'react'
import { Workflow } from 'lucide-react'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import type { Order } from '@/objects/order/Order'
import type { MerchantStoreProfile } from '@/objects/merchant/MerchantStoreProfile'

type OrdersTabProps = {
  selectedStore: MerchantStoreProfile | null
  onFinishCooking: (orderId: string) => void
}

export function OrdersTab({ selectedStore, onFinishCooking }: OrdersTabProps) {
  const merchantPendingOrders = selectedStore?.pendingOrders ?? []
  const merchantHistoryOrders = selectedStore?.historyOrders ?? []
  const activeCookingOrders = merchantPendingOrders.filter((order) => order.status === '制作中')
  const historyOrders = [...merchantPendingOrders.filter((order) => order.status !== '制作中'), ...merchantHistoryOrders]
  const [selectedOrder, setSelectedOrder] = useState<Order | null>(null)

  if (!selectedStore) {
    return (
      <Card className="border-orange-100 bg-white/95">
        <CardContent className="p-6 text-sm text-slate-600">请先选择店铺后查看出餐处理内容。</CardContent>
      </Card>
    )
  }

  return (
    <div className="space-y-4">
      <Card className="border-orange-100 bg-white/95">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Workflow className="size-5 text-orange-500" />
            出餐处理
          </CardTitle>
          <CardDescription>订单创建后自动接单，这里只处理制作中的出餐完成操作</CardDescription>
        </CardHeader>
        <CardContent className="space-y-3">
          {activeCookingOrders.length === 0 ? (
            <p className="text-sm text-slate-500">当前暂无待出餐订单。</p>
          ) : (
            activeCookingOrders.map((order) => (
              <div
                key={order.id}
                className="cursor-pointer rounded-xl border border-orange-100 p-4 transition-colors hover:bg-orange-50/60"
                onClick={() => setSelectedOrder(order)}
              >
                <div className="flex items-center justify-between">
                  <p className="font-medium text-slate-900">订单 {order.id}</p>
                  <Badge variant="outline">{order.status}</Badge>
                </div>
                <p className="mt-1 text-sm text-slate-600">总金额 {order.totalAmount} 元</p>
                <div className="mt-3 flex gap-2">
                  <Button
                    size="sm"
                    variant="outline"
                    disabled={order.status !== '制作中'}
                    onClick={(event) => {
                      event.stopPropagation()
                      onFinishCooking(order.id)
                    }}
                  >
                    出餐完成
                  </Button>
                </div>
              </div>
            ))
          )}
        </CardContent>
      </Card>

      <Card className="border-orange-100 bg-white/95">
        <CardHeader>
          <CardTitle>历史订单</CardTitle>
          <CardDescription>已出餐、配送中、已完成或已取消的订单会显示在这里</CardDescription>
        </CardHeader>
        <CardContent className="space-y-3">
          {historyOrders.length === 0 ? (
            <p className="text-sm text-slate-500">当前暂无历史订单。</p>
          ) : (
            historyOrders.map((order) => (
              <div
                key={order.id}
                className="cursor-pointer rounded-xl border border-orange-100 p-4 transition-colors hover:bg-orange-50/60"
                onClick={() => setSelectedOrder(order)}
              >
                <div className="flex items-center justify-between">
                  <p className="font-medium text-slate-900">订单 {order.id}</p>
                  <Badge variant="outline">{order.status}</Badge>
                </div>
                <p className="mt-1 text-sm text-slate-600">总金额 {order.totalAmount} 元</p>
              </div>
            ))
          )}
        </CardContent>
      </Card>

      <Dialog open={selectedOrder !== null} onOpenChange={(open) => !open && setSelectedOrder(null)}>
        <DialogContent className="max-w-md rounded-2xl border border-orange-100 bg-white p-6">
          <DialogHeader>
            <DialogTitle>订单详情</DialogTitle>
            <DialogDescription>{selectedOrder ? `订单号：${selectedOrder.id}` : '查看订单商品信息'}</DialogDescription>
          </DialogHeader>
          {selectedOrder ? (
            <div className="space-y-3">
              <div className="rounded-xl bg-orange-50 px-3 py-2 text-sm text-slate-700">
                订单总金额：
                <span className="ml-1 font-semibold text-orange-600">¥{selectedOrder.totalAmount.toFixed(2)}</span>
              </div>
              <div className="space-y-2">
                {selectedOrder.items.map((item) => (
                  <div key={`${selectedOrder.id}-${item.productId}`} className="rounded-lg border border-orange-100 px-3 py-2">
                    <p className="font-medium text-slate-900">{item.name}</p>
                    <p className="mt-1 text-sm text-slate-600">数量：{item.quantity}</p>
                    <p className="text-sm text-slate-600">金额：¥{(item.unitPrice * item.quantity).toFixed(2)}</p>
                  </div>
                ))}
              </div>
            </div>
          ) : null}
          <DialogFooter>
            <Button variant="outline" onClick={() => setSelectedOrder(null)}>
              关闭
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
