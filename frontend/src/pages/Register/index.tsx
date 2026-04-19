import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'

import { useAppChrome } from '@/hooks/useAppChrome'
import { useRegisterPageStore } from '@/stores/pages/use-register-page-store'

import { RegisterForm } from './RegisterForm'
import { AuthPageShell } from '../shared/AuthPageShell'

export default function Register() {
  const navigate = useNavigate()
  const { showNotice } = useAppChrome()
  const role = useRegisterPageStore((state) => state.role)
  const account = useRegisterPageStore((state) => state.account)
  const password = useRegisterPageStore((state) => state.password)
  const confirmPassword = useRegisterPageStore((state) => state.confirmPassword)
  const errorMessage = useRegisterPageStore((state) => state.errorMessage)
  const isSubmitting = useRegisterPageStore((state) => state.isSubmitting)
  const resetForm = useRegisterPageStore((state) => state.resetForm)
  const setRole = useRegisterPageStore((state) => state.setRole)
  const setAccount = useRegisterPageStore((state) => state.setAccount)
  const setPassword = useRegisterPageStore((state) => state.setPassword)
  const setConfirmPassword = useRegisterPageStore((state) => state.setConfirmPassword)
  const submit = useRegisterPageStore((state) => state.submit)

  useEffect(() => {
    resetForm()
  }, [resetForm])

  const handleSubmit = async () => {
    const ok = await submit()
    if (ok) {
      showNotice('注册成功，请登录。', 'success')
      navigate('/auth/login')
    }
  }

  return (
    <AuthPageShell title="注册账号" description="请输入账号与两次密码完成注册">
      <RegisterForm
        role={role}
        account={account}
        password={password}
        confirmPassword={confirmPassword}
        errorMessage={errorMessage}
        isSubmitting={isSubmitting}
        onRoleChange={setRole}
        onAccountChange={setAccount}
        onPasswordChange={setPassword}
        onConfirmPasswordChange={setConfirmPassword}
        onSubmit={handleSubmit}
      />
    </AuthPageShell>
  )
}
