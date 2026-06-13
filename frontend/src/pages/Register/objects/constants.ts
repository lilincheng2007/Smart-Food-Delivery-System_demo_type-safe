import { UserRoles } from '@/objects/shared/ids'
import type { RegisterRole } from '@/pages/Register/stores/use-register-page-store'

export const registerRoleOptions: Array<{ value: RegisterRole; label: string }> = [
  { value: UserRoles.customer, label: '顾客' },
  { value: UserRoles.merchant, label: '商家' },
  { value: UserRoles.rider, label: '骑手' },
]

export function isRegisterRole(value: string): value is RegisterRole {
  return Object.values(UserRoles).includes(value as RegisterRole)
}
