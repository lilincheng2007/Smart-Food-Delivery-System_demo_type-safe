import { create } from 'zustand'

import { aiMerchantProductDescriptionsIO } from '@/apis/ai/AIMerchantProductDescriptionsAPI'
import { aiMerchantStoreDescriptionIO } from '@/apis/ai/AIMerchantStoreDescriptionAPI'
import { acceptMerchantOrderIO } from '@/apis/merchant/MerchantOrderAcceptAPI'
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
import type { AIMerchantProductDescriptionsResponse } from '@/objects/ai/apiTypes/AIMerchantProductDescriptionsResponse'
import type { AIMerchantStoreDescriptionResponse } from '@/objects/ai/apiTypes/AIMerchantStoreDescriptionResponse'
import type { CreateProductRequest } from '@/objects/merchant/apiTypes/CreateProductRequest'
import type { MerchantAccountPublic } from '@/objects/merchant/MerchantAccountPublic'
import type { MerchantStoreProfile } from '@/objects/merchant/MerchantStoreProfile'
import type { Product } from '@/objects/merchant/Product'
import type { ProductDescriptionPatch } from '@/objects/merchant/ProductDescriptionPatch'
import type { UpdateProductRequest } from '@/objects/merchant/apiTypes/UpdateProductRequest'
import type { MerchantId, OrderId, ProductId } from '@/objects/shared/ids'
import type { Promotion } from '@/objects/shared/Promotion'
import type { StoreOnboardingRequest } from '@/objects/admin/StoreOnboardingRequest'

export type MerchantTab = 'products' | 'orders' | 'business' | 'reviews' | 'profile'

type MerchantConsoleStore = {
  bootstrapDone: boolean
  loadError: string | null
  merchantAccount: MerchantAccountPublic | null
  activeTab: MerchantTab
  isStoreDialogOpen: boolean
  selectedStoreId: string | null
  newStoreName: string
  newStoreAddress: string
  newStoreDescription: string
  stores: MerchantStoreProfile[]
  storeOnboardingRequests: StoreOnboardingRequest[]
  resetPage: () => void
  setActiveTab: (tab: MerchantTab) => void
  setIsStoreDialogOpen: (open: boolean) => void
  setSelectedStoreId: (storeId: string | null) => void
  setNewStoreName: (name: string) => void
  setNewStoreAddress: (address: string) => void
  setNewStoreDescription: (description: string) => void
  refreshMerchant: () => Promise<MerchantAccountPublic>
  bootstrap: () => Promise<void>
  createStore: () => Promise<string | null>
  acceptOrder: (orderId: OrderId) => Promise<void>
  rejectOrder: (orderId: OrderId) => Promise<void>
  finishCooking: (orderId: OrderId) => Promise<void>
  createProduct: (input: CreateProductRequest) => Promise<void>
  updateProduct: (productId: string, input: UpdateProductRequest) => Promise<void>
  uploadProductImageFile: (productId: ProductId, file: File) => Promise<Product>
  generateStoreDescription: (merchantId: MerchantId, keywords: string) => Promise<AIMerchantStoreDescriptionResponse>
  saveStoreDescription: (merchantId: MerchantId, description: string) => Promise<void>
  saveStoreAnnouncement: (merchantId: MerchantId, announcement: string) => Promise<void>
  saveStorePromotions: (merchantId: MerchantId, promotions: Promotion[]) => Promise<void>
  generateProductDescriptions: (merchantId: MerchantId, keywords: string) => Promise<AIMerchantProductDescriptionsResponse>
  saveProductDescriptions: (merchantId: MerchantId, descriptions: ProductDescriptionPatch[]) => Promise<void>
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
  newStoreDescription: '',
  stores: [] as MerchantStoreProfile[],
  storeOnboardingRequests: [] as StoreOnboardingRequest[],
}

export const useMerchantConsoleStore = create<MerchantConsoleStore>()((set, get) => ({
  ...initialState,
  resetPage: () => set(initialState),
  setActiveTab: (activeTab) => set({ activeTab }),
  setIsStoreDialogOpen: (isStoreDialogOpen) => set({ isStoreDialogOpen }),
  setSelectedStoreId: (selectedStoreId) => set({ selectedStoreId }),
  setNewStoreName: (newStoreName) => set({ newStoreName }),
  setNewStoreAddress: (newStoreAddress) => set({ newStoreAddress }),
  setNewStoreDescription: (newStoreDescription) => set({ newStoreDescription }),
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
    const { newStoreName, newStoreAddress, newStoreDescription } = get()
    const trimmedName = newStoreName.trim()
    const trimmedAddress = newStoreAddress.trim()
    const trimmedDescription = newStoreDescription.trim()

    if (!trimmedName || !trimmedAddress || !trimmedDescription) {
      return null
    }

    const requestId = await runTask(
      createMerchantStoreIO({
        storeName: trimmedName,
        address: trimmedAddress,
        description: trimmedDescription,
      }),
    )
    await get().refreshMerchant()
    set({
      newStoreName: '',
      newStoreAddress: '',
      newStoreDescription: '',
    })
    return requestId
  },
  acceptOrder: async (orderId) => {
    await runTask(acceptMerchantOrderIO(orderId))
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
  createProduct: async (input) => {
    await runTask(createMerchantProductIO(input))
    await get().refreshMerchant()
  },
  updateProduct: async (productId, input) => {
    const updated = await runTask(updateMerchantProductIO(productId, input))
    const normalizedCategoryName = input.categoryName.trim() || '默认分类'
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
