import type { CustomerTab } from '@/pages/CustomerPortal/stores/use-customer-portal-store'

export function isCustomerTab(value: string): value is CustomerTab {
  return value === 'home' || value === 'cart' || value === 'profile'
}
