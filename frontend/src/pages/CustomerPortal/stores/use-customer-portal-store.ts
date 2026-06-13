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
import type { MerchantId, VoucherId } from '@/objects/shared/ids'
import { validateDeliveryContacts } from '@/lib/deliveryContacts'
import { bundleLineUnitPrice } from '@/lib/bundles'
import { bestPromotion, roundMoney } from '@/lib/promotions'
import { cartLineKey, maxCartLineQuantity, normalizeCartLines, productAvailable } from '@/lib/cart-inventory'
import { getLocalDateKey, resolveSelectedMerchantId, toggleCustomerFavorite } from './customerPortal/helpers'
import { initialState } from './customerPortal/initialState'
import type { CartLine, CustomerFavorites, CustomerPortalStore, CustomerTab, FavoriteKind } from './customerPortal/types'
import { writeStoredFavorites } from './customerPortal/favoritesStorage'

export type { CartLine, CustomerFavorites, CustomerTab, FavoriteKind }

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
      const currentSelectedMerchantId = get().selectedMerchantId
      const nextSelectedMerchantId = resolveSelectedMerchantId(currentSelectedMerchantId, catalog.merchants)
      const nextCartLines = normalizeCartLines(get().cartLines, catalog.products)

      set({
        customerAccount: me.customerAccount,
        walletBalance: me.customerAccount.profile.walletBalance,
        pendingOrders: orders.pendingOrders,
        historyOrders: orders.historyOrders,
        merchants: catalog.merchants,
        products: catalog.products,
        platformPromotions: catalog.platformPromotions ?? [],
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
      const nextFavorites = toggleCustomerFavorite(state.favorites, kind, id)
      writeStoredFavorites(nextFavorites)
      return { favorites: nextFavorites }
    }),
  reorderOrder: (orderId) => {
    const { pendingOrders, historyOrders, products } = get()
    const order = [...pendingOrders, ...historyOrders].find((item) => item.id === orderId)

    if (!order) {
      return { ok: false, message: '未找到这笔订单，无法再来一单。' }
    }

    const reorderItems = order.items.filter((item) => {
      const product = products.find((current) => current.id === item.productId && current.merchantId === order.merchantId)
      return product ? productAvailable(product) && (product.bundleGroups ?? []).length === 0 : false
    })

    if (reorderItems.length === 0) {
      return { ok: false, message: '这笔订单中的菜品暂不可购买。' }
    }

    let addedCount = 0
    set((state) => {
      const nextCartLines = [...state.cartLines]
      reorderItems.forEach((item) => {
        const draftLine = { merchantId: order.merchantId, productId: item.productId, quantity: 0 }
        const key = cartLineKey(draftLine)
        const matchedIndex = nextCartLines.findIndex((line) => cartLineKey(line) === key)
        const currentQuantity = matchedIndex >= 0 ? nextCartLines[matchedIndex].quantity : 0
        const otherLines = matchedIndex >= 0 ? nextCartLines.filter((_, index) => index !== matchedIndex) : nextCartLines
        const maxQuantity = maxCartLineQuantity(draftLine, products, otherLines)
        const nextQuantity = Math.min(maxQuantity, currentQuantity + item.quantity)
        const added = Math.max(0, nextQuantity - currentQuantity)
        addedCount += added

        if (nextQuantity <= 0) return
        if (matchedIndex >= 0) {
          nextCartLines[matchedIndex] = { ...nextCartLines[matchedIndex], quantity: nextQuantity }
        } else {
          nextCartLines.push({ merchantId: order.merchantId, productId: item.productId, quantity: nextQuantity })
        }
      })

      return { cartLines: nextCartLines, activeTab: 'cart' }
    })

    return addedCount > 0 ? { ok: true, addedCount } : { ok: false, message: '这笔订单中的菜品已售罄或超过限购。' }
  },
  addProductToCart: (merchantId, productId) =>
    set((state) => {
      const product = state.products.find((item) => item.id === productId && item.merchantId === merchantId)
      if (!product || !productAvailable(product)) return {}
      const draftLine = { merchantId, productId, quantity: 1 }
      const key = cartLineKey(draftLine)
      const matched = state.cartLines.find((line) => cartLineKey(line) === key)
      const otherLines = state.cartLines.filter((line) => cartLineKey(line) !== key)
      const maxQuantity = maxCartLineQuantity(draftLine, state.products, otherLines)

      if (!matched) {
        return maxQuantity >= 1 ? { cartLines: [...state.cartLines, draftLine] } : {}
      }

      return {
        cartLines: state.cartLines.map((line) =>
          cartLineKey(line) === key ? { ...line, quantity: Math.min(maxQuantity, line.quantity + 1) } : line,
        ),
      }
    }),
  addBundleToCart: (merchantId, productId, bundleSelections) =>
    set((state) => {
      const draftLine = { merchantId, productId, quantity: 1, bundleSelections }
      const key = cartLineKey(draftLine)
      const matched = state.cartLines.find((line) => cartLineKey(line) === key)
      const otherLines = state.cartLines.filter((line) => cartLineKey(line) !== key)
      const maxQuantity = maxCartLineQuantity(draftLine, state.products, otherLines)

      if (!matched) {
        return maxQuantity >= 1 ? { cartLines: [...state.cartLines, draftLine] } : {}
      }

      return {
        cartLines: state.cartLines.map((line) =>
          cartLineKey(line) === key ? { ...line, quantity: Math.min(maxQuantity, line.quantity + 1) } : line,
        ),
      }
    }),
  changeQuantity: (merchantId, productId, nextQuantity) =>
    set((state) => {
      const key = cartLineKey({ merchantId, productId })
      if (nextQuantity <= 0) {
        return { cartLines: state.cartLines.filter((line) => cartLineKey(line) !== key) }
      }

      const targetLine = state.cartLines.find((line) => cartLineKey(line) === key) ?? { merchantId, productId, quantity: nextQuantity }
      const otherLines = state.cartLines.filter((line) => cartLineKey(line) !== key)
      const maxQuantity = maxCartLineQuantity(targetLine, state.products, otherLines)
      const quantity = Math.min(nextQuantity, maxQuantity)

      return {
        cartLines: quantity <= 0
          ? state.cartLines.filter((line) => cartLineKey(line) !== key)
          : state.cartLines.map((line) => cartLineKey(line) === key ? { ...line, quantity } : line),
      }
    }),
  changeCartLineQuantity: (lineKey, nextQuantity) =>
    set((state) => {
      if (nextQuantity <= 0) {
        return { cartLines: state.cartLines.filter((line) => cartLineKey(line) !== lineKey) }
      }

      const targetLine = state.cartLines.find((line) => cartLineKey(line) === lineKey)
      if (!targetLine) return {}
      const otherLines = state.cartLines.filter((line) => cartLineKey(line) !== lineKey)
      const maxQuantity = maxCartLineQuantity(targetLine, state.products, otherLines)
      const quantity = Math.min(nextQuantity, maxQuantity)

      return {
        cartLines: quantity <= 0
          ? state.cartLines.filter((line) => cartLineKey(line) !== lineKey)
          : state.cartLines.map((line) => cartLineKey(line) === lineKey ? { ...line, quantity } : line),
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
    const { cartLines, walletBalance, products, customerAccount, merchants, platformPromotions } = get()
    const lines = merchantId ? cartLines.filter((line) => line.merchantId === merchantId) : cartLines

    if (lines.length === 0) {
      return {
        ok: false,
        message: merchantId ? '本店购物车为空，无法结算。' : '购物车为空，无法结算。',
      }
    }

    const cartTotal = lines.reduce((sum, line) => {
      const product = products.find((item) => item.id === line.productId)
      return sum + (product ? bundleLineUnitPrice(product, line.bundleSelections, products) * line.quantity : 0)
    }, 0)
    const merchantDiscount = [...new Set(lines.map((line) => line.merchantId))].reduce((sum, lineMerchantId) => {
      const merchantLines = lines.filter((line) => line.merchantId === lineMerchantId)
      const merchantTotal = merchantLines.reduce((lineSum, line) => {
        const product = products.find((item) => item.id === line.productId)
        return lineSum + (product ? bundleLineUnitPrice(product, line.bundleSelections, products) * line.quantity : 0)
      }, 0)
      const merchantItemCount = merchantLines.reduce((lineSum, line) => lineSum + line.quantity, 0)
      const merchant = merchants.find((item) => item.id === lineMerchantId)
      const promotionLines = merchantLines.flatMap((line) => {
        const product = products.find((item) => item.id === line.productId)
        return product ? [{ productId: line.productId, unitPrice: bundleLineUnitPrice(product, line.bundleSelections, products), quantity: line.quantity }] : []
      })
      return sum + (bestPromotion(merchant?.promotions, merchantTotal, merchantItemCount, promotionLines)?.discountAmount ?? 0)
    }, 0)
    const afterMerchantDiscount = Math.max(0, roundMoney(cartTotal - merchantDiscount))

    const selectedVoucher = customerAccount?.profile.vouchers.find((voucher) => voucher.id === options?.voucherId)
    const estimatedVoucherDiscount = selectedVoucher && afterMerchantDiscount >= selectedVoucher.minSpend && selectedVoucher.remainingCount > 0
      ? Math.min(selectedVoucher.discountAmount, afterMerchantDiscount)
      : 0
    const itemCount = lines.reduce((sum, line) => sum + line.quantity, 0)
    const promotionLines = lines.flatMap((line) => {
      const product = products.find((item) => item.id === line.productId)
      return product ? [{ productId: line.productId, unitPrice: bundleLineUnitPrice(product, line.bundleSelections, products), quantity: line.quantity }] : []
    })
    const estimatedPlatformDiscount = bestPromotion(platformPromotions, Math.max(0, afterMerchantDiscount - estimatedVoucherDiscount), itemCount, promotionLines)?.discountAmount ?? 0
    const estimatedDiscount = roundMoney(merchantDiscount + estimatedVoucherDiscount + estimatedPlatformDiscount)

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
