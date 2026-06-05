import { create } from 'zustand'

import { aiDietWeeklyReportIO } from '@/apis/ai/AIDietWeeklyReportAPI'
import { aiOrderProgressNarrativesIO } from '@/apis/ai/AIOrderProgressNarrativesAPI'
import { fetchCustomerOrdersIO } from '@/apis/order/CustomerOrdersAPI'
import { fetchCatalogIO } from '@/apis/merchant/CatalogAPI'
import { cancelOrderIO } from '@/apis/order/OrderCancelAPI'
import { uploadRefundImageFileIO } from '@/apis/order/CustomerRefundImageFileAPI'
import { uploadOrderImageFileIO } from '@/apis/order/CustomerOrderImageFileAPI'
import { completeOrderIO } from '@/apis/order/OrderCompleteAPI'
import { checkoutIO, type CheckoutDeliverySnapshot } from '@/apis/order/CheckoutAPI'
import type { OrderMerchantNote } from '@/objects/order/apiTypes/CheckoutRequest'
import { fetchOrderDetailIO } from '@/apis/order/OrderDetailAPI'
import { appealOrderRefundIO } from '@/apis/order/OrderRefundAppealAPI'
import { requestOrderRefundIO } from '@/apis/order/OrderRefundRequestAPI'
import { uploadReviewImageFileIO } from '@/apis/review/CustomerReviewImageFileAPI'
import { voteMerchantReviewIO } from '@/apis/review/CustomerReviewVoteAPI'
import { submitOrderReviewIO } from '@/apis/review/CustomerSubmitOrderReviewAPI'
import { fetchMerchantReviewsIO } from '@/apis/review/MerchantReviewsAPI'
import { runTask } from '@/apis/shared/client'
import { fetchCustomerMeIO } from '@/apis/user/CustomerMeAPI'
import { patchCustomerProfileIO } from '@/apis/user/CustomerProfilePatchAPI'
import { rechargeCustomerWalletIO } from '@/apis/user/CustomerRechargeAPI'
import { discardCustomerVoucherIO } from '@/apis/user/CustomerVoucherDiscardAPI'
import type { Merchant } from '@/objects/merchant/Merchant'
import type { Product } from '@/objects/merchant/Product'
import type { Order } from '@/objects/order/Order'
import type { MerchantId } from '@/objects/shared/ids'
import type { OrderId } from '@/objects/shared/ids'
import type { ProductId } from '@/objects/shared/ids'
import type { VoucherId } from '@/objects/shared/ids'
import type { AIDietWeeklyReportResponse } from '@/objects/ai/apiTypes/AIDietWeeklyReportResponse'
import type { AIOrderProgressNarrativesResponse } from '@/objects/ai/apiTypes/AIOrderProgressNarrativesResponse'
import { validateDeliveryContacts } from '@/lib/deliveryContacts'
import type { CustomerAccountPublic } from '@/objects/user/CustomerAccountPublic'
import type { CustomerDeliveryContact } from '@/objects/user/CustomerDeliveryContact'
import type { MerchantReviewsResponse } from '@/objects/review/apiTypes/MerchantReviewsResponse'

export type CustomerTab = 'home' | 'cart' | 'profile'

export interface CartLine {
  merchantId: MerchantId
  productId: ProductId
  quantity: number
}

export type FavoriteKind = 'merchant' | 'product'

