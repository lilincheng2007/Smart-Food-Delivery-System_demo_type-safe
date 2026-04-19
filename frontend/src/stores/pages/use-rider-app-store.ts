import { create } from 'zustand'

import { fetchRiderMeIO } from '@/api/rider/RiderMeApi'
import { runTask } from '@/api/shared/client'
import type { RiderAccountPublic } from '@/objects/rider'

type RiderAppStore = {
  bootstrapDone: boolean
  loadError: string | null
  riderAccount: RiderAccountPublic | null
  resetPage: () => void
  bootstrap: () => Promise<void>
}

const initialState = {
  bootstrapDone: false,
  loadError: null as string | null,
  riderAccount: null as RiderAccountPublic | null,
}

export const useRiderAppStore = create<RiderAppStore>()((set) => ({
  ...initialState,
  resetPage: () => set(initialState),
  bootstrap: async () => {
    set({ bootstrapDone: false, loadError: null })
    try {
      const me = await runTask(fetchRiderMeIO())
      set({ riderAccount: me.riderAccount })
    } catch (error) {
      set({ loadError: error instanceof Error ? error.message : '加载失败' })
    } finally {
      set({ bootstrapDone: true })
    }
  },
}))
