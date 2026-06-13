import type { Merchant } from '@/objects/merchant/Merchant'
import type { MerchantId, ProductId } from '@/objects/shared/ids'

import type { CustomerFavorites, FavoriteKind } from './types'

export const getLocalDateKey = () => {
  const now = new Date()
  const year = now.getFullYear()
  const month = `${now.getMonth() + 1}`.padStart(2, '0')
  const day = `${now.getDate()}`.padStart(2, '0')
  return `${year}-${month}-${day}`
}

export const resolveSelectedMerchantId = (currentSelectedMerchantId: MerchantId, merchants: Merchant[]) =>
  currentSelectedMerchantId && merchants.some((merchant) => merchant.id === currentSelectedMerchantId)
    ? currentSelectedMerchantId
    : (merchants[0]?.id ?? '' as MerchantId)

export const toggleCustomerFavorite = (
  favorites: CustomerFavorites,
  kind: FavoriteKind,
  id: MerchantId | ProductId,
): CustomerFavorites =>
  kind === 'merchant'
    ? {
        ...favorites,
        merchantIds: favorites.merchantIds.includes(id as MerchantId)
          ? favorites.merchantIds.filter((merchantId) => merchantId !== id)
          : [...favorites.merchantIds, id as MerchantId],
      }
    : {
        ...favorites,
        productIds: favorites.productIds.includes(id as ProductId)
          ? favorites.productIds.filter((productId) => productId !== id)
          : [...favorites.productIds, id as ProductId],
      }
