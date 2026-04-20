import { create } from 'zustand'

import { fetchCatalogIO } from '@/api/merchant/CatalogApi'
import { checkoutIO } from '@/api/order/CheckoutApi'
import { runTask } from '@/api/shared/client'
import { fetchCustomerMeIO } from '@/api/user/CustomerMeApi'
import { patchCustomerProfileIO } from '@/api/user/CustomerProfilePatchApi'
import type { Merchant, Product } from '@/objects/merchant'
import type { Order } from '@/objects/order'
import type { MerchantId, ProductId } from '@/objects/shared'
import type { CustomerAccountPublic, CustomerProfilePatch } from '@/objects/user'

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
  isEditProfileOpen: boolean
  rechargeAmountInput: string
  profileDraft: {
    name: string
    phone: string
    defaultAddress: string
  }
  selectedOrder: Order | null
  resetPage: () => void
  refreshPortal: () => Promise<void>
  bootstrap: () => Promise<void>
  setActiveTab: (tab: CustomerTab) => void
  setSelectedMerchantId: (merchantId: MerchantId) => void
  addProductToCart: (merchantId: MerchantId, productId: ProductId) => void
  changeQuantity: (merchantId: MerchantId, productId: ProductId, nextQuantity: number) => void
  setIsRechargeOpen: (open: boolean) => void
  openEditProfile: () => void
  setIsEditProfileOpen: (open: boolean) => void
  setProfileDraftField: (field: 'name' | 'phone' | 'defaultAddress', value: string) => void
  setRechargeAmountInput: (value: string) => void
  setSelectedOrder: (order: Order | null) => void
  checkout: (options?: { merchantId?: MerchantId }) => Promise<
    { ok: true; createdCount: number } | { ok: false; message: string }
  >
  recharge: () => Promise<{ ok: true; amount: number } | { ok: false; message: string }>
  saveProfile: () => Promise<{ ok: true } | { ok: false; message: string }>
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
  isEditProfileOpen: false,
  rechargeAmountInput: '',
  profileDraft: {
    name: '',
    phone: '',
    defaultAddress: '',
  },
  selectedOrder: null as Order | null,
}

export const useCustomerPortalStore = create<CustomerPortalStore>()((set, get) => ({
  ...initialState,
  resetPage: () => set(initialState),
  refreshPortal: async () => {
    const me = await runTask(fetchCustomerMeIO())
    const catalog = await runTask(fetchCatalogIO())
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
      pendingOrders: me.customerAccount.profile.pendingOrders,
      historyOrders: me.customerAccount.profile.historyOrders,
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
  openEditProfile: () => {
    const profile = get().customerAccount?.profile
    set({
      isEditProfileOpen: true,
      profileDraft: {
        name: profile?.name ?? '',
        phone: profile?.phone ?? '',
        defaultAddress: profile?.defaultAddress ?? '',
      },
    })
  },
  setIsEditProfileOpen: (isEditProfileOpen) => set({ isEditProfileOpen }),
  setProfileDraftField: (field, value) =>
    set((state) => ({
      profileDraft: {
        ...state.profileDraft,
        [field]: value,
      },
    })),
  setRechargeAmountInput: (rechargeAmountInput) => set({ rechargeAmountInput }),
  setSelectedOrder: (selectedOrder) => set({ selectedOrder }),
  checkout: async (options?: { merchantId?: MerchantId }) => {
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
      const data = await runTask(checkoutIO(lines))
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
  saveProfile: async () => {
    const { profileDraft } = get()
    const patch: CustomerProfilePatch = {
      name: profileDraft.name.trim(),
      phone: profileDraft.phone.trim(),
      defaultAddress: profileDraft.defaultAddress.trim(),
    }

    if (!patch.name) {
      return { ok: false, message: '请输入姓名。' }
    }
    if (!patch.phone) {
      return { ok: false, message: '请输入联系电话。' }
    }
    if (!patch.defaultAddress) {
      return { ok: false, message: '请输入常用收货地址。' }
    }

    try {
      await runTask(patchCustomerProfileIO(patch))
      await get().refreshPortal()
      set({ isEditProfileOpen: false })
      return { ok: true }
    } catch (error) {
      return { ok: false, message: error instanceof Error ? error.message : '个人信息保存失败' }
    }
  },
}))
