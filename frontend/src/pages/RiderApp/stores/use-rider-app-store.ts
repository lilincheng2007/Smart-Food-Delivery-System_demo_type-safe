import { create } from 'zustand'

import { grabRiderOrderIO } from '@/apis/rider/RiderGrabOrderAPI'
import { fetchRiderMeIO } from '@/apis/rider/RiderMeAPI'
import { redeemRiderTimeoutCardIO } from '@/apis/rider/RiderRedeemTimeoutCardAPI'
import { updateRiderOrderStatusIO } from '@/apis/rider/RiderUpdateOrderStatusAPI'
import { riderUseTimeoutCardIO } from '@/apis/rider/RiderUseTimeoutCardAPI'
import { runTask } from '@/apis/shared/client'
import type { Order } from '@/objects/order/Order'
import type { RiderAccountPublic } from '@/objects/rider/RiderAccountPublic'
import type { RiderDeliverySettlement } from '@/objects/rider/RiderDeliverySettlement'
import type { RiderDeliveryStatus } from '@/objects/rider/RiderDeliveryStatus'
import type { ReviewSummary } from '@/objects/review/ReviewSummary'
import type { RiderReview } from '@/objects/review/RiderReview'
import type { RiderTimeoutCardRedeemResponse } from '@/objects/rider/apiTypes/RiderTimeoutCardRedeemResponse'
import type { RiderUseTimeoutCardResponse } from '@/objects/rider/apiTypes/RiderUseTimeoutCardResponse'
import type { OrderId } from '@/objects/shared/ids'
import { OrderStatuses } from '@/objects/shared/ids'

type RiderAppStore = {
  bootstrapDone: boolean
  loadError: string | null
  riderAccount: RiderAccountPublic | null
  availableOrders: Order[]
  deliveryStatuses: RiderDeliveryStatus[]
  reviewSummary: ReviewSummary
  reviews: RiderReview[]
  resetPage: () => void
  refreshRider: () => Promise<RiderAccountPublic>
  bootstrap: () => Promise<void>
  grabOrder: (orderId: OrderId) => Promise<void>
  updateOrderStatus: (orderId: OrderId) => Promise<RiderDeliverySettlement>
  redeemTimeoutCard: () => Promise<RiderTimeoutCardRedeemResponse>
  useTimeoutCard: (orderId: OrderId) => Promise<RiderUseTimeoutCardResponse>
}

const initialState = {
  bootstrapDone: false,
  loadError: null as string | null,
  riderAccount: null as RiderAccountPublic | null,
  availableOrders: [] as Order[],
  deliveryStatuses: [] as RiderDeliveryStatus[],
  reviewSummary: { averageRating: 5.0, reviewCount: 0 } as ReviewSummary,
  reviews: [] as RiderReview[],
}

export const useRiderAppStore = create<RiderAppStore>()((set, get) => ({
  ...initialState,
  resetPage: () => set(initialState),
  refreshRider: async () => {
    const me = await runTask(fetchRiderMeIO())
    set({
      riderAccount: me.riderAccount,
      availableOrders: me.availableOrders,
      deliveryStatuses: me.deliveryStatuses,
      reviewSummary: me.reviewSummary ?? { averageRating: 5.0, reviewCount: 0 },
      reviews: me.reviews ?? [],
    })
    return me.riderAccount
  },
  bootstrap: async () => {
    set({ bootstrapDone: false, loadError: null })
    try {
      await get().refreshRider()
    } catch (error) {
      set({ loadError: error instanceof Error ? error.message : '加载失败' })
    } finally {
      set({ bootstrapDone: true })
    }
  },
  grabOrder: async (orderId) => {
    await runTask(grabRiderOrderIO(orderId))
    await get().refreshRider()
  },
  updateOrderStatus: async (orderId) => {
    const result = await runTask(updateRiderOrderStatusIO(orderId, OrderStatuses.delivered))
    await get().refreshRider()
    return result
  },
  redeemTimeoutCard: async () => {
    const result = await runTask(redeemRiderTimeoutCardIO())
    await get().refreshRider()
    return result
  },
  useTimeoutCard: async (orderId) => {
    const result = await runTask(riderUseTimeoutCardIO(orderId))
    await get().refreshRider()
    return result
  },
}))
