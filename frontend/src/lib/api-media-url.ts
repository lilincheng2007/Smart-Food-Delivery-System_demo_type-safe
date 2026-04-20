/**
 * 将 API 返回的相对路径（如 `/api/merchant/store-images/...`）解析为浏览器可请求的绝对地址。
 *
 * 1. `VITE_API_BASE` 为 `http(s)://...` 时，相对路径需拼到该基址上（否则 `<img src="/api/...">` 仍指向前端源）。
 * 2. 开发环境常见 `VITE_API_BASE=/api`：fetch 走 Vite 代理，但 `<img>` 的相对 URL 在部分环境下代理不稳定；
 *    若配置了 `VITE_BACKEND_URL`（如 `http://localhost:8787`），则对 `/api/...` 直连后端，避免图片裂图。
 */
export function resolveApiMediaUrl(url: string | null | undefined): string {
  if (url == null) return ''
  const trimmed = String(url).trim()
  if (!trimmed) return ''
  if (trimmed.startsWith('http://') || trimmed.startsWith('https://')) return trimmed

  const backendOrigin = (import.meta.env.VITE_BACKEND_URL as string | undefined)?.trim()
  if (
    trimmed.startsWith('/api') &&
    backendOrigin &&
    (backendOrigin.startsWith('http://') || backendOrigin.startsWith('https://'))
  ) {
    return `${backendOrigin.replace(/\/$/, '')}${trimmed}`
  }

  const rawBase = (import.meta.env.VITE_API_BASE as string | undefined)?.trim()
  const base = rawBase && rawBase.length > 0 ? rawBase : '/api'
  const normalized = base.replace(/\/$/, '')

  if (
    trimmed.startsWith('/') &&
    (normalized.startsWith('http://') || normalized.startsWith('https://'))
  ) {
    const baseForResolve = normalized.endsWith('/') ? normalized : `${normalized}/`
    return new URL(trimmed, baseForResolve).href
  }

  return trimmed
}
