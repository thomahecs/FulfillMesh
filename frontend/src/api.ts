import type { Fulfillment, InventoryInput, InventoryItem, Order, ServiceHealth, Shipment } from './types'

export class ApiError extends Error {
  status: number
  constructor(message: string, status: number) {
    super(message)
    this.status = status
  }
}

async function request<T>(path: string, token?: string, init?: RequestInit): Promise<T> {
  const headers = new Headers(init?.headers)
  if (init?.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json')
  if (token) headers.set('Authorization', `Bearer ${token}`)

  const response = await fetch(path, { ...init, headers })
  if (!response.ok) {
    let message = `${response.status} ${response.statusText}`
    try {
      const body = await response.json()
      message = body.error || body.message || message
    } catch { /* response is not JSON */ }
    throw new ApiError(message, response.status)
  }
  if (response.status === 204 || response.headers.get('content-length') === '0') return undefined as T
  return response.json() as Promise<T>
}

export const api = {
  login: (email: string, password: string) => request<{ token: string }>('/api/auth/login', undefined, {
    method: 'POST', body: JSON.stringify({ email, password })
  }),
  register: (email: string, password: string) => request<{ token: string }>('/api/auth/register', undefined, {
    method: 'POST', body: JSON.stringify({ email, password })
  }),
  orders: (token: string) => request<Order[]>('/api/orders', token),
  order: (token: string, id: string) => request<Order>(`/api/orders/${id}`, token),
  createOrder: (token: string, items: { sku: string; quantity: number }[], idempotencyKey: string) =>
    request<Order>('/api/orders', token, {
      method: 'POST',
      headers: { 'Idempotency-Key': idempotencyKey },
      body: JSON.stringify({ items })
    }),
  inventory: (token: string) => request<InventoryItem[]>('/api/inventory', token),
  upsertInventory: (token: string, sku: string, input: InventoryInput) =>
    request<InventoryItem>(`/api/inventory/${encodeURIComponent(sku)}`, token, {
      method: 'PUT', body: JSON.stringify(input)
    }),
  deleteInventory: (token: string, sku: string) => request<void>(`/api/inventory/${encodeURIComponent(sku)}`, token, { method: 'DELETE' }),
  fulfillments: (token: string) => request<Fulfillment[]>('/api/fulfillments', token),
  fulfillmentByOrder: (token: string, orderId: string) => request<Fulfillment>(`/api/fulfillments/orders/${orderId}`, token),
  shipments: (token: string) => request<Shipment[]>('/api/shipments', token),
  shipmentByOrder: (token: string, orderId: string) => request<Shipment>(`/api/shipments/orders/${orderId}`, token)
}

export async function optional<T>(work: Promise<T>): Promise<T | null> {
  try { return await work } catch { return null }
}

const services = [
  { key: 'order' as const, name: 'Order Service', port: 8081 },
  { key: 'inventory' as const, name: 'Inventory Service', port: 8082 },
  { key: 'fulfillment' as const, name: 'Fulfillment Service', port: 8083 },
  { key: 'shipping' as const, name: 'Shipping Service', port: 8084 }
]

export async function checkHealth(): Promise<ServiceHealth[]> {
  return Promise.all(services.map(async service => {
    const started = performance.now()
    try {
      const response = await fetch(`/health/${service.key}`, { signal: AbortSignal.timeout(1800) })
      const body = response.ok ? await response.json() : null
      return { ...service, online: response.ok && body?.status === 'UP', latencyMs: Math.round(performance.now() - started) }
    } catch {
      return { ...service, online: false, latencyMs: null }
    }
  }))
}