export interface CustomerFavorites {
  merchantIds: MerchantId[]
  productIds: ProductId[]
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
  reviewTargetOrder: Order | null
  aiDietReport: AIDietWeeklyReportResponse | null
  aiDietReportLoading: boolean
  aiDietReportError: string | null
  aiOrderProgressNarratives: AIOrderProgressNarrativesResponse | null
  aiOrderProgressNarrativesLoading: boolean
  aiOrderProgressNarrativesError: string | null
  favorites: CustomerFavorites
  resetPage: () => void
  refreshPortal: () => Promise<void>
  ensureAIOrderProgressNarratives: () => Promise<void>
  bootstrap: () => Promise<void>
  setActiveTab: (tab: CustomerTab) => void
  setSelectedMerchantId: (merchantId: MerchantId) => void
  toggleFavorite: (kind: FavoriteKind, id: MerchantId | ProductId) => void
  reorderOrder: (orderId: OrderId) => { ok: true; addedCount: number } | { ok: false; message: string }
  addProductToCart: (merchantId: MerchantId, productId: ProductId) => void
  changeQuantity: (merchantId: MerchantId, productId: ProductId, nextQuantity: number) => void
  setIsRechargeOpen: (open: boolean) => void
  setRechargeAmountInput: (value: string) => void
  setSelectedOrder: (order: Order | null) => void
  setReviewTargetOrder: (order: Order | null) => void
  openOrderDetail: (orderId: OrderId) => Promise<{ ok: true } | { ok: false; message: string }>
  cancelOrder: (orderId: OrderId) => Promise<{ ok: true } | { ok: false; message: string }>
  completeOrder: (orderId: OrderId) => Promise<{ ok: true } | { ok: false; message: string }>
  uploadRefundImage: (file: File) => Promise<{ ok: true; imageUrl: string } | { ok: false; message: string }>
  uploadOrderImage: (file: File) => Promise<{ ok: true; imageUrl: string } | { ok: false; message: string }>
  requestRefund: (input: {
    orderId: OrderId
    reason: string
    imageUrl: string | null
  }) => Promise<{ ok: true } | { ok: false; message: string }>
  appealRefund: (orderId: OrderId) => Promise<{ ok: true } | { ok: false; message: string }>
  uploadReviewImage: (file: File) => Promise<{ ok: true; imageUrl: string } | { ok: false; message: string }>
  submitReview: (input: {
    orderId: OrderId
    merchantRating: number
    merchantDescription: string
    merchantImageUrl: string | null
    riderRating: number | null
  }) => Promise<{ ok: true } | { ok: false; message: string }>
  fetchMerchantReviews: (merchantId: MerchantId) => Promise<MerchantReviewsResponse>
  voteMerchantReview: (reviewId: string, vote: 'up' | 'down' | 'none') => Promise<{ ok: true } | { ok: false; message: string }>
  checkout: (options?: {
    merchantId?: MerchantId
    delivery?: CheckoutDeliverySnapshot
    voucherId?: VoucherId
    merchantNotes?: OrderMerchantNote[]
  }) => Promise<{ ok: true; createdCount: number } | { ok: false; message: string }>
  recharge: () => Promise<{ ok: true; amount: number } | { ok: false; message: string }>
  discardExpiredVoucher: (voucherId: VoucherId) => Promise<{ ok: true } | { ok: false; message: string }>
  saveDeliveryContacts: (
    contacts: CustomerDeliveryContact[],
  ) => Promise<{ ok: true } | { ok: false; message: string }>
  generateAIDietReport: () => Promise<{ ok: true } | { ok: false; message: string }>
}

const getLocalDateKey = () => {
  const now = new Date()
  const year = now.getFullYear()
  const month = `${now.getMonth() + 1}`.padStart(2, '0')
  const day = `${now.getDate()}`.padStart(2, '0')
  return `${year}-${month}-${day}`
}

const CustomerFavoritesStorageKey = 'delivery-customer-favorites'

const readStoredFavorites = (): CustomerFavorites => {
  if (typeof window === 'undefined') {
    return { merchantIds: [], productIds: [] }
  }

  try {
    const raw = window.localStorage.getItem(CustomerFavoritesStorageKey)
    if (!raw) {
      return { merchantIds: [], productIds: [] }
    }

    const parsed = JSON.parse(raw) as Partial<CustomerFavorites>
    return {
      merchantIds: Array.isArray(parsed.merchantIds) ? (parsed.merchantIds as MerchantId[]) : [],
      productIds: Array.isArray(parsed.productIds) ? (parsed.productIds as ProductId[]) : [],
    }
  } catch {
    return { merchantIds: [], productIds: [] }
  }
}

const writeStoredFavorites = (favorites: CustomerFavorites) => {
  if (typeof window === 'undefined') {
    return
  }

  window.localStorage.setItem(CustomerFavoritesStorageKey, JSON.stringify(favorites))
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
  reviewTargetOrder: null as Order | null,
  aiDietReport: null as AIDietWeeklyReportResponse | null,
  aiDietReportLoading: false,
  aiDietReportError: null as string | null,
  aiOrderProgressNarratives: null as AIOrderProgressNarrativesResponse | null,
  aiOrderProgressNarrativesLoading: false,
  aiOrderProgressNarrativesError: null as string | null,
  favorites: readStoredFavorites(),
}

let refreshPortalInFlight: Promise<void> | null = null

