import { useEffect, useMemo, useRef, useState } from 'react'
import { ArrowLeft, ChevronDown, Heart, Megaphone, MessageSquareReply, Minus, Plus, ShoppingCart, Sparkles, Store, ThumbsDown, ThumbsUp } from 'lucide-react'
import { useNavigate, useParams } from 'react-router-dom'

import { aiReviewSummaryIO } from '@/apis/ai/AIReviewSummaryAPI'
import { runTask } from '@/apis/shared/client'
import { DeliveryPageShell } from '@/components/DeliveryPageShell'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { useAppChrome } from '@/hooks/useAppChrome'
import { resolveApiMediaUrl } from '@/lib/api-media-url'
import { cn } from '@/lib/utils'
import { filterReviews, reviewFilterCounts, reviewFilterOptions, type ReviewFilterKey } from '@/lib/review-filters'
import { ListingStatuses } from '@/objects/shared/ids'
import type { MerchantId } from '@/objects/shared/ids'
import type { ProductId } from '@/objects/shared/ids'
import type { AIReviewSummaryResponse } from '@/objects/ai/apiTypes/AIReviewSummaryResponse'
import type { MerchantReviewsResponse } from '@/objects/review/apiTypes/MerchantReviewsResponse'
import { useCustomerPortalStore } from '@/stores/pages/use-customer-portal-store'

type MerchantPane = 'menu' | 'reviews'

const productCategoryName = (product: { categoryName?: string | null }) => product.categoryName?.trim() || '默认分类'

function highlightedSummaryParts(summary: string, highlights: string[]) {
  const cleanHighlights = highlights.filter((item) => item.trim() && summary.includes(item)).slice(0, 4)
  const parts: Array<{ text: string; highlighted: boolean }> = []
  let cursor = 0

  while (cursor < summary.length) {
    const next = cleanHighlights
      .map((highlight) => ({ highlight, index: summary.indexOf(highlight, cursor) }))
      .filter((item) => item.index >= 0)
      .sort((a, b) => a.index - b.index || b.highlight.length - a.highlight.length)[0]

    if (!next) {
      parts.push({ text: summary.slice(cursor), highlighted: false })
      break
    }

    if (next.index > cursor) {
      parts.push({ text: summary.slice(cursor, next.index), highlighted: false })
    }
    parts.push({ text: next.highlight, highlighted: true })
    cursor = next.index + next.highlight.length
  }

  return parts
}

