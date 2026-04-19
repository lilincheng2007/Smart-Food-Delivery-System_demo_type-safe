import { create } from 'zustand'

import { fetchOrdersPanelIO } from '@/api/admin/OrdersPanelApi'
import { runTask } from '@/api/shared/client'
import type { OrdersPanelResponse } from '@/objects/admin'

type OrderCenterStore = {
  panel: OrdersPanelResponse | null
  error: string | null
  resetPage: () => void
  loadPanel: () => Promise<void>
}

const initialState = {
  panel: null as OrdersPanelResponse | null,
  error: null as string | null,
}

export const useOrderCenterStore = create<OrderCenterStore>()((set) => ({
  ...initialState,
  resetPage: () => set(initialState),
  loadPanel: async () => {
    set({ error: null })
    try {
      const panel = await runTask(fetchOrdersPanelIO())
      set({ panel })
    } catch (error) {
      set({ error: error instanceof Error ? error.message : '加载失败' })
    }
  },
}))
