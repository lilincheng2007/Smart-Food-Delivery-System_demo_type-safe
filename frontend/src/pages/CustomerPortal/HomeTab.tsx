import { ArrowRight, ShoppingCart, Sparkles, Store } from 'lucide-react'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { resolveApiMediaUrl } from '@/lib/api-media-url'
import { cn } from '@/lib/utils'
import type { Merchant, Product } from '@/objects/merchant'
import type { MerchantId, ProductId } from '@/objects/shared'

type HomeTabProps = {
  merchants: Merchant[]
  products: Product[]
  selectedMerchantId: MerchantId
  onSelectMerchant: (merchantId: MerchantId) => void
  onAddProductToCart: (merchantId: MerchantId, productId: ProductId) => void
}

export function HomeTab({
  merchants,
  products,
  selectedMerchantId,
  onSelectMerchant,
  onAddProductToCart,
}: HomeTabProps) {
  const selectedMerchant = merchants.find((merchant) => merchant.id === selectedMerchantId) ?? null
  const selectedMerchantProducts = products.filter(
    (product) => product.merchantId === selectedMerchantId && product.listingStatus === '上架',
  )

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
              轻触卡片即可切换商家，菜品实时同步购物车，结算链路直连网关演示环境。
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
          <CardDescription>点击商家卡片后浏览对应上架菜品</CardDescription>
        </CardHeader>
        <CardContent className="grid gap-3 md:grid-cols-2">
          {merchants.map((merchant) => (
            <button
              key={merchant.id}
              type="button"
              className={cn(
                'group cursor-pointer rounded-2xl border p-4 text-left shadow-sm transition-[border-color,background-color,box-shadow] duration-200',
                selectedMerchantId === merchant.id
                  ? 'border-primary/50 bg-gradient-to-br from-primary/10 via-card to-card ring-2 ring-primary/25'
                  : 'border-border/70 bg-card/90 hover:border-primary/35 hover:shadow-md',
              )}
              onClick={() => onSelectMerchant(merchant.id)}
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
                查看菜单
              </p>
            </button>
          ))}
        </CardContent>
      </Card>

      <Card className="border-border/70 bg-card/90 shadow-[0_18px_50px_rgba(15,23,42,0.05)] backdrop-blur-sm dark:shadow-[0_18px_50px_rgba(0,0,0,0.3)]">
        <CardHeader className="gap-1 pb-2">
          <CardTitle className="text-xl">商家菜品</CardTitle>
          <CardDescription>
            {selectedMerchant ? `${selectedMerchant.storeName} 的可选菜品` : '请选择商家后开始点餐'}
          </CardDescription>
        </CardHeader>
        <CardContent className="grid gap-4 md:grid-cols-2">
          {selectedMerchantProducts.map((product) => (
            <div
              key={product.id}
              className="relative overflow-hidden rounded-2xl border border-border/70 bg-gradient-to-br from-card to-secondary/25 p-4 shadow-sm transition-[box-shadow,border-color] duration-200 hover:border-primary/35 hover:shadow-md"
            >
              <div className="absolute inset-y-3 left-0 w-1 rounded-full bg-gradient-to-b from-primary to-[var(--delivery-brand-blue)]" />
              <div className="space-y-3 pl-3">
                <div className="space-y-1">
                  <p className="font-semibold text-foreground">{product.name}</p>
                  <p className="text-sm leading-relaxed text-muted-foreground">{product.description}</p>
                </div>
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <span className="text-lg font-semibold tabular-nums text-primary">¥{product.price.toFixed(1)}</span>
                  <Button
                    size="sm"
                    className="cursor-pointer bg-[var(--delivery-brand-blue)] text-white shadow-md transition-[filter,box-shadow] duration-200 hover:brightness-110 hover:shadow-lg"
                    onClick={() => onAddProductToCart(product.merchantId, product.id)}
                  >
                    <ShoppingCart className="size-4" />
                    加入购物车
                  </Button>
                </div>
              </div>
            </div>
          ))}
        </CardContent>
      </Card>
    </div>
  )
}
