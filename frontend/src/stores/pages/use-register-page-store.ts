import { create } from 'zustand'

import { runTask } from '@/api/shared/client'
import { registerIO } from '@/api/user/RegisterApi'
import type { UserRole } from '@/objects/shared'

export type RegisterRole = Exclude<UserRole, 'admin'>

type RegisterPageStore = {
  role: RegisterRole
  account: string
  password: string
  confirmPassword: string
  errorMessage: string
  isSubmitting: boolean
  resetForm: () => void
  setRole: (role: RegisterRole) => void
  setAccount: (account: string) => void
  setPassword: (password: string) => void
  setConfirmPassword: (confirmPassword: string) => void
  submit: () => Promise<boolean>
}

const initialState = {
  role: 'customer' as RegisterRole,
  account: '',
  password: '',
  confirmPassword: '',
  errorMessage: '',
  isSubmitting: false,
}

export const useRegisterPageStore = create<RegisterPageStore>()((set, get) => ({
  ...initialState,
  resetForm: () => set(initialState),
  setRole: (role) => set({ role }),
  setAccount: (account) => set({ account }),
  setPassword: (password) => set({ password }),
  setConfirmPassword: (confirmPassword) => set({ confirmPassword }),
  submit: async () => {
    const { role, account, password, confirmPassword } = get()

    if (!account.trim() || !password.trim() || !confirmPassword.trim()) {
      set({ errorMessage: '请完整填写账号和两次密码。' })
      return false
    }

    if (password !== confirmPassword) {
      set({ errorMessage: '两次输入的密码不一致。' })
      return false
    }

    set({ isSubmitting: true, errorMessage: '' })
    try {
      await runTask(registerIO({ role, username: account.trim(), password: password.trim() }))
      return true
    } catch (error) {
      set({ errorMessage: error instanceof Error ? error.message : '注册失败' })
      return false
    } finally {
      set({ isSubmitting: false })
    }
  },
}))
