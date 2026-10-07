import type { AuthClaims, OrderState } from './types'

export function parseToken(token: string): AuthClaims | null {
  try {
    const payload = token.split('.')[1]
    const normalized = payload.replace(/-/g, '+').replace(/_/g, '/')
    const json = decodeURIComponent(
      atob(normalized)
        .split('')
        .map((c) => `%${c.charCodeAt(0).toString(16).padStart(2, '0')}`)
        .join('')
    )
    return JSON.parse(json) as AuthClaims
  } catch {
    return null
  }
}

export const shortId = (id?: string | null, size = 8) => id ? id.slice(0, size) : '—'

export function formatDate(value?: string | null) {
  if (!value) return '—'
  return new Intl.DateTimeFormat('en-US', {
    month: 'short', day: 'numeric', hour: 'numeric', minute: '2-digit'
  }).format(new Date(value))
}

export function relativeTime(value?: string | null) {
  if (!value) return '—'
  const delta = Date.now() - new Date(value).getTime()
  const mins = Math.max(0, Math.floor(delta / 60000))
  if (mins < 1) return 'just now'
  if (mins < 60) return `${mins}m ago`
  const hours = Math.floor(mins / 60)
  if (hours < 24) return `${hours}h ago`
  return `${Math.floor(hours / 24)}d ago`
}

export function stateTone(state: string) {
  if (['SHIPPED', 'COMPLETED', 'LABEL_READY', 'IN_TRANSIT'].includes(state)) return 'success'
  if (['INVENTORY_REJECTED'].includes(state)) return 'danger'
  if (['CREATED'].includes(state)) return 'neutral'
  return 'info'
}

export const stateLabel = (state: string) => state.replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, c => c.toUpperCase())

export const orderProgress: Record<OrderState, number> = {
  CREATED: 18,
  INVENTORY_RESERVED: 42,
  INVENTORY_REJECTED: 100,
  PICKING: 70,
  SHIPPED: 100
}

export function copyText(text: string) {
  return navigator.clipboard.writeText(text)
}
