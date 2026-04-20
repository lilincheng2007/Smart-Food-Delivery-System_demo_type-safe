import { create } from 'zustand'

import { finishMerchantOrderCookingIO } from '@/api/merchant/MerchantOrderApi'
import { createMerchantProductIO, updateMerchantProductIO } from '@/api/merchant/MerchantProductApi'
import { fetchMerchantMeIO } from '@/api/merchant/MerchantMeApi'
import { uploadMerchantStoreImageFileIO } from '@/api/merchant/MerchantStoreImageFileApi'
import { updateMerchantStoreImageIO } from '@/api/merchant/MerchantStoreImageApi'
import { createMerchantStoreIO } from '@/api/merchant/MerchantStoreApi'
import { runTask } from '@/api/shared/client'
import type {
  CreateProductRequest,
  MerchantAccountPublic,
  MerchantStoreProfile,
  UpdateProductRequest,
} from '@/objects/merchant'

export type MerchantTab = 'products' | 'orders' | 'profile'

type MerchantConsoleStore = {
  bootstrapDone: boolean
  loadError: string | null
  merchantAccount: MerchantAccountPublic | null
  activeTab: MerchantTab
  isStoreDialogOpen: boolean
  selectedStoreId: string | null
  newStoreName: string
  newStoreAddress: string
  stores: MerchantStoreProfile[]
  resetPage: () => void
  setActiveTab: (tab: MerchantTab) => void
  setIsStoreDialogOpen: (open: boolean) => void
  setSelectedStoreId: (storeId: string | null) => void
  setNewStoreName: (name: string) => void
  setNewStoreAddress: (address: string) => void
  refreshMerchant: () => Promise<MerchantAccountPublic>
  bootstrap: () => Promise<void>
  createStore: () => Promise<string | null>
  finishCooking: (orderId: string) => Promise<void>
  createProduct: (input: CreateProductRequest) => Promise<void>
  updateProduct: (productId: string, input: UpdateProductRequest) => Promise<void>
  updateStoreImage: (merchantId: string, imageUrl: string) => Promise<void>
  uploadStoreImageFile: (merchantId: string, file: File) => Promise<void>
}

const initialState = {
  bootstrapDone: false,
  loadError: null as string | null,
  merchantAccount: null as MerchantAccountPublic | null,
  activeTab: 'products' as MerchantTab,
  isStoreDialogOpen: true,
  selectedStoreId: null as string | null,
  newStoreName: '',
  newStoreAddress: '',
  stores: [] as MerchantStoreProfile[],
}

export const useMerchantConsoleStore = create<MerchantConsoleStore>()((set, get) => ({
  ...initialState,
  resetPage: () => set(initialState),
  setActiveTab: (activeTab) => set({ activeTab }),
  setIsStoreDialogOpen: (isStoreDialogOpen) => set({ isStoreDialogOpen }),
  setSelectedStoreId: (selectedStoreId) => set({ selectedStoreId }),
  setNewStoreName: (newStoreName) => set({ newStoreName }),
  setNewStoreAddress: (newStoreAddress) => set({ newStoreAddress }),
  refreshMerchant: async () => {
    const me = await runTask(fetchMerchantMeIO())
    const nextStores = me.merchantAccount.profile.stores
    const currentSelectedStoreId = get().selectedStoreId
    const nextSelectedStoreId =
      currentSelectedStoreId && nextStores.some((store) => store.merchant.id === currentSelectedStoreId)
        ? currentSelectedStoreId
        : (nextStores[0]?.merchant.id ?? null)

    set({
      merchantAccount: me.merchantAccount,
      stores: nextStores,
      selectedStoreId: nextSelectedStoreId,
    })
    return me.merchantAccount
  },
  bootstrap: async () => {
    set({ bootstrapDone: false, loadError: null })
    try {
      await get().refreshMerchant()
    } catch (error) {
      set({ loadError: error instanceof Error ? error.message : '加载失败' })
    } finally {
      set({ bootstrapDone: true })
    }
  },
  createStore: async () => {
    const { newStoreName, newStoreAddress } = get()
    const trimmedName = newStoreName.trim()
    const trimmedAddress = newStoreAddress.trim()

    if (!trimmedName || !trimmedAddress) {
      return null
    }

    const created = await runTask(createMerchantStoreIO({ storeName: trimmedName, address: trimmedAddress }))
    await get().refreshMerchant()
    set({
      selectedStoreId: created.merchantId,
      newStoreName: '',
      newStoreAddress: '',
      isStoreDialogOpen: false,
      activeTab: 'products',
    })
    return created.merchantId
  },
  finishCooking: async (orderId) => {
    await runTask(finishMerchantOrderCookingIO(orderId))
    await get().refreshMerchant()
  },
  createProduct: async (input) => {
    await runTask(createMerchantProductIO(input))
    await get().refreshMerchant()
  },
  updateProduct: async (productId, input) => {
    await runTask(updateMerchantProductIO(productId, input))
    await get().refreshMerchant()
  },
  updateStoreImage: async (merchantId, imageUrl) => {
    await runTask(updateMerchantStoreImageIO(merchantId, { imageUrl }))
    await get().refreshMerchant()
  },
  uploadStoreImageFile: async (merchantId, file) => {
    const res = await runTask(uploadMerchantStoreImageFileIO(merchantId, file))
    const patchStore = (st: MerchantStoreProfile): MerchantStoreProfile =>
      st.merchant.id === merchantId ? { ...st, merchant: { ...st.merchant, imageUrl: res.imageUrl } } : st
    set((state) => ({
      stores: state.stores.map(patchStore),
      merchantAccount: state.merchantAccount
        ? {
            ...state.merchantAccount,
            profile: {
              ...state.merchantAccount.profile,
              stores: state.merchantAccount.profile.stores.map(patchStore),
            },
          }
        : state.merchantAccount,
    }))
    await get().refreshMerchant()
  },
}))
