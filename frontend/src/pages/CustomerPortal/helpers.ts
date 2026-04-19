import type { CustomerTab } from '@/stores/pages/use-customer-portal-store'

export function isCustomerTab(value: string): value is CustomerTab {
  return value === 'home' || value === 'cart' || value === 'profile'
}
