import { ChevronRight, Sparkles, Store } from 'lucide-react'
import { useNavigate } from 'react-router-dom'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { resolveApiMediaUrl } from '@/lib/api-media-url'
import type { AISearchResponse } from '@/objects/ai/AISearchResponse'
import type { Merchant } from '@/objects/merchant/Merchant'
import type { Product } from '@/objects/merchant/Product'

type AISearchResultsProps = {
  result: AISearchResponse
  merchants: Merchant[]
  products: Product[]
  onRetry: () => void
}

export function AISearchResults({ result, merchants, products, onRetry }: AISearchResultsProps) {
  const navigate = useNavigate()

  const getMerchantImageUrl = (merchantId: string): string | null => {
    const merchant = merchants.find((m) => m.id === merchantId)
    return merchant?.imageUrl?.trim() || null
  }

  const getProductImageUrl = (productId: string): string | null => {
    const product = products.find((p) => p.id === productId)
    return product?.imageUrl?.trim() || null
  }

  if (result.merchants.length === 0) {
    return (
      <Card className="border-border/60 bg-card/80 backdrop-blur-sm">
        <CardContent className="flex flex-col items-center gap-3 py-8">
          <div className="flex size-12 items-center justify-center rounded-2xl bg-muted text-muted-foreground">
            <Sparkles className="size-6" />
          </div>
          <p className="text-sm text-muted-foreground">没有找到匹配的商家，换个关键词试试</p>
          <Button variant="outline" size="sm" onClick={onRetry}>
            重新搜索
          </Button>
        </CardContent>
      </Card>
    )
  }

  return (
    <div className="space-y-3">
      {/* Summary */}
      <div className="flex items-center gap-2 px-1">
        <Sparkles className="size-3.5 text-primary" aria-hidden />
        <p className="text-sm font-medium text-foreground">{result.summary}</p>
      </div>

      {/* Merchant cards */}
      <div className="space-y-3">
        {result.merchants.map((rec) => {
          const merchantImage = getMerchantImageUrl(rec.merchantId)

          return (
            <Card
              key={rec.merchantId}
              className="group cursor-pointer border-border/60 bg-card/90 backdrop-blur-sm transition-all duration-200 hover:border-primary/30 hover:shadow-[0_12px_40px_rgba(124,58,237,0.1)]"
              onClick={() => navigate(`/delivery/customer/m/${encodeURIComponent(rec.merchantId)}`)}
            >
              <CardContent className="flex gap-4 p-4">
                {/* Merchant image */}
                <div className="shrink-0">
                  {merchantImage ? (
                    <div className="size-20 overflow-hidden rounded-xl">
                      <img
                        src={resolveApiMediaUrl(merchantImage)}
                        alt={`${rec.storeName} 店铺`}
                        className="size-full object-cover transition-transform duration-300 group-hover:scale-105"
                      />
                    </div>
                  ) : (
                    <div className="flex size-20 items-center justify-center rounded-xl bg-gradient-to-br from-primary/10 to-purple-400/10 text-primary">
                      <Store className="size-8" />
                    </div>
                  )}
                </div>

                {/* Merchant info + products */}
                <div className="flex min-w-0 flex-1 flex-col gap-2">
                  {/* Name + category */}
                  <div className="flex items-start justify-between gap-2">
                    <div className="min-w-0">
                      <div className="flex items-center gap-2">
                        <h3 className="truncate font-semibold text-foreground">{rec.storeName}</h3>
                        <ChevronRight className="size-3.5 shrink-0 text-muted-foreground opacity-0 transition-opacity group-hover:opacity-100" />
                      </div>
                      {rec.reason && (
                        <p className="mt-0.5 line-clamp-1 text-xs text-primary/80">{rec.reason}</p>
                      )}
                    </div>
                    <Badge variant="outline" className="shrink-0 border-primary/25 text-xs text-primary">
                      {rec.category}
                    </Badge>
                  </div>

                  {/* Recommended products - horizontal scroll */}
                  <div className="scrollbar-thin scrollbar-thumb-muted-foreground/20 -mx-1 flex gap-2 overflow-x-auto px-1 pb-1 snap-x snap-mandatory">
                    {rec.products.map((product) => {
                      const productImage = getProductImageUrl(product.productId)

                      return (
                        <div
                          key={product.productId}
                          className="flex w-[120px] shrink-0 snap-start flex-col gap-1.5 rounded-xl border border-border/40 bg-background/50 p-2 transition-colors hover:border-primary/20 hover:bg-primary/5"
                          onClick={(e) => e.stopPropagation()}
                        >
                          <div className="aspect-[4/3] w-full overflow-hidden rounded-lg">
                            {productImage ? (
                              <img
                                src={resolveApiMediaUrl(productImage)}
                                alt={product.productName}
                                className="size-full object-cover"
                              />
                            ) : (
                              <div className="flex size-full items-center justify-center bg-muted text-[10px] text-muted-foreground">
                                暂无图
                              </div>
                            )}
                          </div>
                          <p className="line-clamp-1 text-xs font-medium text-foreground">
                            {product.productName}
                          </p>
                          <div className="flex items-center justify-between">
                            <span className="text-xs font-semibold text-primary">
                              ¥{product.price.toFixed(0)}
                            </span>
                          </div>
                          {product.reason && (
                            <p className="line-clamp-1 text-[10px] leading-tight text-muted-foreground">
                              {product.reason}
                            </p>
                          )}
                        </div>
                      )
                    })}
                  </div>
                </div>
              </CardContent>
            </Card>
          )
        })}
      </div>
    </div>
  )
}

type AISearchErrorProps = {
  message: string
  onRetry: () => void
}

export function AISearchError({ message, onRetry }: AISearchErrorProps) {
  return (
    <Card className="border-destructive/30 bg-card/80 backdrop-blur-sm">
      <CardContent className="flex flex-col items-center gap-3 py-8">
        <div className="flex size-12 items-center justify-center rounded-2xl bg-destructive/10 text-destructive">
          <Sparkles className="size-6" />
        </div>
        <p className="max-w-sm text-center text-sm text-muted-foreground">{message}</p>
        <Button variant="outline" size="sm" onClick={onRetry}>
          重新搜索
        </Button>
      </CardContent>
    </Card>
  )
}
