import { KeyRound, LogIn, UserRound } from 'lucide-react'
import { Link } from 'react-router-dom'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { isUserRole } from '@/lib/auth-session'
import type { UserRole } from '@/objects/shared'

import { roleOptions } from './constants'

interface LoginFormProps {
  role: UserRole
  roleLabel: string
  account: string
  password: string
  errorMessage: string | null
  isSubmitting: boolean
  onRoleChange: (role: UserRole) => void
  onAccountChange: (value: string) => void
  onPasswordChange: (value: string) => void
  onSubmit: () => void
}

export function LoginForm({
  role,
  roleLabel,
  account,
  password,
  errorMessage,
  isSubmitting,
  onRoleChange,
  onAccountChange,
  onPasswordChange,
  onSubmit,
}: LoginFormProps) {
  return (
    <form
      className="space-y-5"
      onSubmit={(event) => {
        event.preventDefault()
        onSubmit()
      }}
    >
      <div className="space-y-2">
        <Label htmlFor="role">角色</Label>
        <Select
          value={role}
          onValueChange={(value) => {
            if (isUserRole(value)) {
              onRoleChange(value)
            }
          }}
        >
          <SelectTrigger id="role" className="h-11 rounded-xl">
            <SelectValue placeholder="请选择角色" />
          </SelectTrigger>
          <SelectContent>
            {roleOptions.map((item) => (
              <SelectItem key={item.value} value={item.value}>
                {item.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <p className="text-xs text-slate-500">当前选择：{roleLabel}</p>
      </div>

      <div className="space-y-2">
        <Label htmlFor="account">账号</Label>
        <div className="relative">
          <UserRound className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-slate-400" />
          <Input
            id="account"
            value={account}
            placeholder="请输入账号"
            autoComplete="username"
            className="h-11 rounded-xl border-slate-200 pl-10"
            onChange={(event) => onAccountChange(event.target.value)}
          />
        </div>
      </div>

      <div className="space-y-2">
        <Label htmlFor="password">密码</Label>
        <div className="relative">
          <KeyRound className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-slate-400" />
          <Input
            id="password"
            type="password"
            value={password}
            placeholder="请输入密码"
            autoComplete="current-password"
            className="h-11 rounded-xl border-slate-200 pl-10"
            onChange={(event) => onPasswordChange(event.target.value)}
          />
        </div>
      </div>

      {errorMessage ? (
        <Alert variant="destructive" className="rounded-xl border-rose-200 bg-rose-50/90">
          <AlertDescription>{errorMessage}</AlertDescription>
        </Alert>
      ) : null}

      <Button type="submit" className="h-11 w-full rounded-xl" disabled={isSubmitting}>
        <LogIn className="size-4" />
        {isSubmitting ? '登录中…' : '登录'}
      </Button>

      <Button asChild type="button" variant="link" className="h-auto w-full px-0 text-slate-600">
        <Link to="/auth/register">没有账号？去注册</Link>
      </Button>
    </form>
  )
}
