import { create } from 'zustand'

import { aiMerchantProductDescriptionsIO } from '@/apis/ai/AIMerchantProductDescriptionsAPI'
import { updateMerchantBusinessHoursIO } from '@/apis/merchant/MerchantBusinessHoursAPI'
import { aiMerchantStoreDescriptionIO } from '@/apis/ai/AIMerchantStoreDescriptionAPI'
import { acceptMerchantOrderIO } from '@/apis/merchant/MerchantOrderAcceptAPI'
import { delayMerchantOrderPrepIO } from '@/apis/merchant/MerchantOrderPrepDelayAPI'
import { rejectMerchantOrderIO } from '@/apis/merchant/MerchantOrderRejectAPI'
import { finishMerchantOrderCookingIO } from '@/apis/merchant/MerchantOrderReadyAPI'
import { createMerchantProductIO } from '@/apis/merchant/MerchantCreateProductAPI'
import { updateMerchantProductDescriptionsIO } from '@/apis/merchant/MerchantProductDescriptionsAPI'
import { uploadMerchantProductImageFileIO } from '@/apis/merchant/MerchantProductImageFileAPI'
import { updateMerchantProductIO } from '@/apis/merchant/MerchantProductAPI'
import { fetchMerchantMeIO } from '@/apis/merchant/MerchantMeAPI'
import { uploadMerchantStoreImageFileIO } from '@/apis/merchant/MerchantStoreImageFileAPI'
import { updateMerchantStoreAnnouncementIO } from '@/apis/merchant/MerchantStoreAnnouncementAPI'
import { updateMerchantStorePromotionsIO } from '@/apis/merchant/MerchantStorePromotionsAPI'
import { updateMerchantStoreDescriptionIO } from '@/apis/merchant/MerchantStoreDescriptionAPI'
import { updateMerchantStoreImageIO } from '@/apis/merchant/MerchantStoreImageAPI'
import { createMerchantStoreIO } from '@/apis/merchant/MerchantStoreAPI'
import { runTask } from '@/apis/shared/client'
import type { MerchantStoreProfile } from '@/objects/merchant/MerchantStoreProfile'
import { normalizeCreateStoreDraft, normalizeProductCategoryName, resolveSelectedStoreId } from './merchantConsole/helpers'
import { initialState } from './merchantConsole/initialState'
import type { MerchantConsoleStore, MerchantTab } from './merchantConsole/types'

export type { MerchantTab }

