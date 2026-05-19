import { create } from 'zustand'

import { fetchRiderMeIO } from '@/api/rider/RiderMeApi'
import { grabRiderOrderIO, updateRiderOrderStatusIO } from '@/api/rider/RiderOrderApi'
import { runTask } from '@/api/shared/client'
import type { Order } from '@/objects/order/Order'
import type { RiderAccountPublic } from '@/objects/rider/RiderAccountPublic'

type RiderAppStore = {
  bootstrapDone: boolean
  loadError: string | null
  riderAccount: RiderAccountPublic | null
  availableOrders: Order[]
  resetPage: () => void
  refreshRider: () => Promise<RiderAccountPublic>
  bootstrap: () => Promise<void>
  grabOrder: (orderId: string) => Promise<void>
  updateOrderStatus: (orderId: string) => Promise<void>
}

const initialState = {
  bootstrapDone: false,
  loadError: null as string | null,
  riderAccount: null as RiderAccountPublic | null,
  availableOrders: [] as Order[],
}

export const useRiderAppStore = create<RiderAppStore>()((set, get) => ({
  ...initialState,
  resetPage: () => set(initialState),
  refreshRider: async () => {
    const me = await runTask(fetchRiderMeIO())
    set({ riderAccount: me.riderAccount, availableOrders: me.availableOrders })
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
    await runTask(updateRiderOrderStatusIO(orderId))
    await get().refreshRider()
  },
}))