export default function CustomerMerchantOrderPage() {
  const { merchantId: merchantIdParam } = useParams<{ merchantId: string }>()
  const navigate = useNavigate()
  const { showNotice } = useAppChrome()

  const bootstrapDone = useCustomerPortalStore((s) => s.bootstrapDone)
  const loadError = useCustomerPortalStore((s) => s.loadError)
  const customerAccount = useCustomerPortalStore((s) => s.customerAccount)
  const merchants = useCustomerPortalStore((s) => s.merchants)
  const products = useCustomerPortalStore((s) => s.products)
  const cartLines = useCustomerPortalStore((s) => s.cartLines)
  const favorites = useCustomerPortalStore((s) => s.favorites)
  const addProductToCart = useCustomerPortalStore((s) => s.addProductToCart)
  const changeQuantity = useCustomerPortalStore((s) => s.changeQuantity)
  const toggleFavorite = useCustomerPortalStore((s) => s.toggleFavorite)
  const setActiveTab = useCustomerPortalStore((s) => s.setActiveTab)
  const fetchMerchantReviews = useCustomerPortalStore((s) => s.fetchMerchantReviews)
  const voteMerchantReview = useCustomerPortalStore((s) => s.voteMerchantReview)
  const [cartExpanded, setCartExpanded] = useState(false)
  const [reviews, setReviews] = useState<MerchantReviewsResponse | null>(null)
  const [activePane, setActivePane] = useState<MerchantPane>('menu')
  const [activeReviewFilter, setActiveReviewFilter] = useState<ReviewFilterKey>('all')
  const [aiReviewSummary, setAIReviewSummary] = useState<AIReviewSummaryResponse | null>(null)
  const [aiReviewSummaryLoading, setAIReviewSummaryLoading] = useState(false)
  const [aiReviewSummaryError, setAIReviewSummaryError] = useState<string | null>(null)
  const [summaryExpanded, setSummaryExpanded] = useState(false)
  const [activeCategoryName, setActiveCategoryName] = useState('默认分类')
  const categorySectionRefs = useRef<Record<string, HTMLElement | null>>({})

  useEffect(() => {
    void (async () => {
      const st = useCustomerPortalStore.getState()
      if (!st.bootstrapDone && !st.loadError) {
        await st.bootstrap()
      }
    })()
  }, [])

  const merchantId = merchantIdParam as MerchantId | undefined
  const merchant = merchantId ? merchants.find((m) => m.id === merchantId) ?? null : null
  const merchantProducts = useMemo(
    () =>
      merchantId
        ? products.filter((p) => p.merchantId === merchantId && p.listingStatus === ListingStatuses.listed)
        : [],
    [merchantId, products],
  )
  const categoryGroups = useMemo(
    () =>
      merchantProducts.reduce<Array<{ categoryName: string; products: typeof merchantProducts }>>(
        (groups, product) => {
          const categoryName = productCategoryName(product)
          const matched = groups.find((group) => group.categoryName === categoryName)
          if (matched) {
            matched.products.push(product)
          } else {
            groups.push({ categoryName, products: [product] })
          }
          return groups
        },
        [],
      ),
    [merchantProducts],
  )
  const linesForMerchant = merchantId ? cartLines.filter((l) => l.merchantId === merchantId) : []

  useEffect(() => {
    if (!merchantId) return
    setActiveReviewFilter('all')
    setAIReviewSummary(null)
    setAIReviewSummaryError(null)
    setSummaryExpanded(false)
    void (async () => {
      try {
        const nextReviews = await fetchMerchantReviews(merchantId)
        setReviews(nextReviews)
        if (nextReviews.reviews.length === 0) {
          setAIReviewSummaryLoading(false)
          return
        }

        setAIReviewSummaryLoading(true)
        try {
          const summary = await runTask(aiReviewSummaryIO(merchantId))
          setAIReviewSummary(summary)
        } catch (error) {
          setAIReviewSummaryError(error instanceof Error ? error.message : 'AI 评价总结生成失败')
        } finally {
          setAIReviewSummaryLoading(false)
        }
      } catch {
        setReviews(null)
        setAIReviewSummaryLoading(false)
      }
    })()
  }, [fetchMerchantReviews, merchantId])

  useEffect(() => {
    if (!bootstrapDone || merchants.length === 0 || !merchantId) return
    if (!merchant) {
      navigate('/delivery/customer', { replace: true })
    }
  }, [bootstrapDone, merchant, merchantId, merchants.length, navigate])

  useEffect(() => {
    const firstCategory = categoryGroups[0]?.categoryName
    if (firstCategory && !categoryGroups.some((group) => group.categoryName === activeCategoryName)) {
      setActiveCategoryName(firstCategory)
    }
  }, [activeCategoryName, categoryGroups])

  useEffect(() => {
    if (activePane !== 'menu') return
    const sections = categoryGroups
      .map((group) => categorySectionRefs.current[group.categoryName])
      .filter((section): section is HTMLElement => Boolean(section))
    if (sections.length === 0) return

    const observer = new IntersectionObserver(
      (entries) => {
        const visible = entries
          .filter((entry) => entry.isIntersecting)
          .sort((a, b) => a.boundingClientRect.top - b.boundingClientRect.top)[0]
        const categoryName = visible?.target.getAttribute('data-category-name')
        if (categoryName) {
          setActiveCategoryName(categoryName)
        }
      },
      { rootMargin: '-24% 0px -62% 0px', threshold: [0, 0.15, 0.35] },
    )

    sections.forEach((section) => observer.observe(section))
    return () => observer.disconnect()
  }, [activePane, categoryGroups])

  const cartTotalForMerchant = linesForMerchant.reduce((sum, line) => {
    const product = products.find((item) => item.id === line.productId)
    return sum + (product ? product.price * line.quantity : 0)
  }, 0)
  const lineCountForMerchant = linesForMerchant.reduce((sum, line) => sum + line.quantity, 0)
  const merchantFavorited = merchantId ? favorites.merchantIds.includes(merchantId) : false
  const allReviews = reviews?.reviews ?? []
  const reviewCounts = reviewFilterCounts(allReviews)
  const filteredReviews = filterReviews(allReviews, activeReviewFilter)
  const summaryText = aiReviewSummary?.summary ?? ''
  const shouldCollapseSummary = summaryText.length > 118
  const visibleSummary =
    shouldCollapseSummary && !summaryExpanded ? `${summaryText.slice(0, 118)}...` : summaryText
  const summaryParts = highlightedSummaryParts(visibleSummary, aiReviewSummary?.highlights ?? [])

  const handleAdd = (mId: MerchantId, productId: ProductId) => {
    addProductToCart(mId, productId)
    showNotice('已加入本店购物车。', 'success')
  }

  const switchPane = (pane: MerchantPane) => {
    setActivePane(pane)
    window.requestAnimationFrame(() => {
      document.getElementById('merchant-pane')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    })
  }

  const scrollToCategory = (categoryName: string) => {
    setActiveCategoryName(categoryName)
    categorySectionRefs.current[categoryName]?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }

  if (!bootstrapDone) {
    return (
      <DeliveryPageShell>
        <Card className="border-border/70 bg-card/90 backdrop-blur-sm">
          <CardContent className="p-6 text-sm text-muted-foreground">加载中…</CardContent>
        </Card>
      </DeliveryPageShell>
    )
  }

  if (loadError) {
    return (
      <DeliveryPageShell>
        <Card className="border-border/70 bg-card/90 backdrop-blur-sm">
          <CardContent className="p-6 text-sm text-destructive">{loadError}</CardContent>
        </Card>
      </DeliveryPageShell>
    )
  }

  if (!customerAccount || !merchantId || !merchant) {
    return (
      <DeliveryPageShell>
        <Card className="border-border/70 bg-card/90 backdrop-blur-sm">
          <CardContent className="p-6 text-sm text-muted-foreground">正在跳转…</CardContent>
        </Card>
      </DeliveryPageShell>
    )
  }

  return (
    <DeliveryPageShell>
      <div className="space-y-5 pb-[min(28rem,72vh)] sm:pb-[min(26rem,65vh)]">
        <Button
          type="button"
          variant="outline"
          className="cursor-pointer border-border/70 bg-background/80"
          onClick={() => navigate('/delivery/customer')}
        >
          <ArrowLeft className="mr-2 size-4" aria-hidden />
          返回商家列表
        </Button>

        <Card className="border-border/70 bg-card/90 shadow-[0_18px_50px_rgba(15,23,42,0.05)] backdrop-blur-sm dark:shadow-[0_18px_50px_rgba(0,0,0,0.3)]">
          <CardHeader className="gap-3 pb-2 sm:flex-row sm:items-center sm:justify-between">
            <div className="flex items-start gap-3">
              <span className="flex size-11 shrink-0 items-center justify-center rounded-xl bg-primary/10 text-primary">
                <Store className="size-5" aria-hidden />
              </span>
              <div className="space-y-1">
                <div className="flex flex-wrap items-center gap-2">
                  <CardTitle className="text-xl">{merchant.storeName}</CardTitle>
                  <Button
                    type="button"
                    size="icon"
                    variant="outline"
                    className={cn(
                      'size-9 cursor-pointer rounded-full border-primary/20 bg-background/80 transition-colors hover:border-primary/40 hover:bg-primary/5',
                      merchantFavorited && 'border-rose-200 bg-rose-50 text-rose-500 hover:bg-rose-50',
                    )}
                    aria-label={merchantFavorited ? '取消收藏店铺' : '收藏店铺'}
                    title={merchantFavorited ? '取消收藏店铺' : '收藏店铺'}
                    onClick={() => {
                      toggleFavorite('merchant', merchant.id)
                      showNotice(merchantFavorited ? '已取消收藏店铺。' : '已收藏店铺。', 'success')
                    }}
                  >
                    <Heart className={cn('size-4', merchantFavorited && 'fill-current')} aria-hidden />
                  </Button>
                </div>
                <CardDescription className="text-pretty">{merchant.address}</CardDescription>
                <Badge variant="outline" className="mt-1 w-fit border-primary/25 text-primary">
                  {merchant.category}
                </Badge>
                {(merchant.description ?? '').trim() ? (
                  <p className="max-w-2xl text-sm leading-relaxed text-foreground/75">{merchant.description}</p>
                ) : null}
              </div>
            </div>
            {merchant.imageUrl?.trim() ? (
              <div className="mx-auto aspect-square w-full max-w-[7.5rem] shrink-0 overflow-hidden rounded-xl border border-border/60 sm:mx-0">
                <img
                  src={resolveApiMediaUrl(merchant.imageUrl)}
                  alt={`${merchant.storeName} 店铺`}
                  className="size-full object-cover"
                />
              </div>
            ) : null}
          </CardHeader>
        </Card>

        {(merchant.announcement ?? '').trim() ? (
          <div className="rounded-2xl border border-primary/20 bg-primary/10 px-4 py-3 text-primary shadow-sm">
            <div className="flex items-start gap-2">
              <Megaphone className="mt-0.5 size-4 shrink-0" aria-hidden />
              <div>
                <p className="text-sm font-semibold">店铺公告</p>
                <p className="mt-1 text-sm leading-6 text-foreground">{merchant.announcement.trim()}</p>
              </div>
            </div>
          </div>
        ) : null}

        <div className="sticky top-0 z-30 border-b border-border/60 bg-background/95 backdrop-blur-md">
          <div className="flex items-center gap-8 px-2 py-3">
            <button
              type="button"
              className={cn(
                'relative cursor-pointer text-base font-semibold transition-colors',
                activePane === 'menu' ? 'text-foreground' : 'text-muted-foreground hover:text-foreground',
              )}
              onClick={() => switchPane('menu')}
            >
              商家点菜
              {activePane === 'menu' ? <span className="absolute -bottom-3 left-0 h-1 w-full rounded-full bg-primary" /> : null}
            </button>
            <button
              type="button"
              className={cn(
                'relative cursor-pointer text-base font-semibold transition-colors',
                activePane === 'reviews' ? 'text-foreground' : 'text-muted-foreground hover:text-foreground',
              )}
              onClick={() => switchPane('reviews')}
            >
              用户评价
              {activePane === 'reviews' ? <span className="absolute -bottom-3 left-0 h-1 w-full rounded-full bg-primary" /> : null}
            </button>
          </div>
        </div>

        <div id="merchant-pane" className="scroll-mt-16">
          {activePane === 'menu' ? (
            <Card className="border-border/70 bg-card/90 shadow-[0_18px_50px_rgba(15,23,42,0.05)] backdrop-blur-sm dark:shadow-[0_18px_50px_rgba(0,0,0,0.3)]">
              <CardHeader className="gap-1 pb-2">
                <CardTitle className="text-xl">商家点菜</CardTitle>
                <CardDescription>选择菜品后加入页面底部购物车栏，展开可改数量，本店应付与结算始终固定在屏幕下方</CardDescription>
              </CardHeader>
              <CardContent className="p-0">
                {categoryGroups.length === 0 ? (
                  <p className="px-4 pb-5 text-sm text-muted-foreground">当前店铺暂无可点菜品。</p>
                ) : (
                  <div className="grid grid-cols-[6.75rem_minmax(0,1fr)] gap-0 sm:grid-cols-[8.5rem_minmax(0,1fr)]">
                    <aside className="border-r border-border/60 bg-secondary/20">
                      <div className="sticky top-[3.25rem] max-h-[calc(100vh-7rem)] overflow-y-auto py-2">
                        {categoryGroups.map((group) => (
                          <button
                            key={group.categoryName}
                            type="button"
                            className={cn(
                              'relative w-full cursor-pointer px-3 py-3 text-left text-sm font-medium transition-colors sm:px-4',
                              activeCategoryName === group.categoryName
                                ? 'bg-background text-primary'
                                : 'text-muted-foreground hover:bg-background/70 hover:text-foreground',
                            )}
                            onClick={() => scrollToCategory(group.categoryName)}
                          >
                            {activeCategoryName === group.categoryName ? (
                              <span className="absolute inset-y-3 left-0 w-1 rounded-r-full bg-primary" />
                            ) : null}
                            <span className="line-clamp-2">{group.categoryName}</span>
                          </button>
                        ))}
                      </div>
                    </aside>
                    <div className="min-w-0 space-y-5 px-3 pb-4 sm:px-4">
                      {categoryGroups.map((group) => (
                        <section
                          key={group.categoryName}
                          ref={(node) => {
                            categorySectionRefs.current[group.categoryName] = node
                          }}
                          data-category-name={group.categoryName}
                          className="scroll-mt-24 space-y-3 pt-4"
                        >
                          <div className="flex items-center gap-3">
                            <h3 className="text-lg font-semibold text-foreground">{group.categoryName}</h3>
                            <span className="h-px flex-1 bg-border/70" />
                          </div>
                          <div className="space-y-3">
                            {group.products.map((product) => (
                              <div
                                key={product.id}
                                className="relative overflow-hidden rounded-2xl border border-border/70 bg-gradient-to-br from-card to-secondary/25 p-4 shadow-sm transition-[box-shadow,border-color] duration-200 hover:border-primary/35 hover:shadow-md"
                              >
                                {(() => {
                                  const productFavorited = favorites.productIds.includes(product.id)
                                  return (
                                    <Button
                                      type="button"
                                      size="icon"
                                      variant="outline"
                                      className={cn(
                                        'absolute right-3 top-3 z-10 size-9 cursor-pointer rounded-full border-primary/20 bg-background/85 shadow-sm backdrop-blur-sm transition-colors hover:border-primary/40 hover:bg-primary/5',
                                        productFavorited && 'border-rose-200 bg-rose-50 text-rose-500 hover:bg-rose-50',
                                      )}
                                      aria-label={productFavorited ? '取消收藏菜品' : '收藏菜品'}
                                      title={productFavorited ? '取消收藏菜品' : '收藏菜品'}
                                      onClick={() => {
                                        toggleFavorite('product', product.id)
                                        showNotice(productFavorited ? '已取消收藏菜品。' : '已收藏菜品。', 'success')
                                      }}
                                    >
                                      <Heart className={cn('size-4', productFavorited && 'fill-current')} aria-hidden />
                                    </Button>
                                  )
                                })()}
                                <div className="absolute inset-y-3 left-0 w-1 rounded-full bg-gradient-to-b from-primary to-[var(--delivery-brand-blue)]" />
                                <div className="flex gap-3 pl-3 pr-8">
                                  {product.imageUrl?.trim() ? (
                                    <div className="aspect-square w-24 shrink-0 overflow-hidden rounded-xl border border-border/60 bg-muted">
                                      <img
                                        src={resolveApiMediaUrl(product.imageUrl)}
                                        alt={product.name}
                                        className="size-full object-cover"
                                      />
                                    </div>
                                  ) : null}
                                  <div className="min-w-0 flex-1 space-y-3">
                                    <div className="space-y-1">
                                      <p className="font-semibold text-foreground">{product.name}</p>
                                      <p className="text-sm leading-relaxed text-muted-foreground">{product.description}</p>
                                    </div>
                                    <div className="flex flex-wrap items-center justify-between gap-3">
                                      <span className="text-lg font-semibold tabular-nums text-primary">¥{product.price.toFixed(1)}</span>
                                      <Button
                                        size="sm"
                                        className="cursor-pointer bg-[var(--delivery-brand-blue)] text-white shadow-md transition-[filter,box-shadow] duration-200 hover:brightness-110 hover:shadow-lg"
                                        onClick={() => handleAdd(product.merchantId, product.id)}
                                      >
                                        <ShoppingCart className="size-4" />
                                        加入购物车
                                      </Button>
                                    </div>
                                  </div>
                                </div>
                              </div>
                            ))}
                          </div>
                        </section>
                      ))}
                    </div>
                  </div>
                )}
              </CardContent>
            </Card>
          ) : (
            <Card className="border-border/70 bg-card/90 shadow-[0_18px_50px_rgba(15,23,42,0.05)] backdrop-blur-sm">
              <CardHeader className="gap-1 pb-2">
                <CardTitle className="text-xl">用户评价</CardTitle>
                <CardDescription>
                  平均评分 {(reviews?.summary.averageRating ?? merchant.rating).toFixed(1)} · {reviews?.summary.reviewCount ?? 0} 条评价
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-3">
                {!reviews || allReviews.length === 0 ? (
                  <p className="rounded-xl border border-dashed border-border/70 px-4 py-6 text-sm text-muted-foreground">
                    暂无用户评价。
                  </p>
                ) : (
                  <>
                    <div className="flex flex-wrap gap-2">
                      {reviewFilterOptions.map((option) => (
                        <button
                          key={option.key}
                          type="button"
                          className={cn(
                            'rounded-full border px-3 py-1.5 text-sm font-medium transition-colors',
                            activeReviewFilter === option.key
                              ? 'border-primary/25 bg-primary/10 text-primary'
                              : 'border-border/70 bg-background text-muted-foreground hover:border-primary/25 hover:text-primary',
                          )}
                          onClick={() => setActiveReviewFilter(option.key)}
                        >
                          {option.label} {reviewCounts[option.key]}
                        </button>
                      ))}
                    </div>
                    {aiReviewSummaryLoading ? (
                      <div className="rounded-2xl border border-primary/10 bg-gradient-to-br from-primary/5 to-amber-50/70 p-4">
                        <div className="mb-3 flex items-center gap-2">
                          <span className="flex size-8 items-center justify-center rounded-full bg-white text-primary shadow-sm">
                            <Sparkles className="size-4 animate-pulse" />
                          </span>
                          <p className="font-semibold text-foreground">评价总结</p>
                        </div>
                        <div className="space-y-2">
                          <div className="h-4 w-11/12 animate-pulse rounded bg-primary/10" />
                          <div className="h-4 w-9/12 animate-pulse rounded bg-primary/10" />
                          <div className="h-4 w-7/12 animate-pulse rounded bg-primary/10" />
                        </div>
                      </div>
                    ) : aiReviewSummary ? (
                      <div className="overflow-hidden rounded-2xl border border-primary/10 bg-gradient-to-br from-primary/5 via-background to-amber-50/70 shadow-sm">
                        <div className="p-4">
                          <div className="mb-3 flex items-center gap-2">
                            <span className="flex size-8 items-center justify-center rounded-full bg-white text-primary shadow-sm">
                              <Sparkles className="size-4" />
                            </span>
                            <p className="font-semibold text-foreground">评价总结</p>
                          </div>
                          <p className="text-pretty text-base leading-8 text-foreground">
                            {summaryParts.map((part, index) => (
                              <span
                                key={`${part.text}-${index}`}
                                className={part.highlighted ? 'font-semibold text-orange-600' : undefined}
                              >
                                {part.text}
                              </span>
                            ))}
                            {shouldCollapseSummary ? (
                              <button
                                type="button"
                                className="ml-2 cursor-pointer text-sm font-medium text-muted-foreground hover:text-primary"
                                onClick={() => setSummaryExpanded((value) => !value)}
                              >
                                {summaryExpanded ? '收起' : '展开'}
                              </button>
                            ) : null}
                          </p>
                        </div>
                        <div className="flex items-center justify-between gap-3 border-t border-border/50 px-4 py-3 text-sm text-muted-foreground">
                          <span>大模型总结{aiReviewSummary.reviewCount}条真实评价生成</span>
                          <span className="flex items-center gap-3">
                            <ThumbsUp className="size-4" />
                            <ThumbsDown className="size-4" />
                          </span>
                        </div>
                      </div>
                    ) : aiReviewSummaryError ? (
                      <p className="rounded-xl border border-dashed border-border/70 px-4 py-3 text-sm text-muted-foreground">
                        AI 评价总结暂不可用：{aiReviewSummaryError}
                      </p>
                    ) : null}
                    {filteredReviews.length === 0 ? (
                      <p className="rounded-xl border border-dashed border-border/70 px-4 py-6 text-sm text-muted-foreground">
                        当前标签下暂无评价。
                      </p>
                    ) : (
                      filteredReviews.map((review) => (
                        <article key={review.id} className="rounded-2xl border border-border/70 bg-background/80 p-4">
                          <div className="flex flex-wrap items-start justify-between gap-3">
                            <div>
                              <p className="font-medium text-foreground">{review.customerName}</p>
                              <p className="mt-1 text-sm text-amber-500">{'★'.repeat(review.rating)}{'☆'.repeat(5 - review.rating)}</p>
                            </div>
                            <div className="flex gap-2">
                              <Button
                                type="button"
                                size="sm"
                                variant="outline"
                                onClick={() => {
                                  void voteMerchantReview(review.id, 'up').then((result) => {
                                    if (!result.ok) showNotice(result.message, 'error')
                                    else void fetchMerchantReviews(merchant.id).then(setReviews)
                                  })
                                }}
                              >
                                <ThumbsUp className="size-4" />
                                {review.upvotes}
                              </Button>
                              <Button
                                type="button"
                                size="sm"
                                variant="outline"
                                onClick={() => {
                                  void voteMerchantReview(review.id, 'down').then((result) => {
                                    if (!result.ok) showNotice(result.message, 'error')
                                    else void fetchMerchantReviews(merchant.id).then(setReviews)
                                  })
                                }}
                              >
                                <ThumbsDown className="size-4" />
                                {review.downvotes}
                              </Button>
                            </div>
                          </div>
                          {review.orderItemNames && review.orderItemNames.length > 0 ? (
                            <p className="mt-3 rounded-xl border border-primary/15 bg-primary/5 px-3 py-2 text-xs font-medium text-primary">
                              对应菜品：{review.orderItemNames.join('、')}
                            </p>
                          ) : null}
                          <p className="mt-3 text-sm leading-6 text-muted-foreground">{review.description}</p>
                          {review.imageUrl ? (
                            <img
                              src={resolveApiMediaUrl(review.imageUrl)}
                              alt="评价图片"
                              className="mt-3 aspect-video w-full max-w-sm rounded-xl object-cover"
                            />
                          ) : null}
                          {review.merchantReply ? (
                            <div className="mt-3 rounded-xl border border-primary/15 bg-primary/5 px-3 py-2">
                              <p className="flex items-center gap-1 text-xs font-semibold text-primary">
                                <MessageSquareReply className="size-3.5" />
                                商家回复
                              </p>
                              <p className="mt-1 text-sm leading-6 text-foreground/75">{review.merchantReply}</p>
                            </div>
                          ) : null}
                        </article>
                      ))
                    )}
                  </>
                )}
              </CardContent>
            </Card>
          )}
        </div>

      </div>

      {/* 底部固定：可折叠「本店已选」+ 始终可见的本店应付与结算（避免长列表要滑到底） */}
      <div
        className="pointer-events-none fixed inset-x-0 z-40 px-5 sm:px-10"
        style={{ bottom: 'max(1rem, env(safe-area-inset-bottom, 0px))' }}
      >
        <div className="pointer-events-auto mx-auto max-w-6xl overflow-hidden rounded-2xl border border-border/70 bg-card/95 shadow-[0_-12px_40px_rgba(15,23,42,0.12)] backdrop-blur-md dark:border-border/80 dark:bg-card/90 dark:shadow-[0_-12px_40px_rgba(0,0,0,0.45)]">
          <button
            type="button"
            className="flex w-full cursor-pointer items-center justify-between gap-3 border-b border-border/50 px-4 py-3 text-left transition-colors hover:bg-muted/40"
            aria-expanded={cartExpanded}
            onClick={() => setCartExpanded((v) => !v)}
          >
            <span className="flex items-center gap-2 font-semibold text-foreground">
              <span className="flex size-8 items-center justify-center rounded-lg bg-primary/10 text-primary">
                <ShoppingCart className="size-4" aria-hidden />
              </span>
              本店已选
            </span>
            <span className="text-sm text-muted-foreground">
              {linesForMerchant.length === 0 ? '暂无商品' : `共 ${lineCountForMerchant} 件`}
            </span>
            <ChevronDown
              className={cn('size-5 shrink-0 text-muted-foreground transition-transform', cartExpanded && 'rotate-180')}
              aria-hidden
            />
          </button>
          {cartExpanded ? (
            <div className="max-h-[min(40vh,20rem)] overflow-y-auto border-b border-border/50 px-3 py-2">
              {linesForMerchant.length === 0 ? (
                <div className="rounded-xl border border-dashed border-primary/30 bg-muted/20 px-4 py-6 text-center text-sm text-muted-foreground">
                  还没有选择菜品，在上方加入购物车
                </div>
              ) : (
                <div className="space-y-2">
                  {linesForMerchant.map((line) => {
                    const product = products.find((item) => item.id === line.productId)
                    if (!product) return null
                    return (
                      <div
                        key={line.productId}
                        className="flex items-center justify-between gap-3 rounded-xl border border-border/50 bg-background/80 p-2.5"
                      >
                        <div className="min-w-0 space-y-0.5">
                          <p className="truncate font-medium text-foreground">{product.name}</p>
                          <p className="text-xs text-muted-foreground">¥{product.price.toFixed(1)}</p>
                        </div>
                        <div className="flex shrink-0 items-center gap-1.5">
                          <Button
                            size="icon"
                            variant="outline"
                            className="size-8 cursor-pointer"
                            onClick={() => changeQuantity(line.merchantId, line.productId, line.quantity - 1)}
                          >
                            <Minus className="size-4" />
                          </Button>
                          <span className="w-7 text-center text-sm font-semibold tabular-nums">{line.quantity}</span>
                          <Button
                            size="icon"
                            variant="outline"
                            className="size-8 cursor-pointer"
                            onClick={() => changeQuantity(line.merchantId, line.productId, line.quantity + 1)}
                          >
                            <Plus className="size-4" />
                          </Button>
                        </div>
                      </div>
                    )
                  })}
                </div>
              )}
            </div>
          ) : null}
          <div className="flex flex-col gap-3 bg-gradient-to-br from-primary/8 via-card to-[var(--delivery-brand-blue)]/8 p-4 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.18em] text-muted-foreground">本店应付</p>
              <p className="mt-1 text-2xl font-semibold tabular-nums text-primary">¥{cartTotalForMerchant.toFixed(1)}</p>
            </div>
            <div className="flex w-full flex-col gap-2 sm:w-auto sm:flex-row sm:justify-end">
              <Button
                type="button"
                variant="outline"
                className="h-11 min-w-[8.5rem] cursor-pointer border-border/80 font-semibold transition-colors hover:border-primary/40 hover:bg-primary/5"
                onClick={() => {
                  setActiveTab('cart')
                  navigate('/delivery/customer')
                }}
              >
                前往购物车
              </Button>
              <Button
                className="h-11 min-w-[8.5rem] cursor-pointer bg-gradient-to-r from-primary to-[oklch(0.62_0.18_45)] text-base font-semibold text-primary-foreground shadow-[0_16px_40px_rgba(225,29,72,0.35)] transition-[filter,box-shadow] duration-200 hover:brightness-110 hover:shadow-[0_20px_50px_rgba(225,29,72,0.42)] disabled:cursor-not-allowed disabled:opacity-50 dark:to-[oklch(0.68_0.14_45)]"
                disabled={linesForMerchant.length === 0}
                onClick={() => {
                  if (!merchantId || linesForMerchant.length === 0) return
                  navigate(`/delivery/customer/checkout?merchantId=${encodeURIComponent(merchantId)}`)
                }}
              >
                结算本店
              </Button>
            </div>
          </div>
        </div>
      </div>
    </DeliveryPageShell>
  )
}
