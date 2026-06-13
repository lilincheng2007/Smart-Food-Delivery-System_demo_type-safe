import { useEffect, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'

import { useLoginPageStore } from '@/pages/Login/stores/use-login-page-store'

import { AuthPageShell } from '@/components/auth/AuthPageShell'

import { LoginForm } from './components/LoginForm'
import { getRoleLabel } from './objects/constants'

export default function Login() {
  const navigate = useNavigate()
  const role = useLoginPageStore((state) => state.role)
  const account = useLoginPageStore((state) => state.account)
  const password = useLoginPageStore((state) => state.password)
  const errorMessage = useLoginPageStore((state) => state.errorMessage)
  const isSubmitting = useLoginPageStore((state) => state.isSubmitting)
  const resetForm = useLoginPageStore((state) => state.resetForm)
  const setRole = useLoginPageStore((state) => state.setRole)
  const setAccount = useLoginPageStore((state) => state.setAccount)
  const setPassword = useLoginPageStore((state) => state.setPassword)
  const submit = useLoginPageStore((state) => state.submit)

  useEffect(() => {
    resetForm()
  }, [resetForm])

  const roleLabel = useMemo(() => getRoleLabel(role), [role])

  const handleSubmit = async () => {
    const redirectTo = await submit()
    if (redirectTo) {
      navigate(redirectTo)
    }
  }

  return (
    <AuthPageShell title="外卖平台登录" description="选择角色后输入账号密码即可进入对应端">
      <LoginForm
        role={role}
        roleLabel={roleLabel}
        account={account}
        password={password}
        errorMessage={errorMessage}
        isSubmitting={isSubmitting}
        onRoleChange={setRole}
        onAccountChange={setAccount}
        onPasswordChange={setPassword}
        onSubmit={handleSubmit}
      />
    </AuthPageShell>
  )
}
