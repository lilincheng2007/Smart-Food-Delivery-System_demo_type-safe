import { useEffect } from 'react'

import { DeliveryPageShell } from '@/components/DeliveryPageShell'
import { Card, CardContent } from '@/components/ui/card'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { useAppChrome } from '@/hooks/useAppChrome'
import type { MerchantId, ProductId } from '@/objects/shared'
import { useCustomerPortalStore } from '@/stores/pages/use-customer-portal-store'

import { CartTab } from './CartTab'
import { HomeTab } from './HomeTab'
import { OrderDetailDialog } from './OrderDetailDialog'
import { ProfileTab } from './ProfileTab'
import { RechargeDialog } from './RechargeDialog'
import { isCustomerTab } from './helpers'

export default function CustomerPortal() {
  const { showNotice } = useAppChrome()
  const bootstrapDone = useCustomerPortalStore((state) => state.bootstrapDone)
  const loadError = useCustomerPortalStore((state) => state.loadError)
  const customerAccount = useCustomerPortalStore((state) => state.customerAccount)
  const merchants = useCustomerPortalStore((state) => state.merchants)
  const products = useCustomerPortalStore((state) => state.products)
  const activeTab = useCustomerPortalStore((state) => state.activeTab)
  const selectedMerchantId = useCustomerPortalStore((state) => state.selectedMerchantId)
  const cartLines = useCustomerPortalStore((state) => state.cartLines)
  const walletBalance = useCustomerPortalStore((state) => state.walletBalance)
  const pendingOrders = useCustomerPortalStore((state) => state.pendingOrders)
  const historyOrders = useCustomerPortalStore((state) => state.historyOrders)
  const isRechargeOpen = useCustomerPortalStore((state) => state.isRechargeOpen)
  const rechargeAmountInput = useCustomerPortalStore((state) => state.rechargeAmountInput)
  const selectedOrder = useCustomerPortalStore((state) => state.selectedOrder)
  const resetPage = useCustomerPortalStore((state) => state.resetPage)
  const bootstrap = useCustomerPortalStore((state) => state.bootstrap)
  const setActiveTab = useCustomerPortalStore((state) => state.setActiveTab)
  const setSelectedMerchantId = useCustomerPortalStore((state) => state.setSelectedMerchantId)
  const addProductToCart = useCustomerPortalStore((state) => state.addProductToCart)
  const changeQuantity = useCustomerPortalStore((state) => state.changeQuantity)
  const setIsRechargeOpen = useCustomerPortalStore((state) => state.setIsRechargeOpen)
  const setRechargeAmountInput = useCustomerPortalStore((state) => state.setRechargeAmountInput)
  const setSelectedOrder = useCustomerPortalStore((state) => state.setSelectedOrder)
  const checkout = useCustomerPortalStore((state) => state.checkout)
  const recharge = useCustomerPortalStore((state) => state.recharge)

  useEffect(() => {
    resetPage()
    void bootstrap()
  }, [bootstrap, resetPage])

  const handleAddProductToCart = (merchantId: MerchantId, productId: ProductId) => {
    addProductToCart(merchantId, productId)
    showNotice('已加入购物车。', 'success')
  }

  const handleCheckout = async () => {
    const result = await checkout()
    if (result.ok) {
      showNotice(`结算成功，已创建 ${result.createdCount} 笔待收货订单。`, 'success')
      return
    }

    showNotice(result.message, 'error')
  }

  const handleRechargeConfirm = async () => {
    const result = await recharge()
    if (result.ok) {
      showNotice(`充值成功，到账 ¥${result.amount.toFixed(2)}。`, 'success')
      return
    }

    showNotice(result.message, 'error')
  }

  if (!bootstrapDone) {
    return (
      <DeliveryPageShell title="顾客端核心功能" description="正在加载数据…" roleBadge="顾客 APP">
        <Card className="border-orange-100 bg-white/95">
          <CardContent className="p-6 text-sm text-slate-600">加载中…</CardContent>
        </Card>
      </DeliveryPageShell>
    )
  }

  if (loadError) {
    return (
      <DeliveryPageShell title="顾客端核心功能" description="数据加载失败" roleBadge="顾客 APP">
        <Card className="border-orange-100 bg-white/95">
          <CardContent className="p-6 text-sm text-rose-600">{loadError}</CardContent>
        </Card>
      </DeliveryPageShell>
    )
  }

  if (!customerAccount) {
    return (
      <DeliveryPageShell
        title="顾客端核心功能"
        description="当前账号未绑定顾客信息，请先完成顾客注册资料。"
        roleBadge="顾客 APP"
      >
        <Card className="border-orange-100 bg-white/95">
          <CardContent className="p-6 text-sm text-slate-600">未找到当前顾客账号档案。</CardContent>
        </Card>
      </DeliveryPageShell>
    )
  }

  return (
    <DeliveryPageShell
      title="顾客端核心功能"
      description="覆盖注册登录、定位、浏览搜索、购物车、下单支付、订单状态和配送轨迹等功能。"
      roleBadge="顾客 APP"
    >
      <Tabs
        value={activeTab}
        onValueChange={(value) => {
          if (isCustomerTab(value)) {
            setActiveTab(value)
          }
        }}
      >
        <Card className="border-orange-100 bg-white/95 py-0">
          <CardContent className="px-4 py-4">
            <TabsList className="grid h-11 w-full grid-cols-3 rounded-xl bg-orange-50 p-1">
              <TabsTrigger value="home" className="rounded-lg">
                首页
              </TabsTrigger>
              <TabsTrigger value="cart" className="rounded-lg">
                购物车
              </TabsTrigger>
              <TabsTrigger value="profile" className="rounded-lg">
                我的
              </TabsTrigger>
            </TabsList>
          </CardContent>
        </Card>

        <TabsContent value="home">
          <HomeTab
            customerAccount={customerAccount}
            merchants={merchants}
            products={products}
            selectedMerchantId={selectedMerchantId}
            cartLines={cartLines}
            onSelectMerchant={setSelectedMerchantId}
            onAddProductToCart={handleAddProductToCart}
          />
        </TabsContent>

        <TabsContent value="cart">
          <CartTab
            merchants={merchants}
            products={products}
            cartLines={cartLines}
            onChangeQuantity={changeQuantity}
            onCheckout={handleCheckout}
          />
        </TabsContent>

        <TabsContent value="profile">
          <ProfileTab
            walletBalance={walletBalance}
            pendingOrders={pendingOrders}
            historyOrders={historyOrders}
            onOpenRecharge={() => setIsRechargeOpen(true)}
            onSelectOrder={setSelectedOrder}
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
      />
    </DeliveryPageShell>
  )
}
