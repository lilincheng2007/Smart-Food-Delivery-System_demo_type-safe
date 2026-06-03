import { useEffect, useMemo } from 'react'

import { DeliveryLogoutBar } from '@/components/DeliveryLogoutBar'
import { DeliveryPageShell } from '@/components/DeliveryPageShell'
import { Card, CardContent } from '@/components/ui/card'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { useAppChrome } from '@/hooks/useAppChrome'
import { useMerchantConsoleStore } from '@/stores/pages/use-merchant-console-store'

import { OrdersTab } from './components/OrdersTab'
import { ProductsTab } from './components/ProductsTab'
import { ProfileTab } from './components/ProfileTab'
import { StoreSelectorDialog } from './components/StoreSelectorDialog'
import { isMerchantTab } from './functions/helpers'

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
  const refreshMerchant = useMerchantConsoleStore((state) => state.refreshMerchant)
  const createStore = useMerchantConsoleStore((state) => state.createStore)
  const createProduct = useMerchantConsoleStore((state) => state.createProduct)
  const acceptOrder = useMerchantConsoleStore((state) => state.acceptOrder)
  const rejectOrder = useMerchantConsoleStore((state) => state.rejectOrder)
  const finishCooking = useMerchantConsoleStore((state) => state.finishCooking)
  const updateProduct = useMerchantConsoleStore((state) => state.updateProduct)

  useEffect(() => {
    resetPage()
    void bootstrap()
  }, [bootstrap, resetPage])

  useEffect(() => {
    const timer = window.setInterval(() => {
      void refreshMerchant().catch(() => {})
    }, 5000)

    return () => {
      window.clearInterval(timer)
    }
  }, [refreshMerchant])

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
      <DeliveryPageShell>
        <Card className="border-orange-100 bg-white/95">
          <CardContent className="p-6 text-sm text-slate-600">加载中…</CardContent>
        </Card>
        <DeliveryLogoutBar />
      </DeliveryPageShell>
    )
  }

  if (loadError) {
    return (
      <DeliveryPageShell>
        <Card className="border-orange-100 bg-white/95">
          <CardContent className="p-6 text-sm text-rose-600">{loadError}</CardContent>
        </Card>
        <DeliveryLogoutBar />
      </DeliveryPageShell>
    )
  }

  if (!merchantAccount) {
    return (
      <DeliveryPageShell>
        <Card className="border-orange-100 bg-white/95">
          <CardContent className="p-6 text-sm text-slate-600">未找到当前商家账号档案。</CardContent>
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
                订单处理
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
            onCreateProduct={(input) =>
              createProduct(input)
                .then(() => showNotice('新菜品已创建。', 'success'))
                .catch((error) => {
                  showNotice(error instanceof Error ? error.message : '创建菜品失败', 'error')
                  throw error
                })
            }
            onEditProduct={(productId, input) =>
              updateProduct(productId, input)
                .then(() => showNotice('菜品信息已更新。', 'success'))
                .catch((error) => {
                  showNotice(error instanceof Error ? error.message : '菜品更新失败', 'error')
                  throw error
                })
            }
          />
        </TabsContent>

        <TabsContent value="orders">
          <OrdersTab
            selectedStore={selectedStore}
            onAcceptOrder={(orderId) => {
              void acceptOrder(orderId)
                .then(() => showNotice('已接单，订单进入制作中。', 'success'))
                .catch((error) => showNotice(error instanceof Error ? error.message : '接单失败', 'error'))
            }}
            onRejectOrder={(orderId) => {
              void rejectOrder(orderId)
                .then(() => showNotice('已拒收订单，款项将退回顾客钱包。', 'success'))
                .catch((error) => showNotice(error instanceof Error ? error.message : '拒收失败', 'error'))
            }}
            onFinishCooking={(orderId) => {
              void finishCooking(orderId)
                .then(() => showNotice('订单已进入待骑手接单状态。', 'success'))
                .catch((error) => showNotice(error instanceof Error ? error.message : '出餐完成失败', 'error'))
            }}
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
