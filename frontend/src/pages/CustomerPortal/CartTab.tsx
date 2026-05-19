import type { CSSProperties } from 'react'
import { Minus, Plus, ShoppingCart } from 'lucide-react'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import type { Merchant } from '@/objects/merchant/Merchant'
import type { Product } from '@/objects/merchant/Product'
import type { MerchantId } from '@/objects/shared/ids'
import type { ProductId } from '@/objects/shared/ids'
import type { CartLine } from '@/stores/pages/use-customer-portal-store'

type CartTabProps = {
  merchants: Merchant[]
  products: Product[]
  cartLines: CartLine[]
  onChangeQuantity: (merchantId: MerchantId, productId: ProductId, nextQuantity: number) => void
  onCheckout: () => void
}

/** 贴近视口底边，并保留底部安全区（刘海屏 Home 条等） */
const cartDockBottomStyle: CSSProperties = {
  bottom: 'max(0.75rem, env(safe-area-inset-bottom, 0px))',
}

export function CartTab({ merchants, products, cartLines, onChangeQuantity, onCheckout }: CartTabProps) {
  const cartGroupedByMerchant = cartLines.reduce<Record<string, CartLine[]>>((grouped, line) => {
    if (!grouped[line.merchantId]) {
      grouped[line.merchantId] = []
    }
    grouped[line.merchantId].push(line)
    return grouped
  }, {})

  const cartTotal = cartLines.reduce((sum, line) => {
    const product = products.find((item) => item.id === line.productId)
    return sum + (product ? product.price * line.quantity : 0)
  }, 0)

  return (
    <div className="relative pb-40 sm:pb-44">
      <div className="space-y-6">
        <Card className="border-border/70 bg-gradient-to-br from-primary/12 via-card/95 to-[var(--delivery-brand-blue)]/10 py-0 shadow-[0_18px_50px_rgba(15,23,42,0.06)] backdrop-blur-sm dark:shadow-[0_18px_50px_rgba(0,0,0,0.35)]">
          <CardHeader className="flex flex-row items-end justify-between gap-4 pb-4 sm:pb-5">
            <div className="space-y-1">
              <CardDescription>购物车商品件数</CardDescription>
              <CardTitle className="text-3xl font-semibold tabular-nums tracking-tight text-foreground">
                {cartLines.reduce((sum, line) => sum + line.quantity, 0)}
                <span className="ml-1 text-base font-medium text-muted-foreground">件</span>
              </CardTitle>
            </div>
            <div className="rounded-2xl border border-border/60 bg-background/80 px-4 py-2 text-right text-xs text-muted-foreground shadow-inner backdrop-blur-sm">
              <p className="font-semibold text-foreground">实时计价</p>
              <p>与后端菜品价格一致</p>
            </div>
          </CardHeader>
        </Card>

        <Card className="border-border/70 bg-card/90 shadow-[0_18px_50px_rgba(15,23,42,0.05)] backdrop-blur-sm dark:shadow-[0_18px_50px_rgba(0,0,0,0.3)]">
          <CardHeader className="gap-1 pb-2">
            <CardTitle className="flex items-center gap-2 text-xl">
              <span className="flex size-9 items-center justify-center rounded-xl bg-primary/10 text-primary">
                <ShoppingCart className="size-5" aria-hidden />
              </span>
              购物车
            </CardTitle>
            <CardDescription>按商家分组展示已选菜品；点击「去结算」将打开确认订单页后再扣款</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            {Object.keys(cartGroupedByMerchant).length === 0 ? (
              <div className="rounded-2xl border border-dashed border-primary/35 bg-gradient-to-br from-secondary/40 to-background/60 p-10 text-center">
                <p className="text-sm font-medium text-foreground">购物车还是空的</p>
                <p className="mt-2 text-sm text-muted-foreground">去首页点击商家进入点餐页选菜；在多家店加购后可回到此处合并结算。</p>
              </div>
            ) : (
              Object.entries(cartGroupedByMerchant).map(([merchantId, lines]) => {
                const merchant = merchants.find((item) => item.id === merchantId)
                if (!merchant) {
                  return null
                }

                return (
                  <div
                    key={merchantId}
                    className="rounded-2xl border border-border/70 bg-gradient-to-br from-card to-secondary/30 p-4 shadow-sm"
                  >
                    <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
                      <p className="text-lg font-semibold text-foreground">{merchant.storeName}</p>
                      <Badge variant="outline" className="border-primary/25 text-primary">
                        {merchant.category}
                      </Badge>
                    </div>
                    <div className="space-y-3">
                      {lines.map((line) => {
                        const product = products.find((item) => item.id === line.productId)
                        if (!product) {
                          return null
                        }

                        return (
                          <div
                            key={line.productId}
                            className="flex items-center justify-between gap-4 rounded-xl border border-border/50 bg-background/70 p-3 shadow-sm backdrop-blur-sm"
                          >
                            <div className="space-y-1">
                              <p className="font-medium text-foreground">{product.name}</p>
                              <p className="text-sm text-muted-foreground">单价 ¥{product.price.toFixed(1)}</p>
                            </div>
                            <div className="flex items-center gap-2">
                              <Button
                                size="icon"
                                variant="outline"
                                className="size-8 cursor-pointer border-border/80 transition-colors hover:border-primary/40 hover:bg-primary/5"
                                onClick={() => onChangeQuantity(line.merchantId, line.productId, line.quantity - 1)}
                              >
                                <Minus className="size-4" />
                              </Button>
                              <span className="w-8 text-center text-sm font-semibold tabular-nums">{line.quantity}</span>
                              <Button
                                size="icon"
                                variant="outline"
                                className="size-8 cursor-pointer border-border/80 transition-colors hover:border-primary/40 hover:bg-primary/5"
                                onClick={() => onChangeQuantity(line.merchantId, line.productId, line.quantity + 1)}
                              >
                                <Plus className="size-4" />
                              </Button>
                            </div>
                          </div>
                        )
                      })}
                    </div>
                  </div>
                )
              })
            )}
          </CardContent>
        </Card>
      </div>

      <div className="pointer-events-none fixed inset-x-0 z-40 px-5 sm:px-10" style={cartDockBottomStyle}>
        <div className="pointer-events-auto mx-auto max-w-6xl overflow-hidden rounded-2xl border border-border/70 bg-card/95 shadow-[0_-12px_40px_rgba(15,23,42,0.12)] backdrop-blur-md dark:border-border/80 dark:bg-card/90 dark:shadow-[0_-12px_40px_rgba(0,0,0,0.45)]">
          <div className="flex flex-col gap-3 bg-gradient-to-br from-primary/8 via-card to-[var(--delivery-brand-blue)]/8 p-4 sm:flex-row sm:items-center sm:justify-between sm:p-5">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.18em] text-muted-foreground">应付合计</p>
              <p className="mt-1 text-sm text-muted-foreground">
                已含购物车全部商家商品
                <span className="mx-1 text-foreground">·</span>
                <span className="font-medium text-foreground">{cartLines.length}</span> 个 SKU
              </p>
              <p className="mt-2 text-3xl font-semibold tabular-nums text-primary">¥{cartTotal.toFixed(1)}</p>
            </div>
            <Button
              className="h-11 min-w-[8.5rem] cursor-pointer bg-gradient-to-r from-primary to-[oklch(0.62_0.18_45)] text-base font-semibold text-primary-foreground shadow-[0_16px_40px_rgba(225,29,72,0.35)] transition-[filter,box-shadow] duration-200 hover:brightness-110 hover:shadow-[0_20px_50px_rgba(225,29,72,0.42)] disabled:cursor-not-allowed disabled:opacity-50 dark:to-[oklch(0.68_0.14_45)]"
              onClick={onCheckout}
              disabled={cartLines.length === 0}
            >
              去结算
            </Button>
          </div>
        </div>
      </div>
    </div>
  )
}
