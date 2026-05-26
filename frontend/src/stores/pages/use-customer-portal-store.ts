import { create } from 'zustand'

import { fetchCustomerOrdersIO } from '@/api/order/CustomerOrdersApi'
import { fetchCatalogIO } from '@/api/merchant/CatalogApi'
import { cancelOrderIO } from '@/api/order/OrderCancelApi'
import { checkoutIO, type CheckoutDeliverySnapshot } from '@/api/order/CheckoutApi'
import { fetchOrderDetailIO } from '@/api/order/OrderDetailApi'
import { runTask } from '@/api/shared/client'
import { fetchCustomerMeIO } from '@/api/user/CustomerMeApi'
import { patchCustomerProfileIO } from '@/api/user/CustomerProfilePatchApi'
import type { Merchant } from '@/objects/merchant/Merchant'
import type { Product } from '@/objects/merchant/Product'
import type { Order } from '@/objects/order/Order'
import type { MerchantId } from '@/objects/shared/ids'
import type { OrderId } from '@/objects/shared/ids'
import type { ProductId } from '@/objects/shared/ids'
import { validateDeliveryContacts } from '@/lib/deliveryContacts'
import type { CustomerAccountPublic } from '@/objects/user/CustomerAccountPublic'
import type { CustomerDeliveryContact } from '@/objects/user/CustomerDeliveryContact'

export type CustomerTab = 'home' | 'cart' | 'profile'

export interface CartLine {
  merchantId: MerchantId
  productId: ProductId
  quantity: number
}

type CustomerPortalStore = {
  bootstrapDone: boolean
  loadError: string | null
  customerAccount: CustomerAccountPublic | null
  merchants: Merchant[]
  products: Product[]
  activeTab: CustomerTab
  selectedMerchantId: MerchantId
  cartLines: CartLine[]
  walletBalance: number
  pendingOrders: Order[]
  historyOrders: Order[]
  isRechargeOpen: boolean
  rechargeAmountInput: string
  selectedOrder: Order | null
  resetPage: () => void
  refreshPortal: () => Promise<void>
  bootstrap: () => Promise<void>
  setActiveTab: (tab: CustomerTab) => void
  setSelectedMerchantId: (merchantId: MerchantId) => void
  addProductToCart: (merchantId: MerchantId, productId: ProductId) => void
  changeQuantity: (merchantId: MerchantId, productId: ProductId, nextQuantity: number) => void
  setIsRechargeOpen: (open: boolean) => void
  setRechargeAmountInput: (value: string) => void
  setSelectedOrder: (order: Order | null) => void
  openOrderDetail: (orderId: OrderId) => Promise<{ ok: true } | { ok: false; message: string }>
  cancelOrder: (orderId: OrderId) => Promise<{ ok: true } | { ok: false; message: string }>
  checkout: (options?: {
    merchantId?: MerchantId
    delivery?: CheckoutDeliverySnapshot
  }) => Promise<{ ok: true; createdCount: number } | { ok: false; message: string }>
  recharge: () => Promise<{ ok: true; amount: number } | { ok: false; message: string }>
  saveDeliveryContacts: (
    contacts: CustomerDeliveryContact[],
  ) => Promise<{ ok: true } | { ok: false; message: string }>
}

const initialState = {
  bootstrapDone: false,
  loadError: null as string | null,
  customerAccount: null as CustomerAccountPublic | null,
  merchants: [] as Merchant[],
  products: [] as Product[],
  activeTab: 'home' as CustomerTab,
  selectedMerchantId: '' as MerchantId,
  cartLines: [] as CartLine[],
  walletBalance: 0,
  pendingOrders: [] as Order[],
  historyOrders: [] as Order[],
  isRechargeOpen: false,
  rechargeAmountInput: '',
  selectedOrder: null as Order | null,
}

