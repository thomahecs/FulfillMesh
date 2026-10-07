export type Role = 'ROLE_USER' | 'ROLE_ADMIN'

export type AuthClaims = {
  sub: string
  roles: Role[]
  exp?: number
}

export type OrderState = 'CREATED' | 'INVENTORY_RESERVED' | 'INVENTORY_REJECTED' | 'PICKING' | 'SHIPPED'
export type FulfillmentState = 'CREATED' | 'PICKING' | 'COMPLETED'
export type ShipmentStatus = 'CREATED' | 'LABEL_READY' | 'IN_TRANSIT'

export type OrderLine = { sku: string; quantity: number }
export type Order = {
  id: string
  customerEmail: string
  state: OrderState
  version: number
  createdAt: string
  items: OrderLine[]
}

export type InventoryItem = {
  sku: string
  name: string
  availableQuantity: number
  reservedQuantity: number
  warehouseX: number
  warehouseY: number
  version: number
}

export type InventoryInput = {
  name: string
  availableQuantity: number
  warehouseX: number
  warehouseY: number
}

export type Fulfillment = {
  id: string
  orderId: string
  state: FulfillmentState
  optimizedDistance: number | null
  sequentialDistance: number | null
  improvementPercent: number | null
  pickRoute: string | null
  version: number
  createdAt: string
  updatedAt: string
}

export type Shipment = {
  id: string
  orderId: string
  trackingNumber: string
  status: ShipmentStatus
  version: number
  createdAt: string
}

export type ServiceHealth = {
  key: 'order' | 'inventory' | 'fulfillment' | 'shipping'
  name: string
  port: number
  online: boolean
  latencyMs: number | null
}

export type ToastMessage = { id: number; type: 'success' | 'error' | 'info'; text: string }
