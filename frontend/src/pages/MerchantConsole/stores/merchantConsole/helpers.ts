import type { MerchantStoreProfile } from '@/objects/merchant/MerchantStoreProfile'
import { storeTagOptions } from '@/pages/MerchantConsole/objects/storeTags'

export const resolveSelectedStoreId = (currentSelectedStoreId: string | null, stores: MerchantStoreProfile[]) =>
  currentSelectedStoreId && stores.some((store) => store.merchant.id === currentSelectedStoreId)
    ? currentSelectedStoreId
    : (stores[0]?.merchant.id ?? null)

export const normalizeCreateStoreDraft = ({
  name,
  address,
  description,
  tags,
}: {
  name: string
  address: string
  description: string
  tags: string[]
}) => {
  const trimmedName = name.trim()
  const trimmedAddress = address.trim()
  const trimmedDescription = description.trim()
  const allowedTags = new Set<string>(storeTagOptions)
  const normalizedTags = tags.map((tag) => tag.trim()).filter((tag) => allowedTags.has(tag))
  const uniqueTags = Array.from(new Set(normalizedTags))

  if (!trimmedName || !trimmedAddress || !trimmedDescription || uniqueTags.length === 0) {
    return null
  }

  return {
    storeName: trimmedName,
    address: trimmedAddress,
    description: trimmedDescription,
    tags: uniqueTags,
  }
}

export const normalizeProductCategoryName = (categoryName: string) => categoryName.trim() || '默认分类'
