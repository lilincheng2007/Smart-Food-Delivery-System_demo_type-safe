import { Clock3, UserRound, Wallet } from 'lucide-react'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import type { Order } from '@/objects/order'

type ProfileTabProps = {
  walletBalance: number
  pendingOrders: Order[]
  historyOrders: Order[]
  onOpenRecharge: () => void
  onSelectOrder: (order: Order) => void
}

export function ProfileTab({
  walletBalance,
  pendingOrders,
  historyOrders,
  onOpenRecharge,
  onSelectOrder,
}: ProfileTabProps) {
  const allOrders = [...pendingOrders, ...historyOrders]

  return (
    <div className="space-y-4">
      <section className="grid gap-4 md:grid-cols-3">
        <Card className="border-orange-100 bg-white/95 py-0">
          <CardHeader className="pb-2">
            <CardDescription>我的余额</CardDescription>
            <CardTitle className="flex items-center gap-2">
              <Wallet className="size-4 text-orange-500" />
              {walletBalance.toFixed(2)} 元
            </CardTitle>
          </CardHeader>
        </Card>
        <Card className="border-orange-100 bg-white/95 py-0">
          <CardHeader className="pb-2">
            <CardDescription>历史订单</CardDescription>
            <CardTitle>{historyOrders.length} 单</CardTitle>
          </CardHeader>
        </Card>
        <Card className="border-orange-100 bg-white/95 py-0">
          <CardHeader className="pb-2">
            <CardDescription>待收货</CardDescription>
            <CardTitle>{pendingOrders.length} 单</CardTitle>
          </CardHeader>
        </Card>
      </section>

      <Card className="border-orange-100 bg-white/95">
        <CardContent className="flex items-center justify-between gap-3 p-4">
          <p className="text-sm text-slate-700">余额不足可先充值，再返回购物车结算。</p>
          <Button onClick={onOpenRecharge}>充值</Button>
        </CardContent>
      </Card>

      <Card className="border-orange-100 bg-white/95">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Clock3 className="size-5 text-orange-500" />
            待收货订单
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          {pendingOrders.length === 0 ? (
            <p className="text-sm text-slate-500">暂无待收货订单。</p>
          ) : (
            pendingOrders.map((order) => (
              <button
                key={order.id}
                type="button"
                className="w-full rounded-xl border border-orange-100 p-4 text-left transition-colors hover:bg-orange-50/60"
                onClick={() => onSelectOrder(order)}
              >
                <div className="flex items-center justify-between">
                  <p className="font-medium text-slate-900">订单号：{order.id}</p>
                  <Badge variant="outline">{order.status}</Badge>
                </div>
                <p className="mt-1 text-sm text-slate-600">收货地址：{order.deliveryAddress}</p>
              </button>
            ))
          )}
        </CardContent>
      </Card>

      <Card className="border-orange-100 bg-white/95">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <UserRound className="size-5 text-orange-500" />
            历史订单
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          {allOrders.map((order) => (
            <button
              key={order.id}
              type="button"
              className="w-full rounded-xl border border-orange-100 p-4 text-left transition-colors hover:bg-orange-50/60"
              onClick={() => onSelectOrder(order)}
            >
              <div className="flex items-center justify-between gap-3">
                <p className="font-medium text-slate-900">订单号：{order.id}</p>
                <Badge variant="outline">{order.status}</Badge>
              </div>
              <p className="mt-1 text-sm text-slate-600">
                金额：{order.totalAmount} 元 · 下单时间：{order.placedAt}
              </p>
            </button>
          ))}
        </CardContent>
      </Card>
    </div>
  )
}
