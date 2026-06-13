import type { AIDietWeeklyReportResponse } from '@/objects/ai/apiTypes/AIDietWeeklyReportResponse'
import type { AIOrderProgressNarrativesResponse } from '@/objects/ai/apiTypes/AIOrderProgressNarrativesResponse'
import type { Merchant } from '@/objects/merchant/Merchant'
import type { Product } from '@/objects/merchant/Product'
import type { Order } from '@/objects/order/Order'
import type { CustomerAccountPublic } from '@/objects/user/CustomerAccountPublic'
import type { Promotion } from '@/objects/shared/Promotion'
import type { MerchantId } from '@/objects/shared/ids'

import { readStoredFavorites } from './favoritesStorage'
import type { CartLine, CustomerTab } from './types'

export const initialState = {
  bootstrapDone: false,
  loadError: null as string | null,
  customerAccount: null as CustomerAccountPublic | null,
  merchants: [] as Merchant[],
  products: [] as Product[],
  platformPromotions: [] as Promotion[],
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
