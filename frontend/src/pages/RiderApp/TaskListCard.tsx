import { AlertTriangle, CheckCircle2, MapPinned, Route, ShieldCheck } from 'lucide-react'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import type { Order } from '@/objects/order/Order'
import type { RiderDeliveryStatus } from '@/objects/rider/RiderDeliveryStatus'

interface TaskListCardProps {
  orders: Order[]
  historyOrders: Order[]
  deliveryStatuses: RiderDeliveryStatus[]
  onUpdateStatus: (orderId: string) => void
  onUseTimeoutCard: (orderId: string) => void
}

function statusFor(orderId: string, statuses: RiderDeliveryStatus[]) {
  return statuses.find((item) => item.orderId === orderId) ?? null
}

function formatTime(value?: string) {
  if (!value) return '暂无'
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

function timeoutBadge(status: RiderDeliveryStatus | null) {
  if (!status?.completedAt) {
    return null
  }
  if (!status.wasTimeout) {
    return <Badge className="bg-emerald-50 text-emerald-700 hover:bg-emerald-50">准时送达</Badge>
  }
  if (status.timeoutExempted) {
    return <Badge className="bg-emerald-50 text-emerald-700 hover:bg-emerald-50">超时已免责</Badge>
  }
  return <Badge className="bg-rose-50 text-rose-700 hover:bg-rose-50">超时未免责</Badge>
}

export function TaskListCard({ orders, historyOrders, deliveryStatuses, onUpdateStatus, onUseTimeoutCard }: TaskListCardProps) {
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
            orders.map((order) => {
              const deliveryStatus = statusFor(order.id, deliveryStatuses)
              return (
                <div key={order.id} className="space-y-2 rounded-xl border border-orange-100 p-4">
                  <div className="flex items-center justify-between gap-2">
                    <p className="font-medium text-slate-900">订单 {order.id}</p>
                    <Badge variant="outline">{order.status}</Badge>
                  </div>
                  <p className="text-sm text-slate-600">顾客姓名：{order.customerName || '未填写'}</p>
                  <p className="text-sm text-slate-600">联系电话：{order.customerPhone || '未填写'}</p>
                  <p className="text-sm text-slate-600">配送地址：{order.deliveryAddress}</p>
                  {deliveryStatus ? (
                    <div className="rounded-xl bg-orange-50 px-3 py-2 text-xs text-slate-600">
                      接单时间：{formatTime(deliveryStatus.assignedAt)} · 超时截止：{formatTime(deliveryStatus.deadlineAt)}
                    </div>
                  ) : null}
                  <div className="flex gap-2">
                    <Button size="sm" className="cursor-pointer" onClick={() => onUpdateStatus(order.id)}>
                      <Route className="size-4" />
                      已送达
                    </Button>
                  </div>
                </div>
              )
            })
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
            historyOrders.map((order) => {
              const deliveryStatus = statusFor(order.id, deliveryStatuses)
              return (
                <div key={order.id} className="space-y-2 rounded-xl border border-orange-100 p-4">
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <p className="font-medium text-slate-900">订单 {order.id}</p>
                    <div className="flex flex-wrap gap-2">
                      <Badge variant="outline">{order.status}</Badge>
                      {timeoutBadge(deliveryStatus)}
                    </div>
                  </div>
                  <p className="text-sm text-slate-600">顾客姓名：{order.customerName || '未填写'}</p>
                  <p className="text-sm text-slate-600">联系电话：{order.customerPhone || '未填写'}</p>
                  <p className="text-sm text-slate-600">配送地址：{order.deliveryAddress}</p>
                  {deliveryStatus ? (
                    <div className="rounded-xl bg-slate-50 px-3 py-2 text-xs text-slate-600">
                      <p>接单时间：{formatTime(deliveryStatus.assignedAt)}</p>
                      <p>送达时间：{formatTime(deliveryStatus.completedAt)}</p>
                      {deliveryStatus.wasTimeout ? (
                        <p className="mt-1 flex items-center gap-1 text-rose-600">
                          <AlertTriangle className="size-3" />
                          超时 {Math.ceil(deliveryStatus.overtimeSeconds / 60)} 分钟
                        </p>
                      ) : (
                        <p className="mt-1 flex items-center gap-1 text-emerald-600">
                          <CheckCircle2 className="size-3" />
                          本单准时送达
                        </p>
                      )}
                    </div>
                  ) : null}
                  {deliveryStatus?.canUseTimeoutCard ? (
                    <Button size="sm" variant="outline" className="cursor-pointer border-emerald-200 text-emerald-700 hover:bg-emerald-50" onClick={() => onUseTimeoutCard(order.id)}>
                      <ShieldCheck className="size-4" />
                      使用免责卡
                    </Button>
                  ) : null}
                </div>
              )
            })
          )}
        </CardContent>
      </Card>
    </div>
  )
}