export const useCustomerPortalStore = create<CustomerPortalStore>()((set, get) => ({
  ...initialState,
  resetPage: () => set(initialState),
  refreshPortal: async () => {
    const me = await runTask(fetchCustomerMeIO())
    const catalog = await runTask(fetchCatalogIO())
    const orders = await runTask(fetchCustomerOrdersIO())
    const visibleProductIds = new Set(catalog.products.map((product) => product.id))
    const currentSelectedMerchantId = get().selectedMerchantId
    const nextSelectedMerchantId =
      currentSelectedMerchantId && catalog.merchants.some((merchant) => merchant.id === currentSelectedMerchantId)
        ? currentSelectedMerchantId
        : (catalog.merchants[0]?.id ?? '')
    const nextCartLines = get().cartLines.filter((line) => visibleProductIds.has(line.productId))

    set({
      customerAccount: me.customerAccount,
      walletBalance: me.customerAccount.profile.walletBalance,
      pendingOrders: orders.pendingOrders,
      historyOrders: orders.historyOrders,
      merchants: catalog.merchants,
      products: catalog.products,
      selectedMerchantId: nextSelectedMerchantId,
      cartLines: nextCartLines,
    })
  },
  bootstrap: async () => {
    set({ bootstrapDone: false, loadError: null })
    try {
      await get().refreshPortal()
    } catch (error) {
      set({ loadError: error instanceof Error ? error.message : '加载失败' })
    } finally {
      set({ bootstrapDone: true })
    }
  },
  setActiveTab: (activeTab) => set({ activeTab }),
  setSelectedMerchantId: (selectedMerchantId) => set({ selectedMerchantId }),
  addProductToCart: (merchantId, productId) =>
    set((state) => {
      const matched = state.cartLines.find((line) => line.merchantId === merchantId && line.productId === productId)
      if (!matched) {
        return { cartLines: [...state.cartLines, { merchantId, productId, quantity: 1 }] }
      }

      return {
        cartLines: state.cartLines.map((line) =>
          line.merchantId === merchantId && line.productId === productId
            ? { ...line, quantity: line.quantity + 1 }
            : line,
        ),
      }
    }),
  changeQuantity: (merchantId, productId, nextQuantity) =>
    set((state) => {
      if (nextQuantity <= 0) {
        return {
          cartLines: state.cartLines.filter(
            (line) => !(line.merchantId === merchantId && line.productId === productId),
          ),
        }
      }

      return {
        cartLines: state.cartLines.map((line) =>
          line.merchantId === merchantId && line.productId === productId ? { ...line, quantity: nextQuantity } : line,
        ),
      }
    }),
  setIsRechargeOpen: (isRechargeOpen) => set({ isRechargeOpen }),
  setRechargeAmountInput: (rechargeAmountInput) => set({ rechargeAmountInput }),
  setSelectedOrder: (selectedOrder) => set({ selectedOrder }),
  openOrderDetail: async (orderId) => {
    try {
      const order = await runTask(fetchOrderDetailIO(orderId))
      set({ selectedOrder: order })
      return { ok: true }
    } catch (error) {
      return { ok: false, message: error instanceof Error ? error.message : '订单详情加载失败' }
    }
  },
  cancelOrder: async (orderId) => {
    try {
      const data = await runTask(cancelOrderIO(orderId))
      await get().refreshPortal()
      set((state) => ({
        walletBalance: data.walletBalance,
        selectedOrder: state.selectedOrder?.id === orderId ? data.order : state.selectedOrder,
      }))
      return { ok: true }
    } catch (error) {
      return { ok: false, message: error instanceof Error ? error.message : '订单取消失败' }
    }
  },
  checkout: async (options?: { merchantId?: MerchantId; delivery?: CheckoutDeliverySnapshot }) => {
    const merchantId = options?.merchantId
    const { cartLines, walletBalance, products, pendingOrders, customerAccount } = get()
    const lines = merchantId ? cartLines.filter((line) => line.merchantId === merchantId) : cartLines

    if (lines.length === 0) {
      return {
        ok: false,
        message: merchantId ? '本店购物车为空，无法结算。' : '购物车为空，无法结算。',
      }
    }

    const cartTotal = lines.reduce((sum, line) => {
      const product = products.find((item) => item.id === line.productId)
      return sum + (product ? product.price * line.quantity : 0)
    }, 0)

    if (walletBalance < cartTotal) {
      return { ok: false, message: '余额不足，请先充值。' }
    }

    try {
      const data = await runTask(checkoutIO(lines, options?.delivery))
      const nextPendingOrders = [...data.orders, ...pendingOrders]
      const nextCartLines = merchantId
        ? cartLines.filter((line) => line.merchantId !== merchantId)
        : []
      set({
        walletBalance: data.walletBalance,
        pendingOrders: nextPendingOrders,
        customerAccount: customerAccount
          ? {
              ...customerAccount,
              profile: {
                ...customerAccount.profile,
                walletBalance: data.walletBalance,
                pendingOrders: nextPendingOrders,
              },
            }
          : customerAccount,
        cartLines: nextCartLines,
        activeTab: merchantId ? get().activeTab : 'profile',
      })
      return { ok: true, createdCount: data.orders.length }
    } catch (error) {
      return { ok: false, message: error instanceof Error ? error.message : '结算失败' }
    }
  },
  recharge: async () => {
    const { rechargeAmountInput, walletBalance } = get()
    const amount = Number.parseFloat(rechargeAmountInput)

    if (!Number.isFinite(amount) || amount <= 0) {
      return { ok: false, message: '请输入有效的充值金额。' }
    }

    const nextWalletBalance = walletBalance + amount

    try {
      await runTask(patchCustomerProfileIO({ walletBalance: nextWalletBalance }))
      await get().refreshPortal()
      set({
        walletBalance: nextWalletBalance,
        rechargeAmountInput: '',
        isRechargeOpen: false,
      })
      return { ok: true, amount }
    } catch (error) {
      return { ok: false, message: error instanceof Error ? error.message : '充值同步失败' }
    }
  },
  saveDeliveryContacts: async (contacts) => {
    const message = validateDeliveryContacts(contacts)
    if (message) {
      return { ok: false, message }
    }
    try {
      await runTask(patchCustomerProfileIO({ deliveryContacts: contacts }))
      await get().refreshPortal()
      return { ok: true }
    } catch (error) {
      return { ok: false, message: error instanceof Error ? error.message : '收货信息保存失败' }
    }
  },
}))