export const useMerchantConsoleStore = create<MerchantConsoleStore>()((set, get) => ({
  ...initialState,
  resetPage: () => set(initialState),
  prepareForSession: (account) => {
    if (get().sessionAccount !== account) {
      set({ ...initialState, sessionAccount: account })
    }
  },
  setActiveTab: (activeTab) => set({ activeTab }),
  setIsStoreDialogOpen: (isStoreDialogOpen) => set({ isStoreDialogOpen }),
  setSelectedStoreId: (selectedStoreId) => set({ selectedStoreId }),
  setNewStoreName: (newStoreName) => set({ newStoreName }),
  setNewStoreAddress: (newStoreAddress) => set({ newStoreAddress }),
  setNewStoreDescription: (newStoreDescription) => set({ newStoreDescription }),
  setNewStoreTags: (newStoreTags) => set({ newStoreTags }),
  refreshMerchant: async () => {
    const me = await runTask(fetchMerchantMeIO())
    const nextStores = me.merchantAccount.profile.stores
    const currentSelectedStoreId = get().selectedStoreId
    const nextSelectedStoreId = resolveSelectedStoreId(currentSelectedStoreId, nextStores)

    set({
      merchantAccount: me.merchantAccount,
      stores: nextStores,
      storeOnboardingRequests: me.onboardingRequests ?? [],
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
    const { newStoreName, newStoreAddress, newStoreDescription, newStoreTags } = get()
    const createStoreInput = normalizeCreateStoreDraft({
      name: newStoreName,
      address: newStoreAddress,
      description: newStoreDescription,
      tags: newStoreTags,
    })

    if (!createStoreInput) {
      return null
    }

    const requestId = await runTask(createMerchantStoreIO(createStoreInput))
    await get().refreshMerchant()
    set({
      newStoreName: '',
      newStoreAddress: '',
      newStoreDescription: '',
      newStoreTags: [],
    })
    return requestId
  },
  acceptOrder: async (orderId, prepMinutes) => {
    await runTask(acceptMerchantOrderIO(orderId, prepMinutes))
    await get().refreshMerchant()
  },
  rejectOrder: async (orderId) => {
    await runTask(rejectMerchantOrderIO(orderId))
    await get().refreshMerchant()
  },
  finishCooking: async (orderId) => {
    await runTask(finishMerchantOrderCookingIO(orderId))
    await get().refreshMerchant()
  },
  delayPrep: async (orderId, extraMinutes, reason) => {
    await runTask(delayMerchantOrderPrepIO(orderId, extraMinutes, reason))
    await get().refreshMerchant()
  },
  createProduct: async (input) => {
    const created = await runTask(createMerchantProductIO(input))
    await get().refreshMerchant()
    return created
  },
  updateProduct: async (productId, input) => {
    const updated = await runTask(updateMerchantProductIO(productId, input))
    const normalizedCategoryName = normalizeProductCategoryName(input.categoryName)
    const patchedProduct = { ...updated, categoryName: normalizedCategoryName }
    const patchStore = (st: MerchantStoreProfile): MerchantStoreProfile =>
      st.merchant.id === patchedProduct.merchantId
        ? {
            ...st,
            products: st.products.map((product) => (product.id === patchedProduct.id ? patchedProduct : product)),
          }
        : st
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
  },
  uploadProductImageFile: async (productId, file) => {
    const updated = await runTask(uploadMerchantProductImageFileIO(productId, file))
    const patchStore = (st: MerchantStoreProfile): MerchantStoreProfile =>
      st.merchant.id === updated.merchantId
        ? {
            ...st,
            products: st.products.map((product) => (product.id === updated.id ? updated : product)),
          }
        : st
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
    return updated
  },
  generateStoreDescription: async (merchantId, keywords) =>
    runTask(aiMerchantStoreDescriptionIO({ merchantId, keywords })),
  saveStoreDescription: async (merchantId, description) => {
    await runTask(updateMerchantStoreDescriptionIO(merchantId, description))
    await get().refreshMerchant()
  },
  saveStoreAnnouncement: async (merchantId, announcement) => {
    await runTask(updateMerchantStoreAnnouncementIO(merchantId, announcement))
    await get().refreshMerchant()
  },
  saveBusinessHours: async (input) => {
    await runTask(updateMerchantBusinessHoursIO(input))
    await get().refreshMerchant()
  },
  saveStorePromotions: async (merchantId, promotions) => {
    await runTask(updateMerchantStorePromotionsIO(merchantId, promotions))
    await get().refreshMerchant()
  },
  generateProductDescriptions: async (merchantId, keywords) =>
    runTask(aiMerchantProductDescriptionsIO({ merchantId, keywords })),
  saveProductDescriptions: async (merchantId, descriptions) => {
    await runTask(updateMerchantProductDescriptionsIO(merchantId, descriptions))
    await get().refreshMerchant()
  },
  updateStoreImage: async (merchantId, imageUrl) => {
    await runTask(updateMerchantStoreImageIO(merchantId, { imageUrl }))
    await get().refreshMerchant()
  },
  uploadStoreImageFile: async (merchantId, file) => {
    const imageUrl = await runTask(uploadMerchantStoreImageFileIO(merchantId, file))
    const patchStore = (st: MerchantStoreProfile): MerchantStoreProfile =>
      st.merchant.id === merchantId ? { ...st, merchant: { ...st.merchant, imageUrl } } : st
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
