import { create } from 'zustand'

import { runTask } from '@/apis/shared/client'
import { loginIO } from '@/apis/user/LoginAPI'
import { getDefaultRouteForRole, setAuthSessionIO } from '@/lib/auth-session'
import { UserRoles } from '@/objects/shared/ids'
import type { UserRole } from '@/objects/shared/ids'

type LoginPageStore = {
  role: UserRole
  account: string
  password: string
  errorMessage: string
  isSubmitting: boolean
  resetForm: () => void
  setRole: (role: UserRole) => void
  setAccount: (account: string) => void
  setPassword: (password: string) => void
  submit: () => Promise<string | null>
}

const initialState = {
  role: UserRoles.customer,
  account: '',
  password: '',
  errorMessage: '',
  isSubmitting: false,
}

export const useLoginPageStore = create<LoginPageStore>()((set, get) => ({
  ...initialState,
  resetForm: () => set(initialState),
  setRole: (role) => set({ role }),
  setAccount: (account) => set({ account }),
  setPassword: (password) => set({ password }),
  submit: async () => {
    const { role, account, password } = get()
    const trimmedAccount = account.trim()
    const trimmedPassword = password.trim()

    if (!trimmedAccount || !trimmedPassword) {
      set({ errorMessage: '请输入账号和密码。' })
      return null
    }

    set({ isSubmitting: true, errorMessage: '' })
    try {
      const data = await runTask(loginIO({ role, username: trimmedAccount, password: trimmedPassword }))
      await runTask(setAuthSessionIO(data.token, data.username, data.role))
      return getDefaultRouteForRole(data.role)
    } catch (error) {
      set({ errorMessage: error instanceof Error ? error.message : '登录失败' })
      return null
    } finally {
      set({ isSubmitting: false })
    }
  },
}))
