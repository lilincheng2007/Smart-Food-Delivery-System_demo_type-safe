import { useEffect } from 'react'

import { DeliveryLogoutBar } from '@/components/DeliveryLogoutBar'
import { DeliveryPageShell } from '@/components/DeliveryPageShell'
import { Card, CardContent } from '@/components/ui/card'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { useAppChrome } from '@/hooks/useAppChrome'
import type { MerchantId, ProductId } from '@/objects/shared'
import { useCustomerPortalStore } from '@/stores/pages/use-customer-portal-store'

import { CartTab } from './CartTab'
import { EditProfileDialog } from './EditProfileDialog'
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
  const isEditProfileOpen = useCustomerPortalStore((state) => state.isEditProfileOpen)
  const rechargeAmountInput = useCustomerPortalStore((state) => state.rechargeAmountInput)
  const profileDraft = useCustomerPortalStore((state) => state.profileDraft)
  const selectedOrder = useCustomerPortalStore((state) => state.selectedOrder)
  const resetPage = useCustomerPortalStore((state) => state.resetPage)
  const bootstrap = useCustomerPortalStore((state) => state.bootstrap)
  const setActiveTab = useCustomerPortalStore((state) => state.setActiveTab)
  const setSelectedMerchantId = useCustomerPortalStore((state) => state.setSelectedMerchantId)
  const addProductToCart = useCustomerPortalStore((state) => state.addProductToCart)
  const changeQuantity = useCustomerPortalStore((state) => state.changeQuantity)
  const setIsRechargeOpen = useCustomerPortalStore((state) => state.setIsRechargeOpen)
  const openEditProfile = useCustomerPortalStore((state) => state.openEditProfile)
  const setIsEditProfileOpen = useCustomerPortalStore((state) => state.setIsEditProfileOpen)
  const setProfileDraftField = useCustomerPortalStore((state) => state.setProfileDraftField)
  const setRechargeAmountInput = useCustomerPortalStore((state) => state.setRechargeAmountInput)
  const setSelectedOrder = useCustomerPortalStore((state) => state.setSelectedOrder)
  const refreshPortal = useCustomerPortalStore((state) => state.refreshPortal)
  const checkout = useCustomerPortalStore((state) => state.checkout)
  const recharge = useCustomerPortalStore((state) => state.recharge)
  const saveProfile = useCustomerPortalStore((state) => state.saveProfile)

  useEffect(() => {
    resetPage()
    void bootstrap()
  }, [bootstrap, resetPage])

  useEffect(() => {
    const timer = window.setInterval(() => {
      void refreshPortal().catch(() => {})
    }, 5000)

    return () => {
      window.clearInterval(timer)
    }
  }, [refreshPortal])

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

  const handleSaveProfile = async () => {
    const result = await saveProfile()
    if (result.ok) {
      showNotice('个人信息已保存。', 'success')
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
          <HomeTab
            merchants={merchants}
            products={products}
            selectedMerchantId={selectedMerchantId}
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
            name={customerAccount.profile.name}
            phone={customerAccount.profile.phone}
            defaultAddress={customerAccount.profile.defaultAddress}
            walletBalance={walletBalance}
            merchants={merchants}
            pendingOrders={pendingOrders}
            historyOrders={historyOrders}
            onOpenRecharge={() => setIsRechargeOpen(true)}
            onOpenEditProfile={openEditProfile}
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
      <EditProfileDialog
        open={isEditProfileOpen}
        name={profileDraft.name}
        phone={profileDraft.phone}
        defaultAddress={profileDraft.defaultAddress}
        onOpenChange={setIsEditProfileOpen}
        onNameChange={(value) => setProfileDraftField('name', value)}
        onPhoneChange={(value) => setProfileDraftField('phone', value)}
        onDefaultAddressChange={(value) => setProfileDraftField('defaultAddress', value)}
        onConfirm={handleSaveProfile}
      />
      <OrderDetailDialog
        selectedOrder={selectedOrder}
        onOpenChange={(open) => !open && setSelectedOrder(null)}
        onClose={() => setSelectedOrder(null)}
      />
    </DeliveryPageShell>
  )
}