export const useCustomerPortalStore = create<CustomerPortalStore>()((set, get) => ({
  ...initialState,
  resetPage: () => set(initialState),
  refreshPortal: async () => {
    if (refreshPortalInFlight) {
      return refreshPortalInFlight
    }

    refreshPortalInFlight = (async () => {
      const [me, catalog, orders] = await Promise.all([
        runTask(fetchCustomerMeIO()),
        runTask(fetchCatalogIO()),
        runTask(fetchCustomerOrdersIO()),
      ])
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
    })()

    try {
      await refreshPortalInFlight
    } finally {
      refreshPortalInFlight = null
    }
  },
  ensureAIOrderProgressNarratives: async () => {
    const today = getLocalDateKey()
    const current = get().aiOrderProgressNarratives
    if (get().aiOrderProgressNarrativesLoading || current?.generatedFor === today) {
      return
    }

    set({ aiOrderProgressNarrativesLoading: true, aiOrderProgressNarrativesError: null })
    try {
      const result = await runTask(aiOrderProgressNarrativesIO({}))
      set({ aiOrderProgressNarratives: result, aiOrderProgressNarrativesLoading: false })
    } catch (error) {
      set({
        aiOrderProgressNarrativesError: error instanceof Error ? error.message : 'AI 订单进度叙事生成失败',
        aiOrderProgressNarrativesLoading: false,
      })
    }
  },
  bootstrap: async () => {
    set({ bootstrapDone: false, loadError: null })
    try {
      await get().refreshPortal()
      await get().ensureAIOrderProgressNarratives()
    } catch (error) {
      set({ loadError: error instanceof Error ? error.message : '加载失败' })
    } finally {
      set({ bootstrapDone: true })
    }
  },
  setActiveTab: (activeTab) => set({ activeTab }),
  setSelectedMerchantId: (selectedMerchantId) => set({ selectedMerchantId }),
  toggleFavorite: (kind, id) =>
    set((state) => {
      const nextFavorites =
        kind === 'merchant'
          ? {
              ...state.favorites,
              merchantIds: state.favorites.merchantIds.includes(id as MerchantId)
                ? state.favorites.merchantIds.filter((merchantId) => merchantId !== id)
                : [...state.favorites.merchantIds, id as MerchantId],
            }
          : {
              ...state.favorites,
              productIds: state.favorites.productIds.includes(id as ProductId)
                ? state.favorites.productIds.filter((productId) => productId !== id)
                : [...state.favorites.productIds, id as ProductId],
            }

      writeStoredFavorites(nextFavorites)
      return { favorites: nextFavorites }
    }),
  reorderOrder: (orderId) => {
    const { pendingOrders, historyOrders, products } = get()
    const order = [...pendingOrders, ...historyOrders].find((item) => item.id === orderId)

    if (!order) {
      return { ok: false, message: '未找到这笔订单，无法再来一单。' }
    }

    const visibleProductIds = new Set(
      products.filter((product) => product.merchantId === order.merchantId).map((product) => product.id),
    )
    const reorderItems = order.items.filter((item) => visibleProductIds.has(item.productId))

    if (reorderItems.length === 0) {
      return { ok: false, message: '这笔订单中的菜品暂不可购买。' }
    }

    set((state) => {
      const nextCartLines = [...state.cartLines]
      reorderItems.forEach((item) => {
        const matchedIndex = nextCartLines.findIndex(
          (line) => line.merchantId === order.merchantId && line.productId === item.productId,
        )
        if (matchedIndex >= 0) {
          nextCartLines[matchedIndex] = {
            ...nextCartLines[matchedIndex],
            quantity: nextCartLines[matchedIndex].quantity + item.quantity,
          }
        } else {
          nextCartLines.push({
            merchantId: order.merchantId,
            productId: item.productId,
            quantity: item.quantity,
          })
        }
      })

      return { cartLines: nextCartLines, activeTab: 'cart' }
    })

    const addedCount = reorderItems.reduce((sum, item) => sum + item.quantity, 0)
    return { ok: true, addedCount }
  },
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
  setReviewTargetOrder: (reviewTargetOrder) => set({ reviewTargetOrder }),
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
  completeOrder: async (orderId) => {
    try {
      const order = await runTask(completeOrderIO(orderId))
      await get().refreshPortal()
      set((state) => ({
        selectedOrder: state.selectedOrder?.id === orderId ? order : state.selectedOrder,
        reviewTargetOrder: order,
      }))
      return { ok: true }
    } catch (error) {
      return { ok: false, message: error instanceof Error ? error.message : '确认完成失败' }
    }
  },
  uploadRefundImage: async (file) => {
    try {
      const imageUrl = await runTask(uploadRefundImageFileIO(file))
      return { ok: true, imageUrl }
    } catch (error) {
      return { ok: false, message: error instanceof Error ? error.message : '退款凭证上传失败' }
    }
  },
  uploadOrderImage: async (file) => {
    try {
      const imageUrl = await runTask(uploadOrderImageFileIO(file))
      return { ok: true, imageUrl }
    } catch (error) {
      return { ok: false, message: error instanceof Error ? error.message : '图片上传失败' }
    }
  },
  requestRefund: async (input) => {
    try {
      const data = await runTask(requestOrderRefundIO(input.orderId, input.reason, input.imageUrl))
      await get().refreshPortal()
      set((state) => ({
        selectedOrder: state.selectedOrder?.id === input.orderId ? data.order : state.selectedOrder,
      }))
      return { ok: true }
    } catch (error) {
      return { ok: false, message: error instanceof Error ? error.message : '退款申请提交失败' }
    }
  },
  appealRefund: async (orderId) => {
    try {
      const data = await runTask(appealOrderRefundIO(orderId))
      await get().refreshPortal()
      set((state) => ({
        selectedOrder: state.selectedOrder?.id === orderId ? data.order : state.selectedOrder,
      }))
      return { ok: true }
    } catch (error) {
      return { ok: false, message: error instanceof Error ? error.message : '提交管理员仲裁失败' }
    }
  },
  uploadReviewImage: async (file) => {
    try {
      const imageUrl = await runTask(uploadReviewImageFileIO(file))
      return { ok: true, imageUrl }
    } catch (error) {
      return { ok: false, message: error instanceof Error ? error.message : '图片上传失败' }
    }
  },
  submitReview: async (input) => {
    try {
      await runTask(submitOrderReviewIO(input))
      await get().refreshPortal()
      set({ reviewTargetOrder: null })
      return { ok: true }
    } catch (error) {
      return { ok: false, message: error instanceof Error ? error.message : '提交评价失败' }
    }
  },
  fetchMerchantReviews: async (merchantId) => runTask(fetchMerchantReviewsIO(merchantId)),
  voteMerchantReview: async (reviewId, vote) => {
    try {
      await runTask(voteMerchantReviewIO(reviewId, vote))
      return { ok: true }
    } catch (error) {
      return { ok: false, message: error instanceof Error ? error.message : '评价投票失败' }
    }
  },
  checkout: async (options?: { merchantId?: MerchantId; delivery?: CheckoutDeliverySnapshot; voucherId?: VoucherId; merchantNotes?: OrderMerchantNote[] }) => {
    const merchantId = options?.merchantId
    const { cartLines, walletBalance, products, customerAccount } = get()
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

    const selectedVoucher = customerAccount?.profile.vouchers.find((voucher) => voucher.id === options?.voucherId)
    const estimatedDiscount = selectedVoucher && cartTotal >= selectedVoucher.minSpend && selectedVoucher.remainingCount > 0
      ? Math.min(selectedVoucher.discountAmount, cartTotal)
      : 0

    if (walletBalance < cartTotal - estimatedDiscount) {
      return { ok: false, message: '余额不足，请先充值。' }
    }

    try {
      const data = await runTask(checkoutIO(lines, options?.delivery, options?.voucherId, options?.merchantNotes ?? []))
      const nextCartLines = merchantId
        ? cartLines.filter((line) => line.merchantId !== merchantId)
        : []
      set({
        cartLines: nextCartLines,
        activeTab: merchantId ? get().activeTab : 'profile',
      })
      await get().refreshPortal()
      return { ok: true, createdCount: data.orders.length }
    } catch (error) {
      return { ok: false, message: error instanceof Error ? error.message : '结算失败' }
    }
  },
  recharge: async () => {
    const { rechargeAmountInput } = get()
    const amount = Number.parseFloat(rechargeAmountInput)

    if (!Number.isFinite(amount) || amount <= 0) {
      return { ok: false, message: '请输入有效的充值金额。' }
    }

    try {
      const data = await runTask(rechargeCustomerWalletIO({ amount }))
      await get().refreshPortal()
      set({
        walletBalance: data.walletBalance,
        rechargeAmountInput: '',
        isRechargeOpen: false,
      })
      return { ok: true, amount }
    } catch (error) {
      return { ok: false, message: error instanceof Error ? error.message : '充值同步失败' }
    }
  },
  discardExpiredVoucher: async (voucherId) => {
    try {
      await runTask(discardCustomerVoucherIO(voucherId))
      await get().refreshPortal()
      return { ok: true }
    } catch (error) {
      return { ok: false, message: error instanceof Error ? error.message : '优惠券舍弃失败' }
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
  generateAIDietReport: async () => {
    set({ aiDietReportLoading: true, aiDietReportError: null })
    try {
      const result = await runTask(aiDietWeeklyReportIO({}))
      set({ aiDietReport: result, aiDietReportLoading: false })
      return { ok: true }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'AI 周报生成失败'
      set({ aiDietReportError: errorMessage, aiDietReportLoading: false })
      return { ok: false, message: errorMessage }
    }
  },
}))
