import { create } from 'zustand'

import { grabRiderOrderIO } from '@/api/rider/RiderGrabOrderApi'
import { fetchRiderMeIO } from '@/api/rider/RiderMeApi'
import { redeemRiderTimeoutCardIO } from '@/api/rider/RiderRedeemTimeoutCardApi'
import { updateRiderOrderStatusIO } from '@/api/rider/RiderUpdateOrderStatusApi'
import { useRiderTimeoutCardIO } from '@/api/rider/RiderUseTimeoutCardApi'
import { runTask } from '@/api/shared/client'
import type { Order } from '@/objects/order/Order'
import type { RiderAccountPublic } from '@/objects/rider/RiderAccountPublic'
import type { RiderDeliverySettlement } from '@/objects/rider/RiderDeliverySettlement'
import type { RiderDeliveryStatus } from '@/objects/rider/RiderDeliveryStatus'
import type { RiderTimeoutCardRedeemResponse } from '@/objects/rider/RiderTimeoutCardRedeemResponse'
import type { RiderUseTimeoutCardResponse } from '@/objects/rider/RiderUseTimeoutCardResponse'
import type { OrderId } from '@/objects/shared/ids'
import { OrderStatuses } from '@/objects/shared/ids'

type RiderAppStore = {
  bootstrapDone: boolean
  loadError: string | null
  riderAccount: RiderAccountPublic | null
  availableOrders: Order[]
  deliveryStatuses: RiderDeliveryStatus[]
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
    const result = await runTask(useRiderTimeoutCardIO(orderId))
    await get().refreshRider()
    return result
  },
}))
