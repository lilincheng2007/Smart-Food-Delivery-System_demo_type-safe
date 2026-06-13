import type { MerchantId, ProductId } from '@/objects/shared/ids'

import type { CustomerFavorites } from './types'

const CustomerFavoritesStorageKey = 'delivery-customer-favorites'

export const readStoredFavorites = (): CustomerFavorites => {
  if (typeof window === 'undefined') {
    return { merchantIds: [], productIds: [] }
  }

  try {
    const raw = window.localStorage.getItem(CustomerFavoritesStorageKey)
    if (!raw) {
      return { merchantIds: [], productIds: [] }
    }

    const parsed = JSON.parse(raw) as Partial<CustomerFavorites>
    return {
      merchantIds: Array.isArray(parsed.merchantIds) ? (parsed.merchantIds as MerchantId[]) : [],
      productIds: Array.isArray(parsed.productIds) ? (parsed.productIds as ProductId[]) : [],
    }
  } catch {
    return { merchantIds: [], productIds: [] }
  }
}

export const writeStoredFavorites = (favorites: CustomerFavorites) => {
  if (typeof window === 'undefined') {
    return
  }

  window.localStorage.setItem(CustomerFavoritesStorageKey, JSON.stringify(favorites))
}
