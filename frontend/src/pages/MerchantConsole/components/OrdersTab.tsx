import { useState, type ReactNode } from 'react'
import { Bike, CheckCircle2, ChefHat, Clock3, Workflow, XCircle } from 'lucide-react'

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
import { OrderStatuses, type OrderId } from '@/objects/shared/ids'

type OrdersTabProps = {
  selectedStore: MerchantStoreProfile | null
  onAcceptOrder: (orderId: OrderId) => void
  onRejectOrder: (orderId: OrderId) => void
  onFinishCooking: (orderId: OrderId) => void
}

function orderItemSummary(order: Order): string {
  return order.items.map((item) => `${item.name}×${item.quantity}`).join('、')
}

function statusHint(order: Order): string {
  if (order.status === OrderStatuses.waitingForMerchantAcceptance) {
    return '顾客已付款，等待商家确认是否接单。'
  }
  if (order.status === OrderStatuses.cooking) {
    return '已接单，后厨制作完成后可标记出餐。'
  }
  if (order.status === OrderStatuses.waitingForRiderAcceptance) {
    return '已出餐，正在等待骑手接单取餐。'
  }
  if (order.status === OrderStatuses.delivering) {
    return '骑手已接单，餐品正在配送途中。'
  }
  if (order.status === OrderStatuses.canceled) {
    return '订单已取消，款项已按规则退回顾客钱包。'
  }
  return '订单已进入历史记录。'
}

function OrderCard({ order, children, onOpen }: { order: Order; children?: ReactNode; onOpen: () => void }) {
  return (
    <div
      className="cursor-pointer rounded-xl border border-orange-100 bg-white/90 p-4 shadow-sm transition-all hover:-translate-y-0.5 hover:border-orange-200 hover:bg-orange-50/60 hover:shadow-md"
      onClick={onOpen}
    >
      <div className="flex items-center justify-between gap-3">
        <p className="truncate font-medium text-slate-900">订单 {order.id}</p>
        <Badge variant="outline" className="shrink-0 border-orange-200 bg-orange-50 text-orange-700">
          {order.status}
        </Badge>
      </div>
      <p className="mt-1 text-sm text-slate-600">{statusHint(order)}</p>
      <p className="mt-2 line-clamp-2 text-sm text-slate-500">{orderItemSummary(order)}</p>
      <div className="mt-3 flex flex-wrap items-center justify-between gap-2 text-sm">
        <span className="text-slate-500">下单时间 {order.placedAt}</span>
        <span className="font-semibold text-orange-600">¥{order.payableAmount.toFixed(2)}</span>
      </div>
      {children ? <div className="mt-3 flex flex-wrap gap-2">{children}</div> : null}
    </div>
  )
}

