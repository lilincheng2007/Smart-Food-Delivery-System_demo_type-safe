import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'

import { DeliveryLogoutBar } from '@/components/DeliveryLogoutBar'
import { DeliveryPageShell } from '@/components/DeliveryPageShell'
import { Card, CardContent } from '@/components/ui/card'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { useAppChrome } from '@/hooks/useAppChrome'
import { useCustomerPortalStore } from '@/pages/CustomerPortal/stores/use-customer-portal-store'

import { CartTab } from './components/CartTab'
import { HomeTab } from './components/HomeTab'
import { OrderDetailDialog } from './components/OrderDetailDialog'
import { OrderRefundDialog } from './components/OrderRefundDialog'
import { OrderReviewDialog } from './components/OrderReviewDialog'
import { ProfileTab } from './components/ProfileTab'
import { RechargeDialog } from './components/RechargeDialog'
import { isCustomerTab } from './functions/helpers'

const CustomerPortalRefreshIntervalMs = 15_000

export default function CustomerPortal() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const { showNotice } = useAppChrome()
  const bootstrapDone = useCustomerPortalStore((state) => state.bootstrapDone)
  const loadError = useCustomerPortalStore((state) => state.loadError)
  const customerAccount = useCustomerPortalStore((state) => state.customerAccount)
  const merchants = useCustomerPortalStore((state) => state.merchants)
  const products = useCustomerPortalStore((state) => state.products)
  const activeTab = useCustomerPortalStore((state) => state.activeTab)
  const cartLines = useCustomerPortalStore((state) => state.cartLines)
  const favorites = useCustomerPortalStore((state) => state.favorites)
  const walletBalance = useCustomerPortalStore((state) => state.walletBalance)
  const pendingOrders = useCustomerPortalStore((state) => state.pendingOrders)
  const historyOrders = useCustomerPortalStore((state) => state.historyOrders)
  const isRechargeOpen = useCustomerPortalStore((state) => state.isRechargeOpen)
  const rechargeAmountInput = useCustomerPortalStore((state) => state.rechargeAmountInput)
  const selectedOrder = useCustomerPortalStore((state) => state.selectedOrder)
  const reviewTargetOrder = useCustomerPortalStore((state) => state.reviewTargetOrder)
  const [refundTargetOrder, setRefundTargetOrder] = useState<typeof selectedOrder>(null)
  const bootstrap = useCustomerPortalStore((state) => state.bootstrap)
  const setActiveTab = useCustomerPortalStore((state) => state.setActiveTab)
  const toggleFavorite = useCustomerPortalStore((state) => state.toggleFavorite)
  const reorderOrder = useCustomerPortalStore((state) => state.reorderOrder)
  const changeQuantity = useCustomerPortalStore((state) => state.changeQuantity)
  const changeCartLineQuantity = useCustomerPortalStore((state) => state.changeCartLineQuantity)
  const setIsRechargeOpen = useCustomerPortalStore((state) => state.setIsRechargeOpen)
  const setRechargeAmountInput = useCustomerPortalStore((state) => state.setRechargeAmountInput)
  const setSelectedOrder = useCustomerPortalStore((state) => state.setSelectedOrder)
  const setReviewTargetOrder = useCustomerPortalStore((state) => state.setReviewTargetOrder)
  const openOrderDetail = useCustomerPortalStore((state) => state.openOrderDetail)
  const cancelOrder = useCustomerPortalStore((state) => state.cancelOrder)
  const completeOrder = useCustomerPortalStore((state) => state.completeOrder)
  const refreshPortal = useCustomerPortalStore((state) => state.refreshPortal)
  const recharge = useCustomerPortalStore((state) => state.recharge)
  const aiDietReport = useCustomerPortalStore((state) => state.aiDietReport)
  const aiDietReportLoading = useCustomerPortalStore((state) => state.aiDietReportLoading)
  const aiDietReportError = useCustomerPortalStore((state) => state.aiDietReportError)
  const aiOrderProgressNarratives = useCustomerPortalStore((state) => state.aiOrderProgressNarratives)
  const generateAIDietReport = useCustomerPortalStore((state) => state.generateAIDietReport)
  const ensureAIOrderProgressNarratives = useCustomerPortalStore((state) => state.ensureAIOrderProgressNarratives)
  const discardExpiredVoucher = useCustomerPortalStore((state) => state.discardExpiredVoucher)
  const uploadReviewImage = useCustomerPortalStore((state) => state.uploadReviewImage)
  const uploadRefundImage = useCustomerPortalStore((state) => state.uploadRefundImage)
  const submitReview = useCustomerPortalStore((state) => state.submitReview)
  const requestRefund = useCustomerPortalStore((state) => state.requestRefund)
  const appealRefund = useCustomerPortalStore((state) => state.appealRefund)

  useEffect(() => {
    void (async () => {
      const st = useCustomerPortalStore.getState()
      if (!st.bootstrapDone) {
        await bootstrap()
      } else {
        await refreshPortal().catch(() => {})
        await ensureAIOrderProgressNarratives().catch(() => {})
      }
    })()
  }, [bootstrap, ensureAIOrderProgressNarratives, refreshPortal])

  useEffect(() => {
    const tab = searchParams.get('tab')
    if (tab && isCustomerTab(tab)) {
      setActiveTab(tab)
    }
  }, [searchParams, setActiveTab])

  useEffect(() => {
    let stopped = false
    let timer: number | undefined

    const scheduleRefresh = () => {
      if (!stopped) {
        timer = window.setTimeout(() => {
          void tick()
        }, CustomerPortalRefreshIntervalMs)
      }
    }

    const tick = async () => {
      if (stopped) {
        return
      }
      if (!document.hidden) {
        await refreshPortal().catch(() => {})
      }
      scheduleRefresh()
    }

    const handleVisibilityChange = () => {
      if (!document.hidden) {
        void refreshPortal().catch(() => {})
        void ensureAIOrderProgressNarratives().catch(() => {})
      }
    }

    scheduleRefresh()
    document.addEventListener('visibilitychange', handleVisibilityChange)

    return () => {
      stopped = true
      if (timer !== undefined) {
        window.clearTimeout(timer)
      }
      document.removeEventListener('visibilitychange', handleVisibilityChange)
    }
  }, [ensureAIOrderProgressNarratives, refreshPortal])

  const handleRechargeConfirm = async () => {
    const result = await recharge()
    if (result.ok) {
      showNotice(`充值成功，到账 ¥${result.amount.toFixed(2)}。`, 'success')
      return
    }

    showNotice(result.message, 'error')
  }

  const handleOpenOrderDetail = async (orderId: string) => {
    const result = await openOrderDetail(orderId)
    if (!result.ok) {
      showNotice(result.message, 'error')
    }
  }

  const handleCancelOrder = async (orderId: string) => {
    const result = await cancelOrder(orderId)
    if (result.ok) {
      showNotice('订单已取消，金额已退回钱包。', 'success')
      return
    }

    showNotice(result.message, 'error')
  }

  const handleCompleteOrder = async (orderId: string) => {
    const result = await completeOrder(orderId)
    if (result.ok) {
      showNotice('订单已确认完成，吃货积分已到账。', 'success')
      return
    }

    showNotice(result.message, 'error')
  }

  const handleGenerateAIDietReport = async () => {
    const result = await generateAIDietReport()
    if (!result.ok) {
      showNotice(result.message, 'error')
    }
  }

  const handleDiscardExpiredVoucher = async (voucherId: string) => {
    const result = await discardExpiredVoucher(voucherId)
    if (result.ok) {
      showNotice('这张过期券已含泪舍弃。', 'success')
      return
    }

    showNotice(result.message, 'error')
  }

  const handleAppealRefund = async (orderId: string) => {
    const result = await appealRefund(orderId)
    if (result.ok) {
      showNotice('已提交管理员仲裁，请等待处理结果。', 'success')
      return
    }

    showNotice(result.message, 'error')
  }

  const handleReorderOrder = (orderId: string) => {
    const result = reorderOrder(orderId)
    if (result.ok) {
      showNotice(`已将 ${result.addedCount} 件菜品加入购物车。`, 'success')
      return
    }

    showNotice(result.message, 'error')
  }

  if (!bootstrapDone) {
    return (
      <DeliveryPageShell>
        <Card className="border-border/70 bg-card/90 backdrop-blur-sm">
          <CardContent className="p-6 text-sm text-muted-foreground">加载中…</CardContent>
        </Card>
        <DeliveryLogoutBar />
      </DeliveryPageShell>
    )
  }

  if (loadError) {
    return (
      <DeliveryPageShell>
        <Card className="border-border/70 bg-card/90 backdrop-blur-sm">
          <CardContent className="p-6 text-sm text-destructive">{loadError}</CardContent>
        </Card>
        <DeliveryLogoutBar />
      </DeliveryPageShell>
    )
  }

  if (!customerAccount) {
    return (
      <DeliveryPageShell>
        <Card className="border-border/70 bg-card/90 backdrop-blur-sm">
          <CardContent className="p-6 text-sm text-muted-foreground">未找到当前顾客账号档案。</CardContent>
        </Card>
        <DeliveryLogoutBar />
      </DeliveryPageShell>
    )
  }

  return (
    <DeliveryPageShell>
      <Tabs
        value={activeTab}
        onValueChange={(value) => {
          if (isCustomerTab(value)) {
            setActiveTab(value)
          }
        }}
      >
        <Card className="border-border/70 bg-card/85 py-0 shadow-[0_18px_50px_rgba(15,23,42,0.06)] backdrop-blur-md dark:shadow-[0_18px_50px_rgba(0,0,0,0.35)]">
          <CardContent className="px-4 py-4 sm:px-5 sm:py-5">
            <TabsList className="grid h-12 w-full grid-cols-3 gap-1 rounded-2xl border border-border/60 bg-gradient-to-br from-secondary/90 to-background/90 p-1 shadow-inner">
              <TabsTrigger
                value="home"
                className="cursor-pointer rounded-xl text-sm font-semibold transition-[color,background-color,box-shadow] duration-200 data-[state=active]:bg-gradient-to-r data-[state=active]:from-primary data-[state=active]:to-[oklch(0.62_0.18_45)] data-[state=active]:text-primary-foreground data-[state=active]:shadow-[0_12px_28px_rgba(225,29,72,0.28)] dark:data-[state=active]:to-[oklch(0.68_0.14_45)]"
              >
                首页
              </TabsTrigger>
              <TabsTrigger
                value="cart"
                className="cursor-pointer rounded-xl text-sm font-semibold transition-[color,background-color,box-shadow] duration-200 data-[state=active]:bg-gradient-to-r data-[state=active]:from-primary data-[state=active]:to-[oklch(0.62_0.18_45)] data-[state=active]:text-primary-foreground data-[state=active]:shadow-[0_12px_28px_rgba(225,29,72,0.28)] dark:data-[state=active]:to-[oklch(0.68_0.14_45)]"
              >
                购物车
              </TabsTrigger>
              <TabsTrigger
                value="profile"
                className="cursor-pointer rounded-xl text-sm font-semibold transition-[color,background-color,box-shadow] duration-200 data-[state=active]:bg-gradient-to-r data-[state=active]:from-primary data-[state=active]:to-[oklch(0.62_0.18_45)] data-[state=active]:text-primary-foreground data-[state=active]:shadow-[0_12px_28px_rgba(225,29,72,0.28)] dark:data-[state=active]:to-[oklch(0.68_0.14_45)]"
              >
                我的
              </TabsTrigger>
            </TabsList>
          </CardContent>
        </Card>

        <TabsContent value="home">
          <HomeTab merchants={merchants} products={products} />
        </TabsContent>

        <TabsContent value="cart">
          <CartTab
            merchants={merchants}
            products={products}
            cartLines={cartLines}
            onChangeQuantity={changeQuantity}
            onChangeCartLineQuantity={changeCartLineQuantity}
            onCheckout={() => navigate('/delivery/customer/checkout')}
          />
        </TabsContent>

        <TabsContent value="profile">
          <ProfileTab
            username={customerAccount.username}
            walletBalance={walletBalance}
            merchants={merchants}
            products={products}
            pendingOrders={pendingOrders}
            historyOrders={historyOrders}
            vouchers={customerAccount.profile.vouchers}
            foodiePoints={customerAccount.profile.foodiePoints}
            foodieLevel={customerAccount.profile.foodieLevel}
            favoriteMerchantIds={favorites.merchantIds}
            favoriteProductIds={favorites.productIds}
            aiDietReport={aiDietReport}
            aiDietReportLoading={aiDietReportLoading}
            aiDietReportError={aiDietReportError}
            aiOrderProgressNarratives={aiOrderProgressNarratives}
            onOpenRecharge={() => setIsRechargeOpen(true)}
            onSelectOrder={(orderId) => void handleOpenOrderDetail(orderId)}
            onCompleteOrder={(orderId) => void handleCompleteOrder(orderId)}
            onAppealRefund={(orderId) => void handleAppealRefund(orderId)}
            onReorderOrder={handleReorderOrder}
            onGenerateAIDietReport={() => void handleGenerateAIDietReport()}
            onDiscardExpiredVoucher={(voucherId) => void handleDiscardExpiredVoucher(voucherId)}
            onToggleMerchantFavorite={(merchantId) => toggleFavorite('merchant', merchantId)}
            onToggleProductFavorite={(productId) => toggleFavorite('product', productId)}
          />
        </TabsContent>
      </Tabs>

      <RechargeDialog
        open={isRechargeOpen}
        amountInput={rechargeAmountInput}
        onOpenChange={setIsRechargeOpen}
        onAmountChange={setRechargeAmountInput}
        onConfirm={handleRechargeConfirm}
      />
      <OrderDetailDialog
        selectedOrder={selectedOrder}
        onOpenChange={(open) => !open && setSelectedOrder(null)}
        onClose={() => setSelectedOrder(null)}
        onCancelOrder={(order) => void handleCancelOrder(order.id)}
        onCompleteOrder={(order) => void handleCompleteOrder(order.id)}
        onReviewOrder={(order) => setReviewTargetOrder(order)}
        onRefundOrder={(order) => setRefundTargetOrder(order)}
        onAppealRefund={(order) => void handleAppealRefund(order.id)}
      />
      <OrderRefundDialog
        order={refundTargetOrder}
        onOpenChange={(open) => !open && setRefundTargetOrder(null)}
        onUploadImage={uploadRefundImage}
        onSubmitRefund={requestRefund}
        onNotice={(message, type) => showNotice(message, type)}
      />
      <OrderReviewDialog
        order={reviewTargetOrder}
        onOpenChange={(open) => !open && setReviewTargetOrder(null)}
        onUploadImage={uploadReviewImage}
        onSubmitReview={submitReview}
        onNotice={(message, type) => showNotice(message, type)}
      />
    </DeliveryPageShell>
  )
}
