import type { RegisterRole } from '@/stores/pages/use-register-page-store'

export const registerRoleOptions: Array<{ value: RegisterRole; label: string }> = [
  { value: 'customer', label: '顾客' },
  { value: 'merchant', label: '商家' },
  { value: 'rider', label: '骑手' },
]

export function isRegisterRole(value: string): value is RegisterRole {
  return value === 'customer' || value === 'merchant' || value === 'rider'
}
