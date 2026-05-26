import type { UserRole } from '@/objects/shared/ids'
import type { TaskIO } from '@/api/shared/TaskIO'
import { localStorageGetItemIO, localStorageRemoveItemIO, localStorageSetItemIO, nowIO } from '@/api/shared/browser'
import { flatMapTask, mapTask } from '@/api/shared/TaskIO'

export interface AuthSession {
  token: string
  account: string
  role: UserRole
  loggedInAt: number
}

export const AUTH_SESSION_KEY = 'delivery-platform-auth-session-v2'
export const AUTH_SESSION_UPDATED_EVENT = 'delivery-auth-session-updated'

type RoleHomeRoute = '/delivery/customer' | '/delivery/merchant' | '/delivery/rider'

const roleHomeRouteMap: Record<UserRole, RoleHomeRoute> = {
  customer: '/delivery/customer',
  merchant: '/delivery/merchant',
  rider: '/delivery/rider',
}

export function isUserRole(value: unknown): value is UserRole {
  return value === 'customer' || value === 'merchant' || value === 'rider'
}

function isAuthSession(value: unknown): value is AuthSession {
  if (!value || typeof value !== 'object') {
    return false
  }

  const record = value as Record<string, unknown>
  return (
    typeof record.token === 'string' &&
    record.token.length > 0 &&
    typeof record.account === 'string' &&
    record.account.length > 0 &&
    isUserRole(record.role) &&
    typeof record.loggedInAt === 'number'
  )
}

export function getDefaultRouteForRole(role: UserRole): RoleHomeRoute {
  return roleHomeRouteMap[role]
}

export function setAuthSessionIO(token: string, account: string, role: UserRole): TaskIO<void> {
  return () =>
    flatMapTask(nowIO(), (loggedInAt) =>
      localStorageSetItemIO(
        AUTH_SESSION_KEY,
        JSON.stringify({
          token,
          account,
          role,
          loggedInAt,
        } satisfies AuthSession),
      ),
    )().then(() => {
      dispatchAuthSessionUpdated()
    })
}

function parseAuthSession(raw: string | null): AuthSession | null {
  if (!raw) return null
  try {
    const parsed: unknown = JSON.parse(raw)
    return isAuthSession(parsed) ? parsed : null
  } catch {
    return null
  }
}

export function readAuthSession(): AuthSession | null {
  return parseAuthSession(window.localStorage.getItem(AUTH_SESSION_KEY))
}

function dispatchAuthSessionUpdated(): void {
  window.dispatchEvent(new Event(AUTH_SESSION_UPDATED_EVENT))
}

export function getAuthSessionIO(): TaskIO<AuthSession | null> {
  return mapTask(localStorageGetItemIO(AUTH_SESSION_KEY), parseAuthSession)
}

export function getAuthTokenIO(): TaskIO<string | null> {
  return mapTask(getAuthSessionIO(), (session) => session?.token ?? null)
}

export function clearAuthSessionIO(): TaskIO<void> {
  return () =>
    localStorageRemoveItemIO(AUTH_SESSION_KEY)().then(() => {
      dispatchAuthSessionUpdated()
    })
}
