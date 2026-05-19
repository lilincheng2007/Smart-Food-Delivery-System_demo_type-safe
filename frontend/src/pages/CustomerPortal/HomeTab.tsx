import { ArrowRight, Sparkles, Store } from 'lucide-react'
import { useNavigate } from 'react-router-dom'

import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { resolveApiMediaUrl } from '@/lib/api-media-url'
import { cn } from '@/lib/utils'
import type { Merchant } from '@/objects/merchant/Merchant'

type HomeTabProps = {
  merchants: Merchant[]
}

export function HomeTab({ merchants }: HomeTabProps) {
  const navigate = useNavigate()

  return (
    <div className="space-y-6">
      <div className="relative overflow-hidden rounded-3xl border border-border/60 bg-gradient-to-br from-card/95 via-card/90 to-secondary/40 p-6 shadow-[0_22px_60px_rgba(15,23,42,0.07)] backdrop-blur-md sm:p-8 dark:from-card/80 dark:to-secondary/20">
        <div className="pointer-events-none absolute -right-10 -top-12 h-40 w-40 rounded-full bg-[radial-gradient(circle,oklch(0.88_0.14_264/0.35),transparent_62%)]" />
        <div className="pointer-events-none absolute -bottom-16 left-8 h-44 w-44 rounded-full bg-[radial-gradient(circle,oklch(0.9_0.1_12/0.45),transparent_65%)]" />
        <div className="relative flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
          <div className="space-y-2">
            <p className="inline-flex items-center gap-2 text-xs font-semibold uppercase tracking-[0.2em] text-primary">
              <Sparkles className="size-3.5" aria-hidden />
              今日精选
            </p>
            <h2 className="text-balance text-2xl font-semibold tracking-tight text-foreground sm:text-3xl">
              发现附近好味道
            </h2>
            <p className="max-w-xl text-pretty text-sm leading-relaxed text-muted-foreground">
              点击商家卡片进入专属点餐页，在店内选菜、查看本店购物车并一键结算。
            </p>
          </div>
          <div className="flex items-center gap-2 rounded-2xl border border-border/60 bg-background/70 px-4 py-3 text-sm text-muted-foreground shadow-sm backdrop-blur-sm">
            <span className="font-semibold text-foreground">{merchants.length}</span>
            家合作门店在线
            <ArrowRight className="size-4 text-primary" aria-hidden />
          </div>
        </div>
      </div>

      <Card className="border-border/70 bg-card/90 shadow-[0_18px_50px_rgba(15,23,42,0.05)] backdrop-blur-sm dark:shadow-[0_18px_50px_rgba(0,0,0,0.3)]">
        <CardHeader className="gap-1 pb-2">
          <CardTitle className="flex items-center gap-2 text-xl">
            <span className="flex size-9 items-center justify-center rounded-xl bg-primary/10 text-primary">
              <Store className="size-5" aria-hidden />
            </span>
            商家列表
          </CardTitle>
          <CardDescription>点击商家进入该店的点餐与结算页面</CardDescription>
        </CardHeader>
        <CardContent className="grid gap-3 md:grid-cols-2">
          {merchants.map((merchant) => (
            <button
              key={merchant.id}
              type="button"
              className={cn(
                'group cursor-pointer rounded-2xl border border-border/70 bg-card/90 p-4 text-left shadow-sm transition-[border-color,background-color,box-shadow] duration-200 hover:border-primary/35 hover:shadow-md',
              )}
              onClick={() => navigate(`/delivery/customer/m/${encodeURIComponent(merchant.id)}`)}
            >
              {merchant.imageUrl?.trim() ? (
                <div className="mb-3 aspect-square w-full overflow-hidden rounded-xl">
                  <img
                    src={resolveApiMediaUrl(merchant.imageUrl)}
                    alt={`${merchant.storeName} 店铺`}
                    className="size-full object-cover"
                  />
                </div>
              ) : (
                <div className="mb-3 flex aspect-square w-full items-center justify-center rounded-xl bg-muted text-xs text-muted-foreground">
                  暂无店铺图
                </div>
              )}
              <div className="flex items-start justify-between gap-3">
                <div className="space-y-1">
                  <p className="font-semibold text-foreground">{merchant.storeName}</p>
                  <p className="text-sm leading-relaxed text-muted-foreground">{merchant.address}</p>
                </div>
                <Badge variant="outline" className="shrink-0 border-primary/25 text-primary">
                  {merchant.category}
                </Badge>
              </div>
              <p className="mt-3 text-xs font-medium text-primary opacity-0 transition-opacity duration-200 group-hover:opacity-100">
                进入点餐
              </p>
            </button>
          ))}
        </CardContent>
      </Card>
    </div>
  )
}
