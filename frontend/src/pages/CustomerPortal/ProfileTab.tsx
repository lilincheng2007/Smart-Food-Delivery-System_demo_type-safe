import { Clock3, MapPinHouse, Phone, UserRound, Wallet } from 'lucide-react'

import { DeliveryLogoutBar } from '@/components/DeliveryLogoutBar'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import type { Merchant } from '@/objects/merchant'
import type { Order } from '@/objects/order'

type ProfileTabProps = {
  name: string
  phone: string
  defaultAddress: string
  walletBalance: number
  merchants: Merchant[]
  pendingOrders: Order[]
  historyOrders: Order[]
  onOpenRecharge: () => void
  onOpenEditProfile: () => void
  onSelectOrder: (order: Order) => void
}

export function ProfileTab({
  name,
  phone,
  defaultAddress,
  walletBalance,
  merchants,
  pendingOrders,
  historyOrders,
  onOpenRecharge,
  onOpenEditProfile,
  onSelectOrder,
}: ProfileTabProps) {
  const getMerchantName = (merchantId: string) =>
    merchants.find((merchant) => merchant.id === merchantId)?.storeName ?? '未知商家'

  return (
    <div className="space-y-6">
      <section className="grid gap-4 lg:grid-cols-4">
        <Card className="relative overflow-hidden border-border/70 bg-gradient-to-br from-primary/15 via-card/95 to-[var(--delivery-brand-blue)]/12 py-0 shadow-[0_20px_55px_rgba(15,23,42,0.08)] backdrop-blur-md dark:shadow-[0_20px_55px_rgba(0,0,0,0.4)] lg:col-span-2">
          <div className="pointer-events-none absolute -right-8 -top-10 h-32 w-32 rounded-full bg-[radial-gradient(circle,oklch(0.88_0.14_264/0.35),transparent_65%)]" />
          <CardHeader className="relative flex flex-row items-center justify-between gap-3 pb-4">
            <div className="space-y-2">
              <CardDescription>我的余额</CardDescription>
              <CardTitle className="flex items-center gap-2 text-3xl font-semibold tabular-nums tracking-tight">
                <Wallet className="size-5 text-primary" aria-hidden />
                ¥{walletBalance.toFixed(2)}
              </CardTitle>
              <p className="text-xs text-muted-foreground">充值金额将写入后端钱包流水</p>
            </div>
            <Button
              className="cursor-pointer bg-[var(--delivery-brand-blue)] text-white shadow-md transition-[filter,box-shadow] duration-200 hover:brightness-110 hover:shadow-lg"
              onClick={onOpenRecharge}
            >
              充值
            </Button>
          </CardHeader>
        </Card>
        <Card className="border-border/70 bg-card/90 py-0 shadow-sm backdrop-blur-sm">
          <CardHeader className="pb-3">
            <CardDescription>姓名</CardDescription>
            <CardTitle className="flex items-center gap-2 text-base font-semibold">
              <UserRound className="size-4 text-primary" aria-hidden />
              {name || '请完善姓名'}
            </CardTitle>
          </CardHeader>
        </Card>
        <Card className="border-border/70 bg-card/90 py-0 shadow-sm backdrop-blur-sm">
          <CardHeader className="pb-3">
            <CardDescription>联系电话</CardDescription>
            <CardTitle className="flex items-center gap-2 text-base font-semibold">
              <Phone className="size-4 text-primary" aria-hidden />
              {phone || '请完善联系电话'}
            </CardTitle>
          </CardHeader>
        </Card>
        <Card className="border-border/70 bg-card/90 py-0 shadow-sm backdrop-blur-sm lg:col-span-4">
          <CardHeader className="pb-3">
            <CardDescription>常用收货地址</CardDescription>
            <CardTitle className="flex items-start gap-2 text-base font-semibold leading-relaxed">
              <MapPinHouse className="mt-0.5 size-4 shrink-0 text-primary" aria-hidden />
              <span>{defaultAddress || '请完善默认收货地址'}</span>
            </CardTitle>
          </CardHeader>
        </Card>
      </section>

      <div className="flex justify-end">
        <Button
          variant="outline"
          className="cursor-pointer border-border/80 transition-colors hover:border-primary/40 hover:bg-primary/5"
          onClick={onOpenEditProfile}
        >
          修改资料
        </Button>
      </div>

      <section className="grid gap-4 md:grid-cols-2">
        <Card className="border-border/70 bg-card/90 py-0 shadow-sm backdrop-blur-sm">
          <CardHeader className="pb-3">
            <CardDescription>历史订单</CardDescription>
            <CardTitle className="text-2xl font-semibold tabular-nums">{historyOrders.length} 单</CardTitle>
          </CardHeader>
        </Card>
        <Card className="border-border/70 bg-card/90 py-0 shadow-sm backdrop-blur-sm">
          <CardHeader className="pb-3">
            <CardDescription>待收货</CardDescription>
            <CardTitle className="text-2xl font-semibold tabular-nums text-primary">{pendingOrders.length} 单</CardTitle>
          </CardHeader>
        </Card>
      </section>

      <Card className="border-border/70 bg-card/90 shadow-[0_18px_50px_rgba(15,23,42,0.05)] backdrop-blur-sm dark:shadow-[0_18px_50px_rgba(0,0,0,0.3)]">
        <CardHeader className="gap-1 pb-2">
          <CardTitle className="flex items-center gap-2 text-xl">
            <span className="flex size-9 items-center justify-center rounded-xl bg-primary/10 text-primary">
              <Clock3 className="size-5" aria-hidden />
            </span>
            待收货订单
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          {pendingOrders.length === 0 ? (
            <p className="text-sm text-muted-foreground">暂无待收货订单。</p>
          ) : (
            pendingOrders.map((order) => (
              <div
                key={order.id}
                className="rounded-2xl border border-border/70 bg-gradient-to-br from-card to-secondary/25 p-4 shadow-sm transition-[border-color,box-shadow] duration-200 hover:border-primary/35 hover:shadow-md"
              >
                <div className="flex items-center justify-between gap-2">
                  <p className="font-medium text-foreground">订单号：{order.id}</p>
                  <Badge variant="outline" className="border-primary/25 text-primary">
                    {order.status}
                  </Badge>
                </div>
                <p className="mt-1 text-sm text-muted-foreground">商家：{getMerchantName(order.merchantId)}</p>
                <p className="mt-1 text-sm text-muted-foreground">收货地址：{order.deliveryAddress}</p>
                <div className="mt-3 flex justify-end">
                  <Button
                    size="sm"
                    variant="outline"
                    className="cursor-pointer border-border/80 transition-colors hover:border-primary/40 hover:bg-primary/5"
                    onClick={() => onSelectOrder(order)}
                  >
                    订单详情
                  </Button>
                </div>
              </div>
            ))
          )}
        </CardContent>
      </Card>

      <Card className="border-border/70 bg-card/90 shadow-[0_18px_50px_rgba(15,23,42,0.05)] backdrop-blur-sm dark:shadow-[0_18px_50px_rgba(0,0,0,0.3)]">
        <CardHeader className="gap-1 pb-2">
          <CardTitle className="flex items-center gap-2 text-xl">
            <span className="flex size-9 items-center justify-center rounded-xl bg-primary/10 text-primary">
              <UserRound className="size-5" aria-hidden />
            </span>
            历史订单
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          {historyOrders.length === 0 ? (
            <p className="text-sm text-muted-foreground">暂无历史订单。</p>
          ) : (
            historyOrders.map((order) => (
              <div
                key={order.id}
                className="rounded-2xl border border-border/70 bg-gradient-to-br from-card to-secondary/25 p-4 shadow-sm transition-[border-color,box-shadow] duration-200 hover:border-primary/35 hover:shadow-md"
              >
                <div className="flex items-center justify-between gap-3">
                  <p className="font-medium text-foreground">订单号：{order.id}</p>
                  <Badge variant="outline" className="border-primary/25 text-primary">
                    {order.status}
                  </Badge>
                </div>
                <p className="mt-1 text-sm text-muted-foreground">商家：{getMerchantName(order.merchantId)}</p>
                <p className="mt-1 text-sm text-muted-foreground">
                  金额：{order.totalAmount} 元 · 下单时间：{order.placedAt}
                </p>
                <div className="mt-3 flex justify-end">
                  <Button
                    size="sm"
                    variant="outline"
                    className="cursor-pointer border-border/80 transition-colors hover:border-primary/40 hover:bg-primary/5"
                    onClick={() => onSelectOrder(order)}
                  >
                    订单详情
                  </Button>
                </div>
              </div>
            ))
          )}
        </CardContent>
      </Card>

      <DeliveryLogoutBar />
    </div>
  )
}
