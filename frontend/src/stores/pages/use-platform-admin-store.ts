import { create } from 'zustand'

import { fetchAdminMeIO } from '@/api/admin/AdminMeApi'
import { fetchPlatformMetaIO } from '@/api/admin/PlatformMetaApi'
import { runTask } from '@/api/shared/client'
import type { PlatformMetaResponse } from '@/objects/admin'

type PlatformAdminStore = {
  meta: PlatformMetaResponse | null
  error: string | null
  adminName: string
  resetPage: () => void
  bootstrap: () => Promise<void>
}

const initialState = {
  meta: null as PlatformMetaResponse | null,
  error: null as string | null,
  adminName: '',
}

export const usePlatformAdminStore = create<PlatformAdminStore>()((set) => ({
  ...initialState,
  resetPage: () => set(initialState),
  bootstrap: async () => {
    set({ error: null })
    try {
      const me = await runTask(fetchAdminMeIO())
      const meta = await runTask(fetchPlatformMetaIO())
      set({ adminName: me.adminAccount.displayName, meta })
    } catch (error) {
      set({ error: error instanceof Error ? error.message : '加载失败' })
    }
  },
}))
