import { useMemo, useState } from 'react'
import { ArrowLeft, ChevronDown, ChevronRight, ChevronUp, Clock3, Crown, Heart, Loader2, MapPin, PackageCheck, ReceiptText, ShoppingCart, Sparkles, TicketPercent, TrendingUp, UserCircle, UserRound, Wallet } from 'lucide-react'
import { useNavigate } from 'react-router-dom'

import { DeliveryLogoutBar } from '@/components/DeliveryLogoutBar'
import { OrderChatUnreadBadge } from '@/components/OrderChatUnreadBadge'

import { DeliveryContactsSection } from './DeliveryContactsSection'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Progress } from '@/components/ui/progress'
import { useOrderChatUnreadCounts } from '@/hooks/useOrderChatUnreadCounts'
import type { AIDietWeeklyReportResponse } from '@/objects/ai/apiTypes/AIDietWeeklyReportResponse'
import type { AIOrderProgressNarrativesResponse } from '@/objects/ai/apiTypes/AIOrderProgressNarrativesResponse'
import type { Merchant } from '@/objects/merchant/Merchant'
import type { Product } from '@/objects/merchant/Product'
import type { Order } from '@/objects/order/Order'
import { OrderStatuses, RefundStatuses, type MerchantId, type OrderId, type ProductId, type VoucherId } from '@/objects/shared/ids'
import type { Voucher } from '@/objects/shared/Voucher'

const getTodayStart = () => {
  const now = new Date()
  return new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()
}

const isVoucherExpired = (voucher: Voucher, todayStart: number) => {
  const time = Date.parse(`${voucher.expiresAt}T00:00:00`)
  return Number.isNaN(time) || time < todayStart
}

const CollapsedListLimit = 3

type ProfilePanel = 'main' | 'contacts' | 'pending' | 'history' | 'favorites' | 'coupons'

type ProfileTabProps = {
  username: string
  walletBalance: number
  merchants: Merchant[]
  products: Product[]
  pendingOrders: Order[]
  historyOrders: Order[]
  vouchers: Voucher[]
  foodiePoints: number
  foodieLevel: number
  favoriteMerchantIds: MerchantId[]
  favoriteProductIds: ProductId[]
  aiDietReport: AIDietWeeklyReportResponse | null
  aiDietReportLoading: boolean
  aiDietReportError: string | null
  aiOrderProgressNarratives: AIOrderProgressNarrativesResponse | null
  onOpenRecharge: () => void
  onSelectOrder: (orderId: OrderId) => void
  onCompleteOrder: (orderId: OrderId) => void
  onAppealRefund: (orderId: OrderId) => void
  onReorderOrder: (orderId: OrderId) => void
  onGenerateAIDietReport: () => void
  onDiscardExpiredVoucher: (voucherId: VoucherId) => void
  onToggleMerchantFavorite: (merchantId: MerchantId) => void
  onToggleProductFavorite: (productId: ProductId) => void
}

