import { create } from 'zustand'

import { fetchOverviewIO } from '@/api/admin/OverviewApi'
import { runTask } from '@/api/shared/client'
import type { OverviewResponse } from '@/objects/admin/OverviewResponse'

type DeliveryDashboardStore = {
  overview: OverviewResponse | null
  overviewError: string | null
  resetPage: () => void
  loadOverview: () => Promise<void>
}

const initialState = {
  overview: null as OverviewResponse | null,
  overviewError: null as string | null,
}

export const useDeliveryDashboardStore = create<DeliveryDashboardStore>()((set) => ({
  ...initialState,
  resetPage: () => set(initialState),
  loadOverview: async () => {
    set({ overviewError: null })
    try {
      const overview = await runTask(fetchOverviewIO())
      set({ overview })
    } catch (error) {
      set({ overviewError: error instanceof Error ? error.message : '加载失败' })
    }
  },
}))
