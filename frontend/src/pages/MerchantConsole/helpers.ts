import type { MerchantTab } from '@/stores/pages/use-merchant-console-store'

export function isMerchantTab(value: string): value is MerchantTab {
  return value === 'products' || value === 'orders' || value === 'profile'
}
