import { KeyRound, UserRound } from 'lucide-react'
import { Link } from 'react-router-dom'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import type { RegisterRole } from '@/pages/Register/stores/use-register-page-store'

import { isRegisterRole, registerRoleOptions } from '../objects/constants'

interface RegisterFormProps {
  role: RegisterRole
  account: string
  password: string
  confirmPassword: string
  errorMessage: string | null
  isSubmitting: boolean
  onRoleChange: (role: RegisterRole) => void
  onAccountChange: (value: string) => void
  onPasswordChange: (value: string) => void
  onConfirmPasswordChange: (value: string) => void
  onSubmit: () => void
}

export function RegisterForm({
  role,
  account,
  password,
  confirmPassword,
  errorMessage,
  isSubmitting,
  onRoleChange,
  onAccountChange,
  onPasswordChange,
  onConfirmPasswordChange,
  onSubmit,
}: RegisterFormProps) {
  return (
    <form
      className="space-y-5"
      onSubmit={(event) => {
        event.preventDefault()
        onSubmit()
      }}
    >
      <div className="space-y-2">
        <Label htmlFor="register-role">角色</Label>
        <Select
          value={role}
          onValueChange={(value) => {
            if (isRegisterRole(value)) {
              onRoleChange(value)
            }
          }}
        >
          <SelectTrigger id="register-role" className="h-11 rounded-xl">
            <SelectValue placeholder="请选择注册角色" />
          </SelectTrigger>
          <SelectContent>
            {registerRoleOptions.map((item) => (
              <SelectItem key={item.value} value={item.value}>
                {item.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <div className="space-y-2">
        <Label htmlFor="register-account">账号</Label>
        <div className="relative">
          <UserRound className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-slate-400" />
          <Input
            id="register-account"
            value={account}
            placeholder="请输入账号"
            autoComplete="username"
            className="h-11 rounded-xl border-slate-200 pl-10"
            onChange={(event) => onAccountChange(event.target.value)}
          />
        </div>
      </div>

      <div className="space-y-2">
        <Label htmlFor="register-password">密码</Label>
        <div className="relative">
          <KeyRound className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-slate-400" />
          <Input
            id="register-password"
            type="password"
            value={password}
            placeholder="请输入密码"
            autoComplete="new-password"
            className="h-11 rounded-xl border-slate-200 pl-10"
            onChange={(event) => onPasswordChange(event.target.value)}
          />
        </div>
      </div>

      <div className="space-y-2">
        <Label htmlFor="register-confirm-password">确认密码</Label>
        <div className="relative">
          <KeyRound className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-slate-400" />
          <Input
            id="register-confirm-password"
            type="password"
            value={confirmPassword}
            placeholder="请再次输入密码"
            autoComplete="new-password"
            className="h-11 rounded-xl border-slate-200 pl-10"
            onChange={(event) => onConfirmPasswordChange(event.target.value)}
          />
        </div>
      </div>

      {errorMessage ? (
        <Alert variant="destructive" className="rounded-xl border-rose-200 bg-rose-50/90">
          <AlertDescription>{errorMessage}</AlertDescription>
        </Alert>
      ) : null}

      <Button type="submit" className="h-11 w-full rounded-xl" disabled={isSubmitting}>
        {isSubmitting ? '提交中…' : '注册'}
      </Button>

      <Button asChild type="button" variant="link" className="h-auto w-full px-0 text-slate-600">
        <Link to="/auth/login">已有账号？返回登录</Link>
      </Button>
    </form>
  )
}
