import { useEffect, useMemo, useState } from 'react'
import { AlertTriangle, ChartNoAxesCombined, Clock3, DollarSign, PackageSearch, Percent, ReceiptText, Sparkles, Trophy } from 'lucide-react'

import { fetchMerchantReviewsIO } from '@/apis/review/MerchantReviewsAPI'
import { runTask } from '@/apis/shared/client'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import type { MerchantStoreProfile } from '@/objects/merchant/MerchantStoreProfile'
import type { Order } from '@/objects/order/Order'
import type { MerchantReviewsResponse } from '@/objects/review/apiTypes/MerchantReviewsResponse'
import { OrderStatuses, RefundStatuses } from '@/objects/shared/ids'

type BusinessDataTabProps = {
  selectedStore: MerchantStoreProfile | null
}

const inventoryWarningThreshold = 10

function parsePlacedAt(value: string): Date | null {
  const match = value.match(/^(\d{4})-(\d{2})-(\d{2})(?:[ T](\d{2}):(\d{2}))?/)
  if (!match) return null
  const [, year, month, day, hour = '0', minute = '0'] = match
  return new Date(Number(year), Number(month) - 1, Number(day), Number(hour), Number(minute))
}

function isTodayOrder(order: Order) {
  const placedAt = parsePlacedAt(order.placedAt)
  const now = new Date()
  return (
    placedAt != null &&
    placedAt.getFullYear() === now.getFullYear() &&
    placedAt.getMonth() === now.getMonth() &&
    placedAt.getDate() === now.getDate()
  )
}

function startOfToday() {
  const now = new Date()
  return new Date(now.getFullYear(), now.getMonth(), now.getDate())
}

function isInDateRange(order: Order, start: Date, end: Date) {
  const placedAt = parsePlacedAt(order.placedAt)
  return placedAt != null && placedAt >= start && placedAt < end
}

function money(value: number) {
  return `¥${value.toFixed(2)}`
}

function percentChange(current: number, previous: number) {
  if (previous <= 0) return current > 0 ? 100 : 0
  return ((current - previous) / previous) * 100
}

function changeText(value: number) {
  if (Math.abs(value) < 0.1) return '与上周持平'
  return `较上周${value > 0 ? '增长' : '下降'} ${Math.abs(value).toFixed(0)}%`
}

const complaintKeywords = [
  { label: '配送慢', keywords: ['配送慢', '送得慢', '太慢', '慢', '超时', '等太久'] },
  { label: '包装问题', keywords: ['漏', '洒', '包装', '破', '撒', '汤漏'] },
  { label: '口味不稳定', keywords: ['难吃', '不好吃', '咸', '淡', '油', '辣', '味道'] },
  { label: '分量不足', keywords: ['少', '分量', '不够', '量小'] },
  { label: '菜品温度', keywords: ['凉', '冷', '不热'] },
]

function complaintReasons(reviews: MerchantReviewsResponse['reviews']) {
  const badReviews = reviews.filter((review) => review.rating <= 2)
  const counts = complaintKeywords.map((item) => ({
    label: item.label,
    count: badReviews.filter((review) => item.keywords.some((keyword) => review.description.includes(keyword))).length,
  }))
  const matched = counts.filter((item) => item.count > 0).sort((a, b) => b.count - a.count).slice(0, 3)
  if (matched.length > 0) return matched.map((item) => item.label)
  if (badReviews.length > 0) return ['低评分评价集中但原因不够明确']
  return ['暂无明显差评集中原因']
}

