import type { AIMerchantProductDescriptionsResponse } from '@/objects/ai/apiTypes/AIMerchantProductDescriptionsResponse'
import type { AIMerchantStoreDescriptionResponse } from '@/objects/ai/apiTypes/AIMerchantStoreDescriptionResponse'
import type { StoreOnboardingRequest } from '@/objects/admin/StoreOnboardingRequest'
import type { CreateProductRequest } from '@/objects/merchant/apiTypes/CreateProductRequest'
import type { UpdateProductRequest } from '@/objects/merchant/apiTypes/UpdateProductRequest'
import type { MerchantAccountPublic } from '@/objects/merchant/MerchantAccountPublic'
import type { MerchantBusinessStatus, MerchantHolidayBusinessHour, MerchantWeeklyBusinessHour } from '@/objects/merchant/MerchantBusinessHours'
import type { MerchantStoreProfile } from '@/objects/merchant/MerchantStoreProfile'
import type { Product } from '@/objects/merchant/Product'
import type { ProductDescriptionPatch } from '@/objects/merchant/ProductDescriptionPatch'
import type { MerchantId, OrderId, ProductId } from '@/objects/shared/ids'
import type { Promotion } from '@/objects/shared/Promotion'

export type MerchantTab = 'products' | 'orders' | 'business' | 'reviews' | 'profile'

export type MerchantConsoleStore = {
  bootstrapDone: boolean
  loadError: string | null
  merchantAccount: MerchantAccountPublic | null
  sessionAccount: string | null
  activeTab: MerchantTab
  isStoreDialogOpen: boolean
  selectedStoreId: string | null
  newStoreName: string
  newStoreAddress: string
  newStoreDescription: string
  newStoreTags: string[]
  stores: MerchantStoreProfile[]
  storeOnboardingRequests: StoreOnboardingRequest[]
  resetPage: () => void
  prepareForSession: (account: string | null) => void
  setActiveTab: (tab: MerchantTab) => void
  setIsStoreDialogOpen: (open: boolean) => void
  setSelectedStoreId: (storeId: string | null) => void
  setNewStoreName: (name: string) => void
  setNewStoreAddress: (address: string) => void
  setNewStoreDescription: (description: string) => void
  setNewStoreTags: (tags: string[]) => void
  refreshMerchant: () => Promise<MerchantAccountPublic>
  bootstrap: () => Promise<void>
  createStore: () => Promise<string | null>
  acceptOrder: (orderId: OrderId, prepMinutes?: number) => Promise<void>
  rejectOrder: (orderId: OrderId) => Promise<void>
  finishCooking: (orderId: OrderId) => Promise<void>
  delayPrep: (orderId: OrderId, extraMinutes: number, reason: string) => Promise<void>
  createProduct: (input: CreateProductRequest) => Promise<Product>
  updateProduct: (productId: string, input: UpdateProductRequest) => Promise<void>
  uploadProductImageFile: (productId: ProductId, file: File) => Promise<Product>
  generateStoreDescription: (merchantId: MerchantId, keywords: string) => Promise<AIMerchantStoreDescriptionResponse>
  saveStoreDescription: (merchantId: MerchantId, description: string) => Promise<void>
  saveStoreAnnouncement: (merchantId: MerchantId, announcement: string) => Promise<void>
  saveBusinessHours: (input: { merchantId: MerchantId; businessStatus: MerchantBusinessStatus; weeklyBusinessHours: MerchantWeeklyBusinessHour[]; holidayBusinessHours: MerchantHolidayBusinessHour[] }) => Promise<void>
  saveStorePromotions: (merchantId: MerchantId, promotions: Promotion[]) => Promise<void>
  generateProductDescriptions: (merchantId: MerchantId, keywords: string) => Promise<AIMerchantProductDescriptionsResponse>
  saveProductDescriptions: (merchantId: MerchantId, descriptions: ProductDescriptionPatch[]) => Promise<void>
  updateStoreImage: (merchantId: string, imageUrl: string) => Promise<void>
  uploadStoreImageFile: (merchantId: string, file: File) => Promise<void>
}
