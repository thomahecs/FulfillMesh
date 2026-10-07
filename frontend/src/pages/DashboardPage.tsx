import { useEffect, useMemo, useState } from 'react'
import { api } from '../api'
import { Button, MetricCard, SectionHead, StatusPill, EmptyState, Icon } from '../components/UI'
import type { Fulfillment, InventoryItem, Order, ServiceHealth, Shipment } from '../types'
import { formatDate, shortId } from '../utils'
import type { PageKey } from '../components/Shell'

export default function DashboardPage({ token, admin, health, globalSearch, navigate, refreshSignal }: {
  token: string
  admin: boolean
  health: ServiceHealth[]
  globalSearch: string
  navigate: (page: PageKey) => void
  refreshSignal: number
}) {
  const [orders, setOrders] = useState<Order[]>([])
  const [inventory, setInventory] = useState<InventoryItem[]>([])
  const [fulfillments, setFulfillments] = useState<Fulfillment[]>([])
  const [shipments, setShipments] = useState<Shipment[]>([])
  const [loading, setLoading] = useState(true)

  async function load() {
    setLoading(true)
    const [orderData, inventoryData] = await Promise.all([
      api.orders(token).catch(() => []),
      api.inventory(token).catch(() => [])
    ])
    setOrders(orderData); setInventory(inventoryData)
    if (admin) {
      const [fulfillmentData, shipmentData] = await Promise.all([
        api.fulfillments(token).catch(() => []),
        api.shipments(token).catch(() => [])
      ])
      setFulfillments(fulfillmentData); setShipments(shipmentData)
    }
    setLoading(false)
  }

  useEffect(() => { void load() }, [token, admin, refreshSignal])

  const filteredOrders = useMemo(() => {
    const q = globalSearch.trim().toLowerCase()
    if (!q) return orders
    return orders.filter(o => o.id.toLowerCase().includes(q) || o.customerEmail.toLowerCase().includes(q) || o.items.some(i => i.sku.toLowerCase().includes(q)))
  }, [orders, globalSearch])

  const stockAvailable = inventory.reduce((sum, item) => sum + item.availableQuantity, 0)
  const reserved = inventory.reduce((sum, item) => sum + item.reservedQuantity, 0)
  const active = orders.filter(o => !['SHIPPED', 'INVENTORY_REJECTED'].includes(o.state)).length
  const lowStock = inventory.filter(i => i.availableQuantity <= 5).sort((a,b) => a.availableQuantity - b.availableQuantity)
  const shippedCount = admin ? shipments.length : orders.filter(o => o.state === 'SHIPPED').length
  const avgGain = fulfillments.filter(f => f.improvementPercent != null).length
    ? fulfillments.filter(f => f.improvementPercent != null).reduce((s, f) => s + (f.improvementPercent ?? 0), 0) / fulfillments.filter(f => f.improvementPercent != null).length
    : 0

  return <div className="page-stack">
    <section className="hero-banner">
      <div><span className="hero-kicker"><Icon name="spark" size={16}/> Live fulfillment operations</span><h1>Command Center</h1><p>Watch the full event-driven path from order creation to inventory reservation, optimized picking, and shipping.</p></div>
      <div className="hero-actions"><Button variant="secondary" icon="warehouse" onClick={() => navigate('inventory')}>Manage inventory</Button><Button icon="plus" onClick={() => navigate('orders')}>Create order</Button></div>
      <div className="hero-mesh-strip">
        {['Order created', 'Stock reserved', 'A* pick route', 'Shipment ready'].map((label, index) => <div key={label}><span>{index + 1}</span><strong>{label}</strong>{index < 3 && <b>→</b>}</div>)}
      </div>
    </section>

    <div className="metrics-grid">
      <MetricCard label="Orders" value={orders.length} sub={`${active} currently active`} icon="cart" tone="blue"/>
      <MetricCard label="Available units" value={stockAvailable} sub={`${reserved} reserved`} icon="warehouse" tone="teal"/>
      <MetricCard label="Shipments" value={shippedCount} sub="event-driven completions" icon="truck" tone="purple"/>
      <MetricCard label="Route reduction" value={admin && fulfillments.length ? `${avgGain.toFixed(1)}%` : '22%'} sub="A* benchmark target" icon="route" tone="amber"/>
    </div>

    <div className="dashboard-grid">
      <section className="panel panel-wide">
        <SectionHead eyebrow="Live workload" title="Recent orders" action={<Button variant="ghost" onClick={() => navigate('orders')}>View all <Icon name="chevron" size={15}/></Button>}/>
        {loading ? <div className="skeleton-list"><div className="skeleton-row"/><div className="skeleton-row"/><div className="skeleton-row"/></div> : filteredOrders.length ? <div className="table-wrap"><table className="data-table"><thead><tr><th>Order</th><th>Customer</th><th>Items</th><th>State</th><th>Created</th></tr></thead><tbody>{filteredOrders.slice(0,7).map(order => <tr key={order.id} onClick={() => navigate('orders')}><td><strong className="mono">#{shortId(order.id)}</strong></td><td>{order.customerEmail}</td><td>{order.items.reduce((s,i)=>s+i.quantity,0)} units</td><td><StatusPill state={order.state}/></td><td>{formatDate(order.createdAt)}</td></tr>)}</tbody></table></div> : <EmptyState title="No orders yet" text="Create your first order to start the Kafka fulfillment workflow." action={<Button icon="plus" onClick={() => navigate('orders')}>Create order</Button>}/>}
      </section>

      <section className="panel">
        <SectionHead eyebrow="Infrastructure" title="Service mesh" action={<button className="tiny-link" onClick={() => navigate('system')}>Details</button>}/>
        <div className="service-list">{health.map(service => <div className="service-row" key={service.key}><span className={`service-state ${service.online ? 'up' : 'down'}`}/><div><strong>{service.name}</strong><small>localhost:{service.port}</small></div><span className="latency">{service.online ? `${service.latencyMs ?? 0} ms` : 'offline'}</span></div>)}</div>
      </section>

      <section className="panel">
        <SectionHead eyebrow="Risk watch" title="Low stock" action={<button className="tiny-link" onClick={() => navigate('inventory')}>Inventory</button>}/>
        {lowStock.length ? <div className="stock-watch">{lowStock.slice(0,6).map(item => <div key={item.sku}><div><strong>{item.sku}</strong><small>{item.name}</small></div><span className={item.availableQuantity === 0 ? 'stock-zero' : ''}>{item.availableQuantity}</span></div>)}</div> : <div className="mini-empty"><Icon name="shield" size={24}/><span>Stock levels look healthy</span></div>}
      </section>

      <section className="panel panel-wide architecture-panel">
        <SectionHead eyebrow="Event topology" title="Fulfillment pipeline"/>
        <div className="pipeline">
          <div className="pipeline-service"><span className="pipeline-icon"><Icon name="cart"/></span><strong>Order</strong><small>8081</small></div>
          <div className="topic"><b>orders.created</b><span>Kafka</span></div>
          <div className="pipeline-service"><span className="pipeline-icon"><Icon name="warehouse"/></span><strong>Inventory</strong><small>8082</small></div>
          <div className="topic"><b>inventory.reserved</b><span>Kafka</span></div>
          <div className="pipeline-service"><span className="pipeline-icon"><Icon name="route"/></span><strong>Fulfillment</strong><small>8083</small></div>
          <div className="topic"><b>picking.completed</b><span>Kafka</span></div>
          <div className="pipeline-service"><span className="pipeline-icon"><Icon name="truck"/></span><strong>Shipping</strong><small>8084</small></div>
        </div>
      </section>
    </div>
  </div>
}