export function BusinessDataTab({ selectedStore }: BusinessDataTabProps) {
  const [reviews, setReviews] = useState<MerchantReviewsResponse | null>(null)
  const selectedMerchantId = selectedStore?.merchant.id ?? null
  const merchantPendingOrders = selectedStore?.pendingOrders ?? []
  const merchantHistoryOrders = selectedStore?.historyOrders ?? []
  const merchantAllOrders = useMemo(
    () => [...merchantPendingOrders, ...merchantHistoryOrders],
    [merchantHistoryOrders, merchantPendingOrders],
  )
  const allReviews = reviews?.reviews ?? []

  useEffect(() => {
    if (!selectedMerchantId) {
      setReviews(null)
      return
    }
    void runTask(fetchMerchantReviewsIO(selectedMerchantId)).then(setReviews).catch(() => setReviews(null))
  }, [selectedMerchantId])

  const dashboard = useMemo(() => {
    const todayOrders = merchantAllOrders.filter(isTodayOrder)
    const todayStart = startOfToday()
    const thisWeekStart = new Date(todayStart)
    thisWeekStart.setDate(todayStart.getDate() - 6)
    const lastWeekStart = new Date(thisWeekStart)
    lastWeekStart.setDate(thisWeekStart.getDate() - 7)
    const lastWeekEnd = new Date(thisWeekStart)
    const tomorrowStart = new Date(todayStart)
    tomorrowStart.setDate(todayStart.getDate() + 1)
    const validRevenueOrder = (order: Order) => order.status !== OrderStatuses.canceled && order.status !== OrderStatuses.refunded
    const thisWeekOrders = merchantAllOrders.filter((order) => isInDateRange(order, thisWeekStart, tomorrowStart))
    const lastWeekOrders = merchantAllOrders.filter((order) => isInDateRange(order, lastWeekStart, lastWeekEnd))
    const thisWeekRevenueOrders = thisWeekOrders.filter(validRevenueOrder)
    const lastWeekRevenueOrders = lastWeekOrders.filter(validRevenueOrder)
    const thisWeekRevenue = thisWeekRevenueOrders.reduce((sum, order) => sum + order.payableAmount, 0)
    const lastWeekRevenue = lastWeekRevenueOrders.reduce((sum, order) => sum + order.payableAmount, 0)
    const thisWeekAverageOrderValue = thisWeekOrders.length > 0 ? thisWeekRevenue / thisWeekOrders.length : 0
    const thisWeekRefundCount = thisWeekOrders.filter(
      (order) => order.status === OrderStatuses.refunded || order.refundStatus === RefundStatuses.accepted,
    ).length
    const revenueOrders = todayOrders.filter(
      (order) => order.status !== OrderStatuses.canceled && order.status !== OrderStatuses.refunded,
    )
    const todayRevenue = revenueOrders.reduce((sum, order) => sum + order.payableAmount, 0)
    const todayOrderCount = todayOrders.length
    const averageOrderValue = todayOrderCount > 0 ? todayRevenue / todayOrderCount : 0
    const statusRows = [
      { label: '已完成', count: todayOrders.filter((order) => order.status === OrderStatuses.completed).length },
      { label: '配送中', count: todayOrders.filter((order) => order.status === OrderStatuses.delivering).length },
      { label: '制作中', count: todayOrders.filter((order) => order.status === OrderStatuses.cooking).length },
      { label: '待接单', count: todayOrders.filter((order) => order.status === OrderStatuses.waitingForMerchantAcceptance).length },
    ]

    const salesByProduct = new Map<string, { name: string; quantity: number }>()
    merchantAllOrders.forEach((order) => {
      if (order.status === OrderStatuses.canceled || order.status === OrderStatuses.refunded) return
      order.items.forEach((item) => {
        const current = salesByProduct.get(item.productId) ?? { name: item.name, quantity: 0 }
        salesByProduct.set(item.productId, { ...current, quantity: current.quantity + item.quantity })
      })
    })

    const badReviewCount = allReviews.filter((review) => review.rating <= 2).length
    const refundedOrders = merchantAllOrders.filter(
      (order) => order.status === OrderStatuses.refunded || order.refundStatus === RefundStatuses.accepted,
    )
    const ordersByHour = new Map<number, number>()
    todayOrders.forEach((order) => {
      const placedAt = parsePlacedAt(order.placedAt)
      if (!placedAt) return
      const hour = placedAt.getHours()
      ordersByHour.set(hour, (ordersByHour.get(hour) ?? 0) + 1)
    })
    const peak = [...ordersByHour.entries()].sort((a, b) => b[1] - a[1])[0]
    const peakHour = peak ? `${String(peak[0]).padStart(2, '0')}:00-${String(peak[0]).padStart(2, '0')}:59` : '暂无'
    const lowStockProducts = (selectedStore?.products ?? [])
      .filter((product) => product.remainingStock < inventoryWarningThreshold)
      .sort((a, b) => a.remainingStock - b.remainingStock)
    const topProducts = [...salesByProduct.values()].sort((a, b) => b.quantity - a.quantity).slice(0, 5)
    const badReasons = complaintReasons(allReviews)
    const aiSuggestions = [
      peak ? `${peakHour} 订单最集中，建议提前备餐并安排接单人员。` : '本周高峰不明显，可先保持常规排班。',
      lowStockProducts.length > 0 ? `${lowStockProducts.slice(0, 3).map((product) => product.name).join('、')} 库存偏低，建议优先补货。` : '库存整体稳定，继续关注热销菜品消耗。',
      badReasons.includes('包装问题') ? '差评提到包装问题，建议复查饮品和汤汁类封口。' : '继续保持包装出餐检查，降低漏洒与错漏风险。',
      thisWeekRefundCount > 0 ? '本周出现退款订单，建议复盘退款原因并同步到出餐检查清单。' : '本周退款风险较低，可继续维持当前履约标准。',
    ].slice(0, 4)

    return {
      todayOrderCount,
      todayRevenue,
      averageOrderValue,
      statusRows,
      topProducts,
      badReviewRate: allReviews.length > 0 ? badReviewCount / allReviews.length : 0,
      refundRate: merchantAllOrders.length > 0 ? refundedOrders.length / merchantAllOrders.length : 0,
      peakHour,
      peakOrderCount: peak?.[1] ?? 0,
      lowStockProducts,
      weeklyReport: {
        orderCount: thisWeekOrders.length,
        orderChange: percentChange(thisWeekOrders.length, lastWeekOrders.length),
        revenue: thisWeekRevenue,
        revenueChange: percentChange(thisWeekRevenue, lastWeekRevenue),
        averageOrderValue: thisWeekAverageOrderValue,
        refundCount: thisWeekRefundCount,
        badReasons,
        topProductName: topProducts[0]?.name ?? '暂无热销菜品',
        suggestions: aiSuggestions,
      },
    }
  }, [allReviews, merchantAllOrders, selectedStore?.products])

  return (
    <Card className="border-orange-100 bg-white/95">
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <ReceiptText className="size-5 text-orange-500" />
          商家经营数据看板
        </CardTitle>
        <CardDescription>今日订单、营业额、销量排行、评价与库存风险。</CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        {!selectedStore ? (
          <p className="text-sm text-slate-500">当前未选择店铺。</p>
        ) : (
          <>
            <div className="rounded-xl border border-orange-100 bg-gradient-to-br from-orange-50 to-white p-4 shadow-sm">
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <div className="flex items-center gap-2 text-sm font-semibold text-orange-700">
                    <Sparkles className="size-4" />
                    AI 商家经营周报
                  </div>
                  <h3 className="mt-2 text-lg font-semibold text-slate-950">本周店铺经营总结</h3>
                </div>
                <span className="rounded-full border border-orange-200 bg-white px-3 py-1 text-xs font-medium text-orange-700">
                  近 7 天
                </span>
              </div>
              <div className="mt-4 grid gap-3 md:grid-cols-3">
                <div className="rounded-lg bg-white/80 px-3 py-2">
                  <p className="text-xs text-slate-500">总订单数</p>
                  <p className="mt-1 text-base font-semibold text-slate-900">
                    {dashboard.weeklyReport.orderCount} 单
                    <span className="ml-2 text-xs font-medium text-orange-600">{changeText(dashboard.weeklyReport.orderChange)}</span>
                  </p>
                </div>
                <div className="rounded-lg bg-white/80 px-3 py-2">
                  <p className="text-xs text-slate-500">本周营业额</p>
                  <p className="mt-1 text-base font-semibold text-slate-900">
                    {money(dashboard.weeklyReport.revenue)}
                    <span className="ml-2 text-xs font-medium text-orange-600">{changeText(dashboard.weeklyReport.revenueChange)}</span>
                  </p>
                </div>
                <div className="rounded-lg bg-white/80 px-3 py-2">
                  <p className="text-xs text-slate-500">客单价 / 退款</p>
                  <p className="mt-1 text-base font-semibold text-slate-900">
                    {money(dashboard.weeklyReport.averageOrderValue)}
                    <span className="ml-2 text-xs font-medium text-slate-500">退款 {dashboard.weeklyReport.refundCount} 单</span>
                  </p>
                </div>
              </div>
              <div className="mt-4 grid gap-3 lg:grid-cols-[minmax(0,0.9fr)_minmax(0,1.1fr)]">
                <div className="rounded-lg bg-white/80 px-3 py-3">
                  <p className="text-sm font-medium text-slate-900">顾客主要差评原因</p>
                  <p className="mt-2 text-sm leading-6 text-slate-600">{dashboard.weeklyReport.badReasons.join('、')}</p>
                  <p className="mt-2 text-xs text-slate-500">热销关注：{dashboard.weeklyReport.topProductName}</p>
                </div>
                <div className="rounded-lg bg-white/80 px-3 py-3">
                  <p className="text-sm font-medium text-slate-900">AI 建议</p>
                  <ul className="mt-2 space-y-1 text-sm leading-6 text-slate-600">
                    {dashboard.weeklyReport.suggestions.map((suggestion) => (
                      <li key={suggestion}>- {suggestion}</li>
                    ))}
                  </ul>
                </div>
              </div>
            </div>

            <div className="grid gap-3 md:grid-cols-3">
              <div className="rounded-xl border border-orange-100 bg-orange-50/60 p-4">
                <div className="flex items-center gap-2 text-sm text-orange-700">
                  <ReceiptText className="size-4" />
                  今日订单数
                </div>
                <p className="mt-2 text-2xl font-semibold text-slate-900">{dashboard.todayOrderCount}</p>
                <div className="mt-3 flex flex-wrap gap-2 text-xs">
                  {dashboard.statusRows.map((item) => (
                    <span key={item.label} className="rounded-full bg-white px-2 py-1 text-slate-600">
                      {item.label} {item.count}
                    </span>
                  ))}
                </div>
              </div>
              <div className="rounded-xl border border-orange-100 p-4">
                <div className="flex items-center gap-2 text-sm text-slate-600">
                  <DollarSign className="size-4 text-orange-500" />
                  今日营业额
                </div>
                <p className="mt-2 text-2xl font-semibold text-slate-900">{money(dashboard.todayRevenue)}</p>
                <p className="mt-2 text-xs text-slate-500">按实付金额汇总，已排除取消和退款订单。</p>
              </div>
              <div className="rounded-xl border border-orange-100 p-4">
                <div className="flex items-center gap-2 text-sm text-slate-600">
                  <ChartNoAxesCombined className="size-4 text-orange-500" />
                  客单价
                </div>
                <p className="mt-2 text-2xl font-semibold text-slate-900">{money(dashboard.averageOrderValue)}</p>
                <p className="mt-2 text-xs text-slate-500">营业额 / 今日订单数。</p>
              </div>
            </div>

            <div className="grid gap-4 lg:grid-cols-[minmax(0,1.25fr)_minmax(18rem,0.75fr)]">
              <div className="rounded-xl border border-orange-100 p-4">
                <div className="mb-3 flex items-center gap-2">
                  <Trophy className="size-4 text-orange-500" />
                  <p className="font-medium text-slate-900">热销菜品 Top 5</p>
                </div>
                {dashboard.topProducts.length === 0 ? (
                  <p className="text-sm text-slate-500">暂无销量数据。</p>
                ) : (
                  <div className="space-y-2">
                    {dashboard.topProducts.map((product, index) => (
                      <div key={product.name} className="flex items-center justify-between gap-3 rounded-lg bg-slate-50 px-3 py-2 text-sm">
                        <div className="flex min-w-0 items-center gap-2">
                          <span className="flex size-6 shrink-0 items-center justify-center rounded-full bg-orange-100 text-xs font-semibold text-orange-700">
                            {index + 1}
                          </span>
                          <span className="truncate text-slate-700">{product.name}</span>
                        </div>
                        <span className="shrink-0 font-medium text-slate-900">{product.quantity} 份</span>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              <div className="space-y-3">
                <div className="rounded-xl border border-orange-100 p-4">
                  <div className="flex items-center gap-2 text-sm text-slate-600">
                    <Percent className="size-4 text-orange-500" />
                    差评率
                  </div>
                  <p className="mt-2 text-xl font-semibold text-slate-900">{(dashboard.badReviewRate * 100).toFixed(1)}%</p>
                  <p className="mt-1 text-xs text-slate-500">低评分评价占全部评价比例。</p>
                </div>
                <div className="rounded-xl border border-orange-100 p-4">
                  <div className="flex items-center gap-2 text-sm text-slate-600">
                    <Percent className="size-4 text-orange-500" />
                    退款率
                  </div>
                  <p className="mt-2 text-xl font-semibold text-slate-900">{(dashboard.refundRate * 100).toFixed(1)}%</p>
                  <p className="mt-1 text-xs text-slate-500">退款订单占全部订单比例。</p>
                </div>
                <div className="rounded-xl border border-orange-100 p-4">
                  <div className="flex items-center gap-2 text-sm text-slate-600">
                    <Clock3 className="size-4 text-orange-500" />
                    高峰时段
                  </div>
                  <p className="mt-2 text-xl font-semibold text-slate-900">{dashboard.peakHour}</p>
                  <p className="mt-1 text-xs text-slate-500">
                    {dashboard.peakOrderCount > 0 ? `${dashboard.peakOrderCount} 单集中在该时段。` : '今日暂无订单。'}
                  </p>
                </div>
              </div>
            </div>

            <div className="rounded-xl border border-orange-100 p-4">
              <div className="mb-3 flex items-center gap-2">
                <AlertTriangle className="size-4 text-orange-500" />
                <p className="font-medium text-slate-900">库存预警</p>
                <span className="text-xs text-slate-500">阈值：小于 {inventoryWarningThreshold} 份</span>
              </div>
              {dashboard.lowStockProducts.length === 0 ? (
                <p className="text-sm text-slate-500">暂无低库存菜品。</p>
              ) : (
                <div className="grid gap-2 md:grid-cols-2">
                  {dashboard.lowStockProducts.map((product) => (
                    <div key={product.id} className="flex items-center justify-between gap-3 rounded-lg bg-orange-50 px-3 py-2 text-sm">
                      <div className="flex min-w-0 items-center gap-2">
                        <PackageSearch className="size-4 shrink-0 text-orange-500" />
                        <span className="truncate text-slate-700">{product.name}</span>
                      </div>
                      <span className="shrink-0 font-medium text-orange-700">{product.remainingStock} 份</span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </>
        )}
      </CardContent>
    </Card>
  )
}
