import type { TaskIO } from './TaskIO'
import { fetchIO } from './browser'
import { clearAuthSessionIO, getAuthTokenIO } from '@/lib/auth-session'

export { runTask } from './TaskIO'
import { runTask } from './TaskIO'

const base = (import.meta.env.VITE_API_BASE as string | undefined) ?? '/api'

async function parseErrorMessage(res: Response): Promise<string> {
  try {
    const data: unknown = await res.clone().json()
    if (data && typeof data === 'object' && 'error' in data) {
      const err = (data as { error?: unknown }).error
      if (typeof err === 'string') return err
    }
  } catch {
    // ignore
  }
  try {
    const text = await res.text()
    return text || res.statusText || `HTTP ${res.status}`
  } catch {
    return res.statusText || `HTTP ${res.status}`
  }
}

export class ApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

async function apiFetchPromise<T>(path: string, init?: RequestInit): Promise<T> {
  const url = `${base.replace(/\/$/, '')}${path.startsWith('/') ? path : `/${path}`}`
  const headers = new Headers(init?.headers)
  if (!(init?.body instanceof FormData) && !headers.has('Content-Type') && init?.body) {
    headers.set('Content-Type', 'application/json')
  }
  const token = await runTask(getAuthTokenIO())
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  let res: Response
  try {
    res = await runTask(fetchIO(url, { ...init, headers }))
  } catch (error) {
    const message = error instanceof Error ? error.message : '请求失败'
    throw new Error(`无法连接后端服务：${message}`)
  }
  if ((res.status === 401 || res.status === 403) && token) {
    await runTask(clearAuthSessionIO())
  }
  if (!res.ok) {
    throw new ApiError(res.status, await parseErrorMessage(res))
  }
  if (res.status === 204) {
    return undefined as T
  }
  return (await res.json()) as T
}

export function apiFetchIO<T>(path: string, init?: RequestInit): TaskIO<T> {
  return () => apiFetchPromise(path, init)
}

export function apiGetIO<T>(path: string): TaskIO<T> {
  return () => apiFetchPromise(path, { method: 'GET' })
}

export function apiPostIO<T>(path: string, body?: unknown): TaskIO<T> {
  return () =>
    apiFetchPromise(path, {
      method: 'POST',
      body: body === undefined ? undefined : JSON.stringify(body),
    })
}

/** multipart/form-data（不要手动设置 Content-Type，以便包含 boundary） */
export function apiPostFormDataIO<T>(path: string, formData: FormData): TaskIO<T> {
  return () =>
    apiFetchPromise(path, {
      method: 'POST',
      body: formData,
    })
}

export function apiPatchIO<T>(path: string, body: unknown): TaskIO<T> {
  return () =>
    apiFetchPromise(path, {
      method: 'PATCH',
      body: JSON.stringify(body),
    })
}

export function apiPutIO<T>(path: string, body: unknown): TaskIO<T> {
  return () =>
    apiFetchPromise(path, {
      method: 'PUT',
      body: JSON.stringify(body),
    })
}
