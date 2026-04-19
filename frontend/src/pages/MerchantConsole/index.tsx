import { useEffect, useMemo } from 'react'

import { DeliveryPageShell } from '@/components/DeliveryPageShell'
import { Card, CardContent } from '@/components/ui/card'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { useAppChrome } from '@/hooks/useAppChrome'
import { useMerchantConsoleStore } from '@/stores/pages/use-merchant-console-store'

import { OrdersTab } from './OrdersTab'
import { ProductsTab } from './ProductsTab'
import { ProfileTab } from './ProfileTab'
import { StoreSelectorDialog } from './StoreSelectorDialog'
import { isMerchantTab } from './helpers'

export default function MerchantConsole() {
  const { showNotice } = useAppChrome()
  const bootstrapDone = useMerchantConsoleStore((state) => state.bootstrapDone)
  const loadError = useMerchantConsoleStore((state) => state.loadError)
  const merchantAccount = useMerchantConsoleStore((state) => state.merchantAccount)
  const activeTab = useMerchantConsoleStore((state) => state.activeTab)
  const isStoreDialogOpen = useMerchantConsoleStore((state) => state.isStoreDialogOpen)
  const selectedStoreId = useMerchantConsoleStore((state) => state.selectedStoreId)
  const newStoreName = useMerchantConsoleStore((state) => state.newStoreName)
  const newStoreAddress = useMerchantConsoleStore((state) => state.newStoreAddress)
  const stores = useMerchantConsoleStore((state) => state.stores)
  const resetPage = useMerchantConsoleStore((state) => state.resetPage)
  const setActiveTab = useMerchantConsoleStore((state) => state.setActiveTab)
  const setIsStoreDialogOpen = useMerchantConsoleStore((state) => state.setIsStoreDialogOpen)
  const setSelectedStoreId = useMerchantConsoleStore((state) => state.setSelectedStoreId)
  const setNewStoreName = useMerchantConsoleStore((state) => state.setNewStoreName)
  const setNewStoreAddress = useMerchantConsoleStore((state) => state.setNewStoreAddress)
  const bootstrap = useMerchantConsoleStore((state) => state.bootstrap)
  const createStore = useMerchantConsoleStore((state) => state.createStore)

  useEffect(() => {
    resetPage()
    void bootstrap()
  }, [bootstrap, resetPage])

  const selectedStore = useMemo(
    () => stores.find((item) => item.merchant.id === selectedStoreId) ?? null,
    [stores, selectedStoreId],
  )

  const handleCreateStore = async () => {
    try {
      await createStore()
    } catch (error) {
      showNotice(error instanceof Error ? error.message : '创建店铺失败', 'error')
    }
  }

  if (!bootstrapDone) {
    return (
      <DeliveryPageShell title="商家端后台核心功能" description="正在加载…" roleBadge="商家后台">
        <Card className="border-orange-100 bg-white/95">
          <CardContent className="p-6 text-sm text-slate-600">加载中…</CardContent>
        </Card>
      </DeliveryPageShell>
    )
  }

  if (loadError) {
    return (
      <DeliveryPageShell title="商家端后台核心功能" description="加载失败" roleBadge="商家后台">
        <Card className="border-orange-100 bg-white/95">
          <CardContent className="p-6 text-sm text-rose-600">{loadError}</CardContent>
        </Card>
      </DeliveryPageShell>
    )
  }

  if (!merchantAccount) {
    return (
      <DeliveryPageShell
        title="商家端后台核心功能"
        description="当前账号未绑定商家信息，请先完成商家注册资料。"
        roleBadge="商家后台"
      >
        <Card className="border-orange-100 bg-white/95">
          <CardContent className="p-6 text-sm text-slate-600">未找到当前商家账号档案。</CardContent>
        </Card>
      </DeliveryPageShell>
    )
  }

  return (
    <DeliveryPageShell
      title="商家端后台核心功能"
      description="包含商家入驻申请、商品管理、订单处理（接单/出餐完成）和营业概况查看。"
      roleBadge="商家后台"
    >
      <Tabs
        value={activeTab}
        onValueChange={(value) => {
          if (isMerchantTab(value)) {
            setActiveTab(value)
          }
        }}
      >
        <Card className="border-orange-100 bg-white/95 py-0">
          <CardContent className="px-4 py-4">
            <TabsList className="grid h-11 w-full grid-cols-3 rounded-xl bg-orange-50 p-1">
              <TabsTrigger value="products" className="rounded-lg">
                菜品
              </TabsTrigger>
              <TabsTrigger value="orders" className="rounded-lg">
                接单与出餐处理
              </TabsTrigger>
              <TabsTrigger value="profile" className="rounded-lg">
                我的
              </TabsTrigger>
            </TabsList>
          </CardContent>
        </Card>

        <TabsContent value="products">
          <ProductsTab
            selectedStore={selectedStore}
            onEditProduct={() => showNotice('商品编辑由后端 API 提供后再接线。', 'info')}
            onToggleProduct={() => showNotice('商品上下架由后端 API 提供后再接线。', 'info')}
          />
        </TabsContent>

        <TabsContent value="orders">
          <OrdersTab
            selectedStore={selectedStore}
            onAcceptOrder={() => showNotice('商家接单由后端 API 提供后再接线。', 'info')}
            onFinishCooking={() => showNotice('出餐完成由后端 API 提供后再接线。', 'info')}
          />
        </TabsContent>

        <TabsContent value="profile">
          <ProfileTab selectedStore={selectedStore} onOpenStoreDialog={() => setIsStoreDialogOpen(true)} />
        </TabsContent>
      </Tabs>

      <StoreSelectorDialog
        open={isStoreDialogOpen}
        selectedStoreId={selectedStoreId}
        newStoreName={newStoreName}
        newStoreAddress={newStoreAddress}
        stores={stores}
        onOpenChange={setIsStoreDialogOpen}
        onSelectStore={setSelectedStoreId}
        onChangeStoreName={setNewStoreName}
        onChangeStoreAddress={setNewStoreAddress}
        onEnterSelectedStore={() => {
          if (!selectedStoreId) {
            return
          }
          setIsStoreDialogOpen(false)
        }}
        onCreateStore={handleCreateStore}
      />
    </DeliveryPageShell>
  )
}
