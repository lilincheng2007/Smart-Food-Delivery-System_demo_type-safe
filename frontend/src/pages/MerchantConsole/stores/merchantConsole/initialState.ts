import type { StoreOnboardingRequest } from '@/objects/admin/StoreOnboardingRequest'
import type { MerchantAccountPublic } from '@/objects/merchant/MerchantAccountPublic'
import type { MerchantStoreProfile } from '@/objects/merchant/MerchantStoreProfile'

import type { MerchantTab } from './types'

export const initialState = {
  bootstrapDone: false,
  loadError: null as string | null,
  merchantAccount: null as MerchantAccountPublic | null,
  sessionAccount: null as string | null,
  activeTab: 'products' as MerchantTab,
  isStoreDialogOpen: true,
  selectedStoreId: null as string | null,
  newStoreName: '',
  newStoreAddress: '',
  newStoreDescription: '',
  newStoreTags: [] as string[],
  stores: [] as MerchantStoreProfile[],
  storeOnboardingRequests: [] as StoreOnboardingRequest[],
}
