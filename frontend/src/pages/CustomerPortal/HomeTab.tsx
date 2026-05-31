import { ArrowRight, Sparkles, Store } from 'lucide-react'
import { useCallback, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'

import { AISearchBar } from '@/components/AISearchBar'
import { AISearchError, AISearchResults } from '@/components/AISearchResults'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { runTask } from '@/api/shared/client'
import { aiSearchIO } from '@/api/ai/AISearchApi'
import { resolveApiMediaUrl } from '@/lib/api-media-url'
import { cn } from '@/lib/utils'
import type { AISearchResponse } from '@/objects/ai/AISearchResponse'
import type { Merchant } from '@/objects/merchant/Merchant'
import type { Product } from '@/objects/merchant/Product'

type HomeTabProps = {
  merchants: Merchant[]
  products: Product[]
}

export function HomeTab({ merchants, products }: HomeTabProps) {
  const navigate = useNavigate()
  const [aiLoading, setAiLoading] = useState(false)
  const [aiResult, setAiResult] = useState<AISearchResponse | null>(null)
  const [aiError, setAiError] = useState<string | null>(null)
  const lastQueryRef = useRef<string>('')

  const handleAISearch = useCallback(async (query: string) => {
    setAiError(null)
    setAiLoading(true)
    lastQueryRef.current = query
    try {
      const result = await runTask(aiSearchIO({ query }))
      if (lastQueryRef.current === query) {
        setAiResult(result)
      }
    } catch (error) {
      if (lastQueryRef.current === query) {
        setAiResult(null)
        setAiError(error instanceof Error ? error.message : 'AI 搜索失败，请稍后再试')
      }
    } finally {
      if (lastQueryRef.current === query) {
        setAiLoading(false)
      }
    }
  }, [])

  const handleRetry = useCallback(() => {
    if (lastQueryRef.current) {
      handleAISearch(lastQueryRef.current)
    }
  }, [handleAISearch])

  return (
    <div className="space-y-6">
      {/* Hero */}
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

      {/* AI Search */}
      <AISearchBar onSearch={handleAISearch} loading={aiLoading} />

      {/* AI Results */}
      {aiResult && !aiError && (
        <AISearchResults
          result={aiResult}
          merchants={merchants}
          products={products}
          onRetry={handleRetry}
        />
      )}

      {aiError && <AISearchError message={aiError} onRetry={handleRetry} />}

      {/* Loading skeleton */}
      {aiLoading && (
        <div className="space-y-3">
          <div className="flex items-center gap-2 px-1">
            <div className="size-3.5 animate-pulse rounded-full bg-primary/30" />
            <div className="h-4 w-48 animate-pulse rounded bg-muted" />
          </div>
          {[1, 2].map((i) => (
            <div
              key={i}
              className="animate-pulse rounded-2xl border border-border/40 bg-card/60 p-4"
            >
              <div className="flex gap-4">
                <div className="size-20 rounded-xl bg-muted" />
                <div className="flex flex-1 flex-col gap-2">
                  <div className="h-4 w-32 rounded bg-muted" />
                  <div className="h-3 w-48 rounded bg-muted" />
                  <div className="flex gap-2">
                    {[1, 2, 3].map((j) => (
                      <div key={j} className="h-24 w-[120px] rounded-xl bg-muted" />
                    ))}
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Merchant List */}
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
