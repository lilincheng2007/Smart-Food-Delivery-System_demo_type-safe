import type { CheckoutDeliverySnapshot } from '@/apis/order/CheckoutAPI'
import type { OrderMerchantNote } from '@/objects/order/apiTypes/CheckoutRequest'
import type { CheckoutBundleSelection } from '@/objects/order/CheckoutLine'
import type { AIDietWeeklyReportResponse } from '@/objects/ai/apiTypes/AIDietWeeklyReportResponse'
import type { AIOrderProgressNarrativesResponse } from '@/objects/ai/apiTypes/AIOrderProgressNarrativesResponse'
import type { Merchant } from '@/objects/merchant/Merchant'
import type { Product } from '@/objects/merchant/Product'
import type { Order } from '@/objects/order/Order'
import type { MerchantReviewsResponse } from '@/objects/review/apiTypes/MerchantReviewsResponse'
import type { CustomerAccountPublic } from '@/objects/user/CustomerAccountPublic'
import type { CustomerDeliveryContact } from '@/objects/user/CustomerDeliveryContact'
import type { Promotion } from '@/objects/shared/Promotion'
import type { MerchantId, OrderId, ProductId, VoucherId } from '@/objects/shared/ids'

export type CustomerTab = 'home' | 'cart' | 'profile'

export interface CartLine {
  merchantId: MerchantId
  productId: ProductId
  quantity: number
  bundleSelections?: CheckoutBundleSelection[]
}

export type FavoriteKind = 'merchant' | 'product'

export interface CustomerFavorites {
  merchantIds: MerchantId[]
  productIds: ProductId[]
}

export type CustomerPortalStore = {
  bootstrapDone: boolean
  loadError: string | null
  customerAccount: CustomerAccountPublic | null
  merchants: Merchant[]
  products: Product[]
  platformPromotions: Promotion[]
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
  addBundleToCart: (merchantId: MerchantId, productId: ProductId, bundleSelections: CheckoutBundleSelection[]) => void
  changeQuantity: (merchantId: MerchantId, productId: ProductId, nextQuantity: number) => void
  changeCartLineQuantity: (lineKey: string, nextQuantity: number) => void
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
