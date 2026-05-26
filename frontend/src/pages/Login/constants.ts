import type { UserRole } from '@/objects/shared/ids'

export const roleOptions: Array<{ value: UserRole; label: string }> = [
  { value: 'customer', label: '顾客' },
  { value: 'merchant', label: '商家' },
  { value: 'rider', label: '骑手' },
]

export function getRoleLabel(role: UserRole) {
  if (role === 'customer') return '顾客'
  if (role === 'merchant') return '商家'
  return '骑手'
}