export function ProfileTab({
  username,
  walletBalance,
  merchants,
  products,
  pendingOrders,
  historyOrders,
  vouchers,
  foodiePoints,
  foodieLevel,
  favoriteMerchantIds,
  favoriteProductIds,
  aiDietReport,
  aiDietReportLoading,
  aiDietReportError,
  aiOrderProgressNarratives,
  onOpenRecharge,
  onSelectOrder,
  onCompleteOrder,
  onAppealRefund,
  onReorderOrder,
  onGenerateAIDietReport,
  onDiscardExpiredVoucher,
  onToggleMerchantFavorite,
  onToggleProductFavorite,
}: ProfileTabProps) {
  const navigate = useNavigate()
  const { unreadFor } = useOrderChatUnreadCounts()
  const [activePanel, setActivePanel] = useState<ProfilePanel>('main')
  const getMerchantName = (merchantId: string) =>
    merchants.find((merchant) => merchant.id === merchantId)?.storeName ?? '未知商家'

  const getOrderStatusDescription = (order: Order) => {
    if (order.status === OrderStatuses.waitingForMerchantAcceptance) {
      return '订单已提交，正在等待商家确认接单'
    }
    if (order.status === OrderStatuses.cooking) {
      return '商家已接单，后厨正在制作餐品'
    }
    if (order.status === OrderStatuses.waitingForRiderAcceptance) {
      return '商家已出餐，正在等待骑手接单取餐'
    }
    if (order.status === OrderStatuses.delivering) {
      return '骑手已接单，餐品正在配送途中'
    }
    if (order.status === OrderStatuses.delivered) {
      return '餐品已送达，可确认完成'
    }
    if (order.status === OrderStatuses.canceled) {
      return '订单已取消，款项已按规则退回钱包'
    }
    if (order.status === OrderStatuses.refunded) {
      return '退款已通过，款项已退回钱包'
    }
    return null
  }

  const getRefundFeedback = (order: Order) => {
    if (order.refundStatus === RefundStatuses.accepted) {
      return {
        title: '退款已通过',
        message: order.refundAdminReason?.trim() || order.refundMerchantReason?.trim() || '退款申请已通过，款项已退回钱包。',
        className: 'border-emerald-200 bg-emerald-50 text-emerald-700',
      }
    }
    if (order.refundStatus === RefundStatuses.rejected) {
      return {
        title: '退款已驳回',
        message: order.refundAdminReason?.trim() || '管理员已驳回退款申请。',
        className: 'border-rose-200 bg-rose-50 text-rose-700',
      }
    }
    if (order.refundStatus === RefundStatuses.merchantRejected) {
      return {
        title: '商家已驳回',
        message: order.refundMerchantReason?.trim() || '商家已驳回退款申请，可提交管理员仲裁。',
        className: 'border-rose-200 bg-rose-50 text-rose-700',
      }
    }
    if (order.refundStatus === RefundStatuses.adminPending) {
      return {
        title: '管理员仲裁中',
        message: '退款申请已提交管理员仲裁，请等待处理结果。',
        className: 'border-amber-200 bg-amber-50 text-amber-700',
      }
    }
    if (order.refundStatus === RefundStatuses.pending || order.refundStatus === RefundStatuses.legacyPending) {
      return {
        title: '等待商家处理',
        message: '退款申请已提交给商家，超过30分钟未处理将自动通过。',
        className: 'border-amber-200 bg-amber-50 text-amber-700',
      }
    }
    return null
  }

  const [orderProgressCycleSeed] = useState(() => Math.floor(Date.now() / 15000))
  const [showAllPendingOrders, setShowAllPendingOrders] = useState(false)
  const [showAllHistoryOrders, setShowAllHistoryOrders] = useState(false)
  const openChat = (orderId: OrderId, peerRole: 'merchant' | 'rider') => {
    navigate(`/delivery/chat/${encodeURIComponent(orderId)}?peer=${peerRole}`)
  }
  const orderProgressNarratives = useMemo(() => {
    const usedMessages = new Set<string>()
    const orders = [...pendingOrders, ...historyOrders]

    return orders.reduce<Map<OrderId, string>>((narratives, order, index) => {
      if (order.status === OrderStatuses.completed || order.status === OrderStatuses.canceled) {
        return narratives
      }

      const messages = aiOrderProgressNarratives?.groups.find((group) => group.status === order.status)?.messages ?? []
      if (messages.length === 0) {
        return narratives
      }

      const orderSeed = [...order.id].reduce((sum, char) => sum + char.charCodeAt(0), 0)
      const offset = (orderSeed + orderProgressCycleSeed + index) % messages.length
      const message = messages
        .map((_, messageIndex) => messages[(offset + messageIndex) % messages.length])
        .find((candidate) => !usedMessages.has(candidate))

      if (message) {
        usedMessages.add(message)
        narratives.set(order.id, message)
      }

      return narratives
    }, new Map<OrderId, string>())
  }, [aiOrderProgressNarratives?.groups, historyOrders, orderProgressCycleSeed, pendingOrders])
  const safeFoodiePoints = Number.isFinite(foodiePoints) ? Math.max(0, Math.floor(foodiePoints)) : 0
  const safeFoodieLevel =
    Number.isFinite(foodieLevel) && foodieLevel > 0 ? Math.floor(foodieLevel) : Math.floor(safeFoodiePoints / 200) + 1
  const levelBase = Math.max(0, (safeFoodieLevel - 1) * 200)
  const pointsInLevel = Math.max(0, safeFoodiePoints - levelBase)
  const pointsToNextLevel = Math.max(0, 200 - pointsInLevel)
  const progress = Math.min(100, (pointsInLevel / 200) * 100)
  const todayStart = getTodayStart()
  const activeVouchers = vouchers.filter((voucher) => voucher.remainingCount > 0 && !isVoucherExpired(voucher, todayStart))
  const expiredVouchers = vouchers.filter((voucher) => voucher.remainingCount > 0 && isVoucherExpired(voucher, todayStart))
  const displayedVouchers = [...activeVouchers.slice(0, 3), ...expiredVouchers]
  const displayedPendingOrders = showAllPendingOrders ? pendingOrders : pendingOrders.slice(0, CollapsedListLimit)
  const displayedHistoryOrders = showAllHistoryOrders ? historyOrders : historyOrders.slice(0, CollapsedListLimit)
  const favoriteMerchants = favoriteMerchantIds
    .map((merchantId) => merchants.find((merchant) => merchant.id === merchantId))
    .filter((merchant): merchant is Merchant => Boolean(merchant))
  const favoriteProducts = favoriteProductIds
    .map((productId) => products.find((product) => product.id === productId))
    .filter((product): product is Product => Boolean(product))

  const backToProfileMain = (
    <Button
      type="button"
      variant="outline"
      className="cursor-pointer border-border/70 bg-background/80"
      onClick={() => setActivePanel('main')}
    >
      <ArrowLeft className="mr-2 size-4" aria-hidden />
      返回我的
    </Button>
  )

  const menuItems: Array<{
    panel: ProfilePanel
    label: string
    icon: typeof MapPin
    count?: string
  }> = [
    { panel: 'contacts', label: '收货信息', icon: MapPin },
    { panel: 'pending', label: '待收货订单', icon: PackageCheck, count: `${pendingOrders.length}` },
    { panel: 'history', label: '历史订单', icon: ReceiptText, count: `${historyOrders.length}` },
    { panel: 'favorites', label: '我的收藏', icon: Heart, count: `${favoriteMerchants.length + favoriteProducts.length}` },
    { panel: 'coupons', label: '我的优惠券', icon: TicketPercent, count: `${activeVouchers.length}` },
  ]

  if (activePanel === 'main') {
    return (
      <div className="space-y-4">
        <section className="grid gap-3 md:grid-cols-2">
          <Card className="border-border/70 bg-card/90 py-0 shadow-sm backdrop-blur-sm">
            <CardHeader className="pb-4">
              <CardDescription>用户名</CardDescription>
              <CardTitle className="flex items-center gap-2 text-xl font-semibold tracking-tight">
                <UserCircle className="size-5 shrink-0 text-primary" aria-hidden />
                <span className="truncate">{username || '—'}</span>
              </CardTitle>
              <p className="text-xs text-muted-foreground">登录账号，用于识别您的顾客身份</p>
            </CardHeader>
          </Card>
          <Card className="relative overflow-hidden border-border/70 bg-gradient-to-br from-primary/15 via-card/95 to-[var(--delivery-brand-blue)]/12 py-0 shadow-sm backdrop-blur-md">
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
        </section>

        <Card className="relative overflow-hidden border-orange-200/70 bg-gradient-to-br from-[#E11D48]/90 via-[#F97316]/90 to-[#FDBA74]/90 py-0 text-white shadow-[0_18px_45px_rgba(249,115,22,0.2)]">
          <CardHeader className="relative gap-3 pb-3">
            <div className="flex items-center justify-between gap-3">
              <CardDescription className="text-white/80">吃货等级</CardDescription>
              <span className="flex size-10 items-center justify-center rounded-2xl bg-white/20 shadow-inner backdrop-blur-sm">
                <Crown className="size-5" aria-hidden />
              </span>
            </div>
            <CardTitle className="text-3xl font-bold tracking-tight">Lv.{safeFoodieLevel} 美食探索家</CardTitle>
            <p className="text-sm text-white/85">累计 {safeFoodiePoints} 积分 · 每 200 积分升 1 级，升级即得平台满30减10优惠券</p>
          </CardHeader>
          <CardContent className="relative space-y-3 pb-5">
            <Progress value={progress} className="h-2 bg-white/25" />
            <div className="flex items-center justify-between text-xs text-white/85">
              <span>本级进度 {Math.floor(pointsInLevel)}/200</span>
              <span>{pointsToNextLevel === 0 ? '即将升级' : `距下一级还差 ${pointsToNextLevel} 积分`}</span>
            </div>
          </CardContent>
        </Card>

        <section className="grid gap-3 sm:grid-cols-2 lg:grid-cols-5">
          {menuItems.map((item) => {
            const Icon = item.icon
            return (
              <button
                key={item.panel}
                type="button"
                className="flex min-h-20 cursor-pointer items-center justify-between rounded-xl border border-border/70 bg-card/90 px-4 py-3 text-left shadow-sm transition-[border-color,box-shadow,transform] hover:border-primary/35 hover:shadow-md"
                onClick={() => setActivePanel(item.panel)}
              >
                <span className="flex min-w-0 items-center gap-3">
                  <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-primary/10 text-primary">
                    <Icon className="size-5" aria-hidden />
                  </span>
                  <span>
                    <span className="block font-semibold text-foreground">{item.label}</span>
                    {item.count ? <span className="mt-0.5 block text-xs text-muted-foreground">{item.count} 项</span> : null}
                  </span>
                </span>
                <ChevronRight className="size-4 shrink-0 text-muted-foreground" aria-hidden />
              </button>
            )
          })}
        </section>

        <Card className="relative overflow-hidden border-border/70 bg-gradient-to-br from-[#6366F1]/10 via-card/95 to-[#8B5CF6]/8 py-0 shadow-sm backdrop-blur-md">
          <CardHeader className="relative gap-1 pb-2">
            <CardTitle className="flex items-center gap-2 text-xl">
              <span className="flex size-9 items-center justify-center rounded-xl bg-gradient-to-br from-[#6366F1] to-[#8B5CF6] text-white shadow-[0_4px_14px_rgba(99,102,241,0.4)]">
                <Sparkles className="size-5" aria-hidden />
              </span>
              AI 饮食周报
            </CardTitle>
            <CardDescription>基于近 7 天订单智能分析</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            {!aiDietReport && !aiDietReportLoading && !aiDietReportError && (
              <div className="flex flex-col items-center gap-3 py-4">
                <p className="text-sm text-muted-foreground">点击下方按钮，AI 将为您生成饮食周报</p>
                <Button
                  className="cursor-pointer bg-gradient-to-r from-[#6366F1] to-[#8B5CF6] text-white shadow-[0_6px_20px_rgba(99,102,241,0.35)] transition-[filter,box-shadow] duration-200 hover:brightness-110 hover:shadow-[0_8px_24px_rgba(99,102,241,0.45)]"
                  onClick={onGenerateAIDietReport}
                >
                  <Sparkles className="mr-2 size-4" aria-hidden />
                  生成我的周报
                </Button>
              </div>
            )}

            {aiDietReportLoading && (
              <div className="flex flex-col items-center gap-3 py-6">
                <Loader2 className="size-8 animate-spin text-[#6366F1]" aria-hidden />
                <p className="text-sm text-muted-foreground">AI 正在分析您的饮食数据...</p>
              </div>
            )}

            {aiDietReportError && !aiDietReportLoading && (
              <div className="flex flex-col items-center gap-3 py-4">
                <p className="text-sm text-destructive">{aiDietReportError}</p>
                <Button
                  variant="outline"
                  className="cursor-pointer border-[#6366F1]/30 text-[#6366F1] transition-colors hover:border-[#6366F1]/60 hover:bg-[#6366F1]/5"
                  onClick={onGenerateAIDietReport}
                >
                  重新生成
                </Button>
              </div>
            )}

            {aiDietReport && !aiDietReportLoading && (
              <>
                <section className="grid grid-cols-2 gap-3 sm:grid-cols-4">
                  <div className="rounded-xl border border-border/60 bg-gradient-to-br from-card to-secondary/20 p-3 text-center shadow-sm">
                    <p className="text-xs text-muted-foreground">估算总热量</p>
                    <p className="mt-1 text-base font-semibold text-foreground">{aiDietReport.summary.calorieTotal}</p>
                  </div>
                  <div className="rounded-xl border border-border/60 bg-gradient-to-br from-card to-secondary/20 p-3 text-center shadow-sm">
                    <p className="text-xs text-muted-foreground">订单数</p>
                    <p className="mt-1 text-base font-semibold tabular-nums text-foreground">{aiDietReport.summary.orderCount}</p>
                  </div>
                  <div className="rounded-xl border border-border/60 bg-gradient-to-br from-card to-secondary/20 p-3 text-center shadow-sm">
                    <p className="text-xs text-muted-foreground">最爱品类</p>
                    <p className="mt-1 text-base font-semibold text-foreground">{aiDietReport.summary.topCategory}</p>
                  </div>
                  <div className="rounded-xl border border-border/60 bg-gradient-to-br from-card to-secondary/20 p-3 text-center shadow-sm">
                    <p className="text-xs text-muted-foreground">常点商家</p>
                    <p className="mt-1 text-base font-semibold text-foreground">{aiDietReport.summary.topMerchant}</p>
                  </div>
                </section>

                <div className="flex items-center justify-between pt-1">
                  <p className="text-xs text-muted-foreground">报告生成于 {aiDietReport.generatedAt}</p>
                  <Button
                    size="sm"
                    variant="outline"
                    className="cursor-pointer border-[#6366F1]/30 text-[#6366F1] transition-colors hover:border-[#6366F1]/60 hover:bg-[#6366F1]/5"
                    onClick={onGenerateAIDietReport}
                  >
                    刷新周报
                  </Button>
                </div>
              </>
            )}
          </CardContent>
        </Card>

        <DeliveryLogoutBar />
      </div>
    )
  }

  if (activePanel === 'contacts') {
    return (
      <div className="space-y-4">
        {backToProfileMain}
        <DeliveryContactsSection />
        <DeliveryLogoutBar />
      </div>
    )
  }

  if (activePanel === 'coupons') {
    return (
      <div className="space-y-4">
        {backToProfileMain}
        <Card className="overflow-hidden border-orange-200/70 bg-gradient-to-br from-orange-50/95 via-card to-rose-50/80 py-0 shadow-[0_18px_50px_rgba(249,115,22,0.12)] backdrop-blur-sm">
          <CardHeader className="pb-3">
            <CardTitle className="flex items-center gap-2 text-xl">
              <span className="flex size-9 items-center justify-center rounded-xl bg-orange-100 text-orange-600">
                <TicketPercent className="size-5" aria-hidden />
              </span>
              我的优惠券
            </CardTitle>
            <CardDescription>
              当前可用 {activeVouchers.length} 张，已过期 {expiredVouchers.length} 张，结算时可选择抵扣
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-2 pb-5">
            {displayedVouchers.length === 0 ? (
              <p className="rounded-xl border border-dashed border-orange-200 bg-white/70 px-3 py-3 text-sm text-muted-foreground">
                暂无可用券，完成订单升级后会自动发放平台满30减10优惠券。
              </p>
            ) : (
              displayedVouchers.map((voucher) => {
                const expired = isVoucherExpired(voucher, todayStart)
                return (
                  <div
                    key={voucher.id}
                    className={`flex flex-col gap-3 rounded-2xl border px-4 py-3 shadow-sm sm:flex-row sm:items-center sm:justify-between ${
                      expired
                        ? 'border-border/60 bg-muted/45 text-muted-foreground opacity-75 grayscale'
                        : 'border-orange-200 bg-white/80'
                    }`}
                  >
                    <div>
                      <p className={`font-semibold ${expired ? 'text-muted-foreground' : 'text-orange-700'}`}>{voucher.title}</p>
                      <p className="text-xs text-muted-foreground">满 ¥{voucher.minSpend.toFixed(0)} 可用 · 至 {voucher.expiresAt}</p>
                      {expired ? <p className="mt-1 text-xs font-medium text-muted-foreground">已过期，不能再用于结算</p> : null}
                    </div>
                    <div className="flex items-center justify-between gap-2 sm:justify-end">
                      <Badge className={expired ? 'bg-muted text-muted-foreground' : 'bg-orange-500 text-white'}>
                        {expired ? '已过期' : `×${voucher.remainingCount}`}
                      </Badge>
                      {expired ? (
                        <Button
                          type="button"
                          size="sm"
                          variant="outline"
                          className="cursor-pointer border-border/80 bg-background/80 text-muted-foreground hover:bg-background"
                          onClick={() => onDiscardExpiredVoucher(voucher.id)}
                        >
                          含泪舍弃
                        </Button>
                      ) : null}
                    </div>
                  </div>
                )
              })
            )}
          </CardContent>
        </Card>
        <DeliveryLogoutBar />
      </div>
    )
  }

  if (activePanel === 'favorites') {
    return (
      <div className="space-y-4">
        {backToProfileMain}
        <Card className="border-border/70 bg-card/90 py-0 shadow-sm backdrop-blur-sm">
          <CardHeader className="pb-3">
            <CardTitle className="flex items-center gap-2 text-xl">
              <span className="flex size-9 items-center justify-center rounded-xl bg-primary/10 text-primary">
                <Heart className="size-5" aria-hidden />
              </span>
              我的收藏
            </CardTitle>
            <CardDescription>已收藏 {favoriteMerchants.length} 个商户，{favoriteProducts.length} 个菜品</CardDescription>
          </CardHeader>
          <CardContent className="space-y-5">
            <section className="space-y-3">
              <h3 className="text-base font-semibold text-foreground">商户</h3>
              {favoriteMerchants.length === 0 ? (
                <p className="rounded-xl border border-dashed border-border/70 px-4 py-6 text-sm text-muted-foreground">暂无收藏商户。</p>
              ) : (
                <div className="grid gap-3 md:grid-cols-2">
                  {favoriteMerchants.map((merchant) => (
                    <div key={merchant.id} className="rounded-2xl border border-border/70 bg-background/80 p-4 shadow-sm">
                      <div className="flex items-start justify-between gap-3">
                        <div className="min-w-0">
                          <p className="truncate font-semibold text-foreground">{merchant.storeName}</p>
                          <p className="mt-1 line-clamp-2 text-sm text-muted-foreground">{merchant.address}</p>
                          <Badge variant="outline" className="mt-2 border-primary/25 text-primary">{merchant.category}</Badge>
                        </div>
                        <Button type="button" size="icon" variant="outline" className="size-9 shrink-0 cursor-pointer text-rose-500" onClick={() => onToggleMerchantFavorite(merchant.id)}>
                          <Heart className="size-4 fill-current" aria-hidden />
                        </Button>
                      </div>
                      <Button
                        type="button"
                        size="sm"
                        className="mt-3 cursor-pointer"
                        onClick={() => navigate(`/delivery/customer/merchant/${encodeURIComponent(merchant.id)}`)}
                      >
                        进入店铺
                      </Button>
                    </div>
                  ))}
                </div>
              )}
            </section>
            <section className="space-y-3">
              <h3 className="text-base font-semibold text-foreground">菜品</h3>
              {favoriteProducts.length === 0 ? (
                <p className="rounded-xl border border-dashed border-border/70 px-4 py-6 text-sm text-muted-foreground">暂无收藏菜品。</p>
              ) : (
                <div className="grid gap-3 md:grid-cols-2">
                  {favoriteProducts.map((product) => {
                    const merchant = merchants.find((item) => item.id === product.merchantId)
                    return (
                      <div key={product.id} className="rounded-2xl border border-border/70 bg-background/80 p-4 shadow-sm">
                        <div className="flex items-start justify-between gap-3">
                          <div className="min-w-0">
                            <p className="truncate font-semibold text-foreground">{product.name}</p>
                            <p className="mt-1 text-sm text-muted-foreground">{merchant?.storeName ?? '未知商家'} · ¥{product.price.toFixed(1)}</p>
                            <p className="mt-1 line-clamp-2 text-sm text-muted-foreground">{product.description}</p>
                          </div>
                          <Button type="button" size="icon" variant="outline" className="size-9 shrink-0 cursor-pointer text-rose-500" onClick={() => onToggleProductFavorite(product.id)}>
                            <Heart className="size-4 fill-current" aria-hidden />
                          </Button>
                        </div>
                        <Button
                          type="button"
                          size="sm"
                          className="mt-3 cursor-pointer"
                          onClick={() => navigate(`/delivery/customer/merchant/${encodeURIComponent(product.merchantId)}`)}
                        >
                          去点这道菜
                        </Button>
                      </div>
                    )
                  })}
                </div>
              )}
            </section>
          </CardContent>
        </Card>
        <DeliveryLogoutBar />
      </div>
    )
  }

  if (activePanel === 'pending') {
    return (
      <div className="space-y-4">
        {backToProfileMain}
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
              displayedPendingOrders.map((order) => (
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
                  <p className="mt-1 text-sm text-muted-foreground">实付：¥{order.payableAmount.toFixed(2)} · 预计积分：{Math.floor(order.payableAmount)}</p>
                  {getOrderStatusDescription(order) && (
                    <p className="mt-2 text-xs font-medium text-primary">{getOrderStatusDescription(order)}</p>
                  )}
                  {orderProgressNarratives.get(order.id) && (
                    <div className="mt-3 flex items-center gap-2 rounded-xl border border-primary/15 bg-primary/5 px-3 py-2 text-sm text-primary shadow-sm">
                      <Sparkles className="size-4 shrink-0" aria-hidden />
                      <span>{orderProgressNarratives.get(order.id)}</span>
                    </div>
                  )}
                  <div className="mt-3 flex flex-wrap justify-end gap-2">
                    <Button
                      size="sm"
                      variant="outline"
                      className="relative cursor-pointer border-border/80 transition-colors hover:border-primary/40 hover:bg-primary/5"
                      onClick={() => openChat(order.id, 'merchant')}
                    >
                      联系商家
                      <OrderChatUnreadBadge count={unreadFor(order.id, 'merchant')} />
                    </Button>
                    <Button
                      size="sm"
                      variant="outline"
                      className="relative cursor-pointer border-border/80 transition-colors hover:border-primary/40 hover:bg-primary/5"
                      onClick={() => openChat(order.id, 'rider')}
                    >
                      联系骑手
                      <OrderChatUnreadBadge count={unreadFor(order.id, 'rider')} />
                    </Button>
                    <Button
                      size="sm"
                      variant="outline"
                      className="cursor-pointer border-primary/25 text-primary transition-colors hover:border-primary/45 hover:bg-primary/5"
                      onClick={() => onReorderOrder(order.id)}
                    >
                      <ShoppingCart className="size-4" aria-hidden />
                      再来一单
                    </Button>
                    <Button
                      size="sm"
                      variant="outline"
                      className="cursor-pointer border-border/80 transition-colors hover:border-primary/40 hover:bg-primary/5"
                      onClick={() => onSelectOrder(order.id)}
                    >
                      订单详情
                    </Button>
                  </div>
                </div>
              ))
            )}
            {!showAllPendingOrders && pendingOrders.length > CollapsedListLimit ? (
              <Button
                type="button"
                variant="ghost"
                className="mx-auto flex cursor-pointer text-muted-foreground hover:text-foreground"
                onClick={() => setShowAllPendingOrders(true)}
              >
                更多
                <ChevronDown className="size-4" />
              </Button>
            ) : null}
            {showAllPendingOrders && pendingOrders.length > CollapsedListLimit ? (
              <Button
                type="button"
                variant="ghost"
                className="mx-auto flex cursor-pointer text-muted-foreground hover:text-foreground"
                onClick={() => setShowAllPendingOrders(false)}
              >
                收起
                <ChevronUp className="size-4" />
              </Button>
            ) : null}
          </CardContent>
        </Card>
        <DeliveryLogoutBar />
      </div>
    )
  }

  if (activePanel === 'history') {
    return (
      <div className="space-y-4">
        {backToProfileMain}
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
              displayedHistoryOrders.map((order) => {
                const refundFeedback = getRefundFeedback(order)
                return (
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
                      实付：¥{order.payableAmount.toFixed(2)} · 获得积分：{order.pointsAwarded} · 下单时间：{order.placedAt}
                    </p>
                    {getOrderStatusDescription(order) && (
                      <p className="mt-2 text-xs font-medium text-primary">{getOrderStatusDescription(order)}</p>
                    )}
                    {refundFeedback ? (
                      <div className={`mt-3 rounded-xl border px-3 py-2 text-sm shadow-sm ${refundFeedback.className}`}>
                        <div className="flex flex-wrap items-center gap-2">
                          <span className="font-semibold">{refundFeedback.title}</span>
                          <span className="text-xs opacity-80">状态：{order.refundStatus}</span>
                        </div>
                        <p className="mt-1 leading-5">{refundFeedback.message}</p>
                        {order.refundStatus === RefundStatuses.merchantRejected ? (
                          <Button
                            type="button"
                            size="sm"
                            variant="outline"
                            className="mt-2 border-rose-200 bg-white/70 text-rose-700 hover:bg-white"
                            onClick={() => onAppealRefund(order.id)}
                          >
                            提交管理员仲裁
                          </Button>
                        ) : null}
                      </div>
                    ) : null}
                    {orderProgressNarratives.get(order.id) && (
                      <div className="mt-3 flex items-center gap-2 rounded-xl border border-primary/15 bg-primary/5 px-3 py-2 text-sm text-primary shadow-sm">
                        <Sparkles className="size-4 shrink-0" aria-hidden />
                        <span>{orderProgressNarratives.get(order.id)}</span>
                      </div>
                    )}
                    <div className="mt-3 flex flex-wrap justify-end gap-2">
                      <Button
                        size="sm"
                        variant="outline"
                        className="relative cursor-pointer border-border/80 transition-colors hover:border-primary/40 hover:bg-primary/5"
                        onClick={() => openChat(order.id, 'merchant')}
                      >
                        联系商家
                        <OrderChatUnreadBadge count={unreadFor(order.id, 'merchant')} />
                      </Button>
                      <Button
                        size="sm"
                        variant="outline"
                        className="relative cursor-pointer border-border/80 transition-colors hover:border-primary/40 hover:bg-primary/5"
                        onClick={() => openChat(order.id, 'rider')}
                      >
                        联系骑手
                        <OrderChatUnreadBadge count={unreadFor(order.id, 'rider')} />
                      </Button>
                      {order.status === OrderStatuses.delivered && (
                        <Button
                          size="sm"
                          className="cursor-pointer bg-primary text-primary-foreground transition-[filter] hover:brightness-110"
                          onClick={() => onCompleteOrder(order.id)}
                        >
                          完成订单
                        </Button>
                      )}
                      <Button
                        size="sm"
                        variant="outline"
                        className="cursor-pointer border-border/80 transition-colors hover:border-primary/40 hover:bg-primary/5"
                        onClick={() => onSelectOrder(order.id)}
                      >
                        订单详情
                      </Button>
                    </div>
                  </div>
                )
              })
            )}
            {!showAllHistoryOrders && historyOrders.length > CollapsedListLimit ? (
              <Button
                type="button"
                variant="ghost"
                className="mx-auto flex cursor-pointer text-muted-foreground hover:text-foreground"
                onClick={() => setShowAllHistoryOrders(true)}
              >
                更多
                <ChevronDown className="size-4" />
              </Button>
            ) : null}
            {showAllHistoryOrders && historyOrders.length > CollapsedListLimit ? (
              <Button
                type="button"
                variant="ghost"
                className="mx-auto flex cursor-pointer text-muted-foreground hover:text-foreground"
                onClick={() => setShowAllHistoryOrders(false)}
              >
                收起
                <ChevronUp className="size-4" />
              </Button>
            ) : null}
          </CardContent>
        </Card>
        <DeliveryLogoutBar />
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <section className="grid gap-4 md:grid-cols-2">
        <Card className="relative overflow-hidden border-border/70 bg-gradient-to-br from-primary/15 via-card/95 to-[var(--delivery-brand-blue)]/12 py-0 shadow-[0_20px_55px_rgba(15,23,42,0.08)] backdrop-blur-md dark:shadow-[0_20px_55px_rgba(0,0,0,0.4)]">
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
          <CardHeader className="pb-4">
            <CardDescription>用户名</CardDescription>
            <CardTitle className="flex items-center gap-2 text-xl font-semibold tracking-tight">
              <UserCircle className="size-5 shrink-0 text-primary" aria-hidden />
              <span className="truncate">{username || '—'}</span>
            </CardTitle>
            <p className="text-xs text-muted-foreground">登录账号，用于识别您的顾客身份</p>
          </CardHeader>
        </Card>
      </section>

      <section className="grid gap-4 lg:grid-cols-[1.1fr_0.9fr]">
        <Card className="relative overflow-hidden border-orange-200/70 bg-gradient-to-br from-[#E11D48]/90 via-[#F97316]/90 to-[#FDBA74]/90 py-0 text-white shadow-[0_24px_70px_rgba(249,115,22,0.25)]">
          <div className="pointer-events-none absolute -right-10 -top-10 size-36 rounded-full bg-white/20 blur-sm" />
          <CardHeader className="relative gap-3 pb-3">
            <div className="flex items-center justify-between gap-3">
              <CardDescription className="text-white/80">吃货等级</CardDescription>
              <span className="flex size-10 items-center justify-center rounded-2xl bg-white/20 shadow-inner backdrop-blur-sm">
                <Crown className="size-5" aria-hidden />
              </span>
            </div>
            <CardTitle className="text-3xl font-bold tracking-tight">Lv.{safeFoodieLevel} 美食探索家</CardTitle>
            <p className="text-sm text-white/85">累计 {safeFoodiePoints} 积分 · 每 200 积分升 1 级，升级即得平台满30减10优惠券</p>
          </CardHeader>
          <CardContent className="relative space-y-3 pb-5">
            <Progress value={progress} className="h-2 bg-white/25" />
            <div className="flex items-center justify-between text-xs text-white/85">
              <span>本级进度 {Math.floor(pointsInLevel)}/200</span>
              <span>{pointsToNextLevel === 0 ? '即将升级' : `距下一级还差 ${pointsToNextLevel} 积分`}</span>
            </div>
          </CardContent>
        </Card>

        <Card className="overflow-hidden border-orange-200/70 bg-gradient-to-br from-orange-50/95 via-card to-rose-50/80 py-0 shadow-[0_18px_50px_rgba(249,115,22,0.12)] backdrop-blur-sm">
          <CardHeader className="pb-3">
            <CardTitle className="flex items-center gap-2 text-xl">
              <span className="flex size-9 items-center justify-center rounded-xl bg-orange-100 text-orange-600">
                <TicketPercent className="size-5" aria-hidden />
              </span>
              我的优惠券
            </CardTitle>
            <CardDescription>
              当前可用 {activeVouchers.length} 张，已过期 {expiredVouchers.length} 张，结算时可选择抵扣
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-2 pb-5">
            {displayedVouchers.length === 0 ? (
              <p className="rounded-xl border border-dashed border-orange-200 bg-white/70 px-3 py-3 text-sm text-muted-foreground">
                暂无可用券，完成订单升级后会自动发放平台满30减10优惠券。
              </p>
            ) : (
              displayedVouchers.map((voucher) => {
                const expired = isVoucherExpired(voucher, todayStart)
                return (
                  <div
                    key={voucher.id}
                    className={`flex flex-col gap-3 rounded-2xl border px-4 py-3 shadow-sm sm:flex-row sm:items-center sm:justify-between ${
                      expired
                        ? 'border-border/60 bg-muted/45 text-muted-foreground opacity-75 grayscale'
                        : 'border-orange-200 bg-white/80'
                    }`}
                  >
                    <div>
                      <p className={`font-semibold ${expired ? 'text-muted-foreground' : 'text-orange-700'}`}>{voucher.title}</p>
                      <p className="text-xs text-muted-foreground">满 ¥{voucher.minSpend.toFixed(0)} 可用 · 至 {voucher.expiresAt}</p>
                      {expired ? <p className="mt-1 text-xs font-medium text-muted-foreground">已过期，不能再用于结算</p> : null}
                    </div>
                    <div className="flex items-center justify-between gap-2 sm:justify-end">
                      <Badge className={expired ? 'bg-muted text-muted-foreground' : 'bg-orange-500 text-white'}>
                        {expired ? '已过期' : `×${voucher.remainingCount}`}
                      </Badge>
                      {expired ? (
                        <Button
                          type="button"
                          size="sm"
                          variant="outline"
                          className="cursor-pointer border-border/80 bg-background/80 text-muted-foreground hover:bg-background"
                          onClick={() => onDiscardExpiredVoucher(voucher.id)}
                        >
                          含泪舍弃
                        </Button>
                      ) : null}
                    </div>
                  </div>
                )
              })
            )}
          </CardContent>
        </Card>
      </section>

      <DeliveryContactsSection />

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

      <Card className="relative overflow-hidden border-border/70 bg-gradient-to-br from-[#6366F1]/10 via-card/95 to-[#8B5CF6]/8 py-0 shadow-[0_20px_55px_rgba(99,102,241,0.08)] backdrop-blur-md dark:shadow-[0_20px_55px_rgba(0,0,0,0.4)]">
        <div className="pointer-events-none absolute -right-6 -top-8 h-28 w-28 rounded-full bg-[radial-gradient(circle,oklch(0.75_0.15_280/0.3),transparent_65%)]" />
        <CardHeader className="relative gap-1 pb-2">
          <CardTitle className="flex items-center gap-2 text-xl">
            <span className="flex size-9 items-center justify-center rounded-xl bg-gradient-to-br from-[#6366F1] to-[#8B5CF6] text-white shadow-[0_4px_14px_rgba(99,102,241,0.4)]">
              <Sparkles className="size-5" aria-hidden />
            </span>
            AI 饮食周报
          </CardTitle>
          <CardDescription>基于近 7 天订单智能分析</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          {!aiDietReport && !aiDietReportLoading && !aiDietReportError && (
            <div className="flex flex-col items-center gap-3 py-4">
              <p className="text-sm text-muted-foreground">点击下方按钮，AI 将为您生成饮食周报</p>
              <Button
                className="cursor-pointer bg-gradient-to-r from-[#6366F1] to-[#8B5CF6] text-white shadow-[0_6px_20px_rgba(99,102,241,0.35)] transition-[filter,box-shadow] duration-200 hover:brightness-110 hover:shadow-[0_8px_24px_rgba(99,102,241,0.45)]"
                onClick={onGenerateAIDietReport}
              >
                <Sparkles className="mr-2 size-4" aria-hidden />
                生成我的周报
              </Button>
            </div>
          )}

          {aiDietReportLoading && (
            <div className="flex flex-col items-center gap-3 py-6">
              <Loader2 className="size-8 animate-spin text-[#6366F1]" aria-hidden />
              <p className="text-sm text-muted-foreground">AI 正在分析您的饮食数据...</p>
            </div>
          )}

          {aiDietReportError && !aiDietReportLoading && (
            <div className="flex flex-col items-center gap-3 py-4">
              <p className="text-sm text-destructive">{aiDietReportError}</p>
              <Button
                variant="outline"
                className="cursor-pointer border-[#6366F1]/30 text-[#6366F1] transition-colors hover:border-[#6366F1]/60 hover:bg-[#6366F1]/5"
                onClick={onGenerateAIDietReport}
              >
                重新生成
              </Button>
            </div>
          )}

          {aiDietReport && !aiDietReportLoading && (
            <>
              <section className="grid grid-cols-2 gap-3 sm:grid-cols-4">
                <div className="rounded-xl border border-border/60 bg-gradient-to-br from-card to-secondary/20 p-3 text-center shadow-sm">
                  <p className="text-xs text-muted-foreground">估算总热量</p>
                  <p className="mt-1 text-base font-semibold text-foreground">{aiDietReport.summary.calorieTotal}</p>
                </div>
                <div className="rounded-xl border border-border/60 bg-gradient-to-br from-card to-secondary/20 p-3 text-center shadow-sm">
                  <p className="text-xs text-muted-foreground">订单数</p>
                  <p className="mt-1 text-base font-semibold tabular-nums text-foreground">{aiDietReport.summary.orderCount}</p>
                </div>
                <div className="rounded-xl border border-border/60 bg-gradient-to-br from-card to-secondary/20 p-3 text-center shadow-sm">
                  <p className="text-xs text-muted-foreground">最爱品类</p>
                  <p className="mt-1 text-base font-semibold text-foreground">{aiDietReport.summary.topCategory}</p>
                </div>
                <div className="rounded-xl border border-border/60 bg-gradient-to-br from-card to-secondary/20 p-3 text-center shadow-sm">
                  <p className="text-xs text-muted-foreground">常点商家</p>
                  <p className="mt-1 text-base font-semibold text-foreground">{aiDietReport.summary.topMerchant}</p>
                </div>
              </section>

              {aiDietReport.nutritionAnalysis.length > 0 && (
                <section className="space-y-2">
                  <h4 className="text-sm font-medium text-foreground">营养分析</h4>
                  <div className="space-y-2">
                    {aiDietReport.nutritionAnalysis.map((item) => (
                      <div
                        key={item.name}
                        className="flex items-center justify-between rounded-xl border border-border/60 bg-card/80 px-4 py-2.5 shadow-sm"
                      >
                        <span className="text-sm font-medium text-foreground">{item.name}</span>
                        <div className="flex items-center gap-3">
                          <span className="text-sm text-muted-foreground">{item.amount}</span>
                          <Badge
                            variant="outline"
                            className={
                              item.assessment === '良好'
                                ? 'border-emerald-400/40 text-emerald-600 dark:text-emerald-400'
                                : item.assessment === '偏高'
                                  ? 'border-amber-400/40 text-amber-600 dark:text-amber-400'
                                  : item.assessment === '偏低'
                                    ? 'border-red-400/40 text-red-600 dark:text-red-400'
                                    : 'border-border/40 text-muted-foreground'
                            }
                          >
                            {item.assessment}
                          </Badge>
                        </div>
                      </div>
                    ))}
                  </div>
                </section>
              )}

              {aiDietReport.suggestions.length > 0 && (
                <section className="space-y-2">
                  <h4 className="text-sm font-medium text-foreground">饮食建议</h4>
                  <ol className="space-y-1.5 pl-4">
                    {aiDietReport.suggestions.map((suggestion, index) => (
                      <li key={index} className="text-sm text-muted-foreground list-decimal">
                        {suggestion}
                      </li>
                    ))}
                  </ol>
                </section>
              )}

              {aiDietReport.weeklyTrend && (
                <section className="space-y-2">
                  <h4 className="flex items-center gap-1.5 text-sm font-medium text-foreground">
                    <TrendingUp className="size-4 text-[#6366F1]" aria-hidden />
                    本周趋势
                  </h4>
                  <p className="text-sm leading-relaxed text-muted-foreground">{aiDietReport.weeklyTrend}</p>
                </section>
              )}

              <div className="flex items-center justify-between pt-1">
                <p className="text-xs text-muted-foreground">报告生成于 {aiDietReport.generatedAt}</p>
                <Button
                  size="sm"
                  variant="outline"
                  className="cursor-pointer border-[#6366F1]/30 text-[#6366F1] transition-colors hover:border-[#6366F1]/60 hover:bg-[#6366F1]/5"
                  onClick={onGenerateAIDietReport}
                >
                  刷新周报
                </Button>
              </div>
            </>
          )}
        </CardContent>
      </Card>

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
            displayedPendingOrders.map((order) => (
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
                <p className="mt-1 text-sm text-muted-foreground">实付：¥{order.payableAmount.toFixed(2)} · 预计积分：{Math.floor(order.payableAmount)}</p>
                {getOrderStatusDescription(order) && (
                  <p className="mt-2 text-xs font-medium text-primary">{getOrderStatusDescription(order)}</p>
                )}
                {orderProgressNarratives.get(order.id) && (
                  <div className="mt-3 flex items-center gap-2 rounded-xl border border-primary/15 bg-primary/5 px-3 py-2 text-sm text-primary shadow-sm">
                    <Sparkles className="size-4 shrink-0" aria-hidden />
                    <span>{orderProgressNarratives.get(order.id)}</span>
                  </div>
                )}
                <div className="mt-3 flex justify-end">
                  <Button
                    size="sm"
                    variant="outline"
                    className="relative mr-2 cursor-pointer border-border/80 transition-colors hover:border-primary/40 hover:bg-primary/5"
                    onClick={() => openChat(order.id, 'merchant')}
                  >
                    联系商家
                    <OrderChatUnreadBadge count={unreadFor(order.id, 'merchant')} />
                  </Button>
                  <Button
                    size="sm"
                    variant="outline"
                    className="relative mr-2 cursor-pointer border-border/80 transition-colors hover:border-primary/40 hover:bg-primary/5"
                    onClick={() => openChat(order.id, 'rider')}
                  >
                    联系骑手
                    <OrderChatUnreadBadge count={unreadFor(order.id, 'rider')} />
                  </Button>
                  <Button
                    size="sm"
                    variant="outline"
                    className="cursor-pointer border-border/80 transition-colors hover:border-primary/40 hover:bg-primary/5"
                    onClick={() => onSelectOrder(order.id)}
                  >
                    订单详情
                  </Button>
                </div>
              </div>
            ))
          )}
          {!showAllPendingOrders && pendingOrders.length > CollapsedListLimit ? (
            <Button
              type="button"
              variant="ghost"
              className="mx-auto flex cursor-pointer text-muted-foreground hover:text-foreground"
              onClick={() => setShowAllPendingOrders(true)}
            >
              更多
              <ChevronDown className="size-4" />
            </Button>
          ) : null}
          {showAllPendingOrders && pendingOrders.length > CollapsedListLimit ? (
            <Button
              type="button"
              variant="ghost"
              className="mx-auto flex cursor-pointer text-muted-foreground hover:text-foreground"
              onClick={() => setShowAllPendingOrders(false)}
            >
              收起
              <ChevronUp className="size-4" />
            </Button>
          ) : null}
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
            displayedHistoryOrders.map((order) => {
              const refundFeedback = getRefundFeedback(order)
              return (
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
                    实付：¥{order.payableAmount.toFixed(2)} · 获得积分：{order.pointsAwarded} · 下单时间：{order.placedAt}
                  </p>
                  {getOrderStatusDescription(order) && (
                    <p className="mt-2 text-xs font-medium text-primary">{getOrderStatusDescription(order)}</p>
                  )}
                  {refundFeedback ? (
                    <div className={`mt-3 rounded-xl border px-3 py-2 text-sm shadow-sm ${refundFeedback.className}`}>
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="font-semibold">{refundFeedback.title}</span>
                        <span className="text-xs opacity-80">状态：{order.refundStatus}</span>
                      </div>
                      <p className="mt-1 leading-5">{refundFeedback.message}</p>
                      {order.refundStatus === RefundStatuses.merchantRejected ? (
                        <Button
                          type="button"
                          size="sm"
                          variant="outline"
                          className="mt-2 border-rose-200 bg-white/70 text-rose-700 hover:bg-white"
                          onClick={() => onAppealRefund(order.id)}
                        >
                          提交管理员仲裁
                        </Button>
                      ) : null}
                    </div>
                  ) : null}
                  {orderProgressNarratives.get(order.id) && (
                    <div className="mt-3 flex items-center gap-2 rounded-xl border border-primary/15 bg-primary/5 px-3 py-2 text-sm text-primary shadow-sm">
                      <Sparkles className="size-4 shrink-0" aria-hidden />
                      <span>{orderProgressNarratives.get(order.id)}</span>
                    </div>
                  )}
                  <div className="mt-3 flex justify-end gap-2">
                    <Button
                      size="sm"
                      variant="outline"
                      className="relative cursor-pointer border-border/80 transition-colors hover:border-primary/40 hover:bg-primary/5"
                      onClick={() => openChat(order.id, 'merchant')}
                    >
                      联系商家
                      <OrderChatUnreadBadge count={unreadFor(order.id, 'merchant')} />
                    </Button>
                    <Button
                      size="sm"
                      variant="outline"
                      className="relative cursor-pointer border-border/80 transition-colors hover:border-primary/40 hover:bg-primary/5"
                      onClick={() => openChat(order.id, 'rider')}
                    >
                      联系骑手
                      <OrderChatUnreadBadge count={unreadFor(order.id, 'rider')} />
                    </Button>
                    {order.status === OrderStatuses.delivered && (
                      <Button
                        size="sm"
                        className="cursor-pointer bg-primary text-primary-foreground transition-[filter] hover:brightness-110"
                        onClick={() => onCompleteOrder(order.id)}
                      >
                        完成订单
                      </Button>
                    )}
                    <Button
                      size="sm"
                      variant="outline"
                      className="cursor-pointer border-primary/25 text-primary transition-colors hover:border-primary/45 hover:bg-primary/5"
                      onClick={() => onReorderOrder(order.id)}
                    >
                      <ShoppingCart className="size-4" aria-hidden />
                      再来一单
                    </Button>
                    <Button
                      size="sm"
                      variant="outline"
                      className="cursor-pointer border-border/80 transition-colors hover:border-primary/40 hover:bg-primary/5"
                      onClick={() => onSelectOrder(order.id)}
                    >
                      订单详情
                    </Button>
                  </div>
                </div>
              )
            })
          )}
          {!showAllHistoryOrders && historyOrders.length > CollapsedListLimit ? (
            <Button
              type="button"
              variant="ghost"
              className="mx-auto flex cursor-pointer text-muted-foreground hover:text-foreground"
              onClick={() => setShowAllHistoryOrders(true)}
            >
              更多
              <ChevronDown className="size-4" />
            </Button>
          ) : null}
          {showAllHistoryOrders && historyOrders.length > CollapsedListLimit ? (
            <Button
              type="button"
              variant="ghost"
              className="mx-auto flex cursor-pointer text-muted-foreground hover:text-foreground"
              onClick={() => setShowAllHistoryOrders(false)}
            >
              收起
              <ChevronUp className="size-4" />
            </Button>
          ) : null}
        </CardContent>
      </Card>

      <DeliveryLogoutBar />
    </div>
  )
}
