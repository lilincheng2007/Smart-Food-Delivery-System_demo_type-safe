import type { MerchantTab } from '@/pages/MerchantConsole/stores/use-merchant-console-store'

export function isMerchantTab(value: string): value is MerchantTab {
  return value === 'products' || value === 'orders' || value === 'business' || value === 'reviews' || value === 'profile'
}
