import { create } from 'zustand'

import { fetchCatalogIO } from '@/api/merchant/CatalogApi'
import { checkoutIO } from '@/api/order/CheckoutApi'
import { runTask } from '@/api/shared/client'
import { fetchCustomerMeIO } from '@/api/user/CustomerMeApi'
import { patchCustomerProfileIO } from '@/api/user/CustomerProfilePatchApi'
import type { Merchant, Product } from '@/objects/merchant'
import type { Order } from '@/objects/order'
import type { MerchantId, ProductId } from '@/objects/shared'
import type { CustomerAccountPublic } from '@/objects/user'

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
  bootstrap: () => Promise<void>
  setActiveTab: (tab: CustomerTab) => void
  setSelectedMerchantId: (merchantId: MerchantId) => void
  addProductToCart: (merchantId: MerchantId, productId: ProductId) => void
  changeQuantity: (merchantId: MerchantId, productId: ProductId, nextQuantity: number) => void
  setIsRechargeOpen: (open: boolean) => void
  setRechargeAmountInput: (value: string) => void
  setSelectedOrder: (order: Order | null) => void
  checkout: () => Promise<{ ok: true; createdCount: number } | { ok: false; message: string }>
  recharge: () => Promise<{ ok: true; amount: number } | { ok: false; message: string }>
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
  bootstrap: async () => {
    set({ bootstrapDone: false, loadError: null })
    try {
      const me = await runTask(fetchCustomerMeIO())
      const catalog = await runTask(fetchCatalogIO())
      set({
        customerAccount: me.customerAccount,
        walletBalance: me.customerAccount.profile.walletBalance,
        pendingOrders: me.customerAccount.profile.pendingOrders,
        historyOrders: me.customerAccount.profile.historyOrders,
        merchants: catalog.merchants,
        products: catalog.products,
        selectedMerchantId: catalog.merchants[0]?.id ?? '',
      })
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
  checkout: async () => {
    const { cartLines, walletBalance, products, pendingOrders, customerAccount } = get()

    if (cartLines.length === 0) {
      return { ok: false, message: '购物车为空，无法结算。' }
    }

    const cartTotal = cartLines.reduce((sum, line) => {
      const product = products.find((item) => item.id === line.productId)
      return sum + (product ? product.price * line.quantity : 0)
    }, 0)

    if (walletBalance < cartTotal) {
      return { ok: false, message: '余额不足，请先充值。' }
    }

    try {
      const data = await runTask(checkoutIO(cartLines))
      const nextPendingOrders = [...data.orders, ...pendingOrders]
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
        cartLines: [],
        activeTab: 'profile',
      })
      return { ok: true, createdCount: data.orders.length }
    } catch (error) {
      return { ok: false, message: error instanceof Error ? error.message : '结算失败' }
    }
  },
  recharge: async () => {
    const { rechargeAmountInput, walletBalance, customerAccount } = get()
    const amount = Number.parseFloat(rechargeAmountInput)

    if (!Number.isFinite(amount) || amount <= 0) {
      return { ok: false, message: '请输入有效的充值金额。' }
    }

    const nextWalletBalance = walletBalance + amount

    try {
      await runTask(patchCustomerProfileIO({ walletBalance: nextWalletBalance }))
      set({
        walletBalance: nextWalletBalance,
        customerAccount: customerAccount
          ? {
              ...customerAccount,
              profile: {
                ...customerAccount.profile,
                walletBalance: nextWalletBalance,
              },
            }
          : customerAccount,
        rechargeAmountInput: '',
        isRechargeOpen: false,
      })
      return { ok: true, amount }
    } catch (error) {
      return { ok: false, message: error instanceof Error ? error.message : '充值同步失败' }
    }
  },
}))