export function OrdersTab({ selectedStore, onAcceptOrder, onRejectOrder, onFinishCooking }: OrdersTabProps) {
  const merchantPendingOrders = selectedStore?.pendingOrders ?? []
  const merchantHistoryOrders = selectedStore?.historyOrders ?? []
  const awaitingMerchantOrders = merchantPendingOrders.filter(
    (order) => order.status === OrderStatuses.waitingForMerchantAcceptance,
  )
  const activeCookingOrders = merchantPendingOrders.filter((order) => order.status === OrderStatuses.cooking)
  const fulfillmentOrders = merchantPendingOrders.filter(
    (order) => order.status === OrderStatuses.waitingForRiderAcceptance || order.status === OrderStatuses.delivering,
  )
  const [selectedOrder, setSelectedOrder] = useState<Order | null>(null)

  if (!selectedStore) {
    return (
      <Card className="border-orange-100 bg-white/95">
        <CardContent className="p-6 text-sm text-slate-600">请先选择店铺后查看订单处理内容。</CardContent>
      </Card>
    )
  }

  return (
    <div className="space-y-4">
      <Card className="overflow-hidden border-orange-100 bg-gradient-to-br from-white via-orange-50/70 to-rose-50/60">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Workflow className="size-5 text-orange-500" />
            待商家接单
          </CardTitle>
          <CardDescription>顾客已完成付款，请及时确认接单；拒收会取消订单并退款。</CardDescription>
        </CardHeader>
        <CardContent className="space-y-3">
          {awaitingMerchantOrders.length === 0 ? (
            <p className="text-sm text-slate-500">当前暂无待确认订单。</p>
          ) : (
            awaitingMerchantOrders.map((order) => (
              <OrderCard key={order.id} order={order} onOpen={() => setSelectedOrder(order)}>
                <Button
                  size="sm"
                  className="bg-gradient-to-r from-orange-500 to-rose-500 text-white shadow-sm hover:from-orange-600 hover:to-rose-600"
                  onClick={(event) => {
                    event.stopPropagation()
                    onAcceptOrder(order.id)
                  }}
                >
                  <CheckCircle2 className="mr-1 size-4" />
                  接单
                </Button>
                <Button
                  size="sm"
                  variant="outline"
                  className="border-rose-200 text-rose-600 hover:bg-rose-50 hover:text-rose-700"
                  onClick={(event) => {
                    event.stopPropagation()
                    onRejectOrder(order.id)
                  }}
                >
                  <XCircle className="mr-1 size-4" />
                  拒收
                </Button>
              </OrderCard>
            ))
          )}
        </CardContent>
      </Card>

      <Card className="border-orange-100 bg-white/95">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <ChefHat className="size-5 text-orange-500" />
            制作中
          </CardTitle>
          <CardDescription>商家接单后进入制作中；出餐完成后会变为待骑手接单。</CardDescription>
        </CardHeader>
        <CardContent className="space-y-3">
          {activeCookingOrders.length === 0 ? (
            <p className="text-sm text-slate-500">当前暂无待出餐订单。</p>
          ) : (
            activeCookingOrders.map((order) => (
              <OrderCard key={order.id} order={order} onOpen={() => setSelectedOrder(order)}>
                <Button
                  size="sm"
                  variant="outline"
                  disabled={order.status !== OrderStatuses.cooking}
                  onClick={(event) => {
                    event.stopPropagation()
                    onFinishCooking(order.id)
                  }}
                >
                  出餐完成
                </Button>
              </OrderCard>
            ))
          )}
        </CardContent>
      </Card>

      <Card className="border-orange-100 bg-white/95">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Bike className="size-5 text-blue-500" />
            履约中
          </CardTitle>
          <CardDescription>已出餐等待骑手接单，或骑手正在配送中的订单。</CardDescription>
        </CardHeader>
        <CardContent className="space-y-3">
          {fulfillmentOrders.length === 0 ? (
            <p className="text-sm text-slate-500">当前暂无履约中订单。</p>
          ) : (
            fulfillmentOrders.map((order) => <OrderCard key={order.id} order={order} onOpen={() => setSelectedOrder(order)} />)
          )}
        </CardContent>
      </Card>

      <Card className="border-orange-100 bg-white/95">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Clock3 className="size-5 text-slate-500" />
            历史订单
          </CardTitle>
          <CardDescription>已取消、已送达和已完成的订单会显示在这里。</CardDescription>
        </CardHeader>
        <CardContent className="space-y-3">
          {merchantHistoryOrders.length === 0 ? (
            <p className="text-sm text-slate-500">当前暂无历史订单。</p>
          ) : (
            merchantHistoryOrders.map((order) => <OrderCard key={order.id} order={order} onOpen={() => setSelectedOrder(order)} />)
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
                当前状态：<span className="font-semibold text-orange-600">{selectedOrder.status}</span>
                <p className="mt-1 text-xs text-slate-500">{statusHint(selectedOrder)}</p>
              </div>
              <div className="rounded-xl bg-orange-50 px-3 py-2 text-sm text-slate-700">
                实付金额：
                <span className="ml-1 font-semibold text-orange-600">¥{selectedOrder.payableAmount.toFixed(2)}</span>
                {selectedOrder.discountAmount > 0 ? (
                  <span className="ml-2 text-xs text-green-600">已优惠 ¥{selectedOrder.discountAmount.toFixed(2)}</span>
                ) : null}
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
