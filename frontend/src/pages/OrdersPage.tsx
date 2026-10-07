import { useEffect, useMemo, useState } from 'react'
import { api, optional } from '../api'
import { Button, EmptyState, Icon, Modal, SectionHead, StatusPill } from '../components/UI'
import WarehouseRoute from '../components/WarehouseRoute'
import type { Fulfillment, InventoryItem, Order, Shipment } from '../types'
import { copyText, formatDate, orderProgress, shortId, stateLabel } from '../utils'

export default function OrdersPage({ token, admin, globalSearch, notify, onChanged }: {
  token: string
  admin: boolean
  globalSearch: string
  notify: (type: 'success'|'error'|'info', text: string) => void
  onChanged: () => void
}) {
  const [orders, setOrders] = useState<Order[]>([])
  const [inventory, setInventory] = useState<InventoryItem[]>([])
  const [stateFilter, setStateFilter] = useState('ALL')
  const [loading, setLoading] = useState(true)
  const [createOpen, setCreateOpen] = useState(false)
  const [selected, setSelected] = useState<Order | null>(null)
  const [fulfillment, setFulfillment] = useState<Fulfillment | null>(null)
  const [shipment, setShipment] = useState<Shipment | null>(null)

  async function load() {
    setLoading(true)

    try {
      const orderData = await api.orders(token)
      setOrders(orderData)
    } catch (err) {
      notify('error', err instanceof Error ? `Orders: ${err.message}` : 'Orders failed to load')
    }

    try {
      const inventoryData = await api.inventory(token)
      setInventory(inventoryData)
    } catch (err) {
      notify('error', err instanceof Error ? `Inventory: ${err.message}` : 'Inventory failed to load')
    }

    setLoading(false)
  }

  useEffect(() => {
    if (!selected) return
    let active = true
    const poll = async () => {
      const fresh = await optional(api.order(token, selected.id))
      const [f, s] = await Promise.all([optional(api.fulfillmentByOrder(token, selected.id)), optional(api.shipmentByOrder(token, selected.id))])
      if (!active) return
      if (fresh) {
        setSelected(fresh)
        setOrders(prev => prev.map(o => o.id === fresh.id ? fresh : o))
      }
      setFulfillment(f); setShipment(s)
    }
    void poll()
    const timer = window.setInterval(() => { if (selected.state !== 'SHIPPED' && selected.state !== 'INVENTORY_REJECTED') void poll() }, 2500)
    return () => { active = false; clearInterval(timer) }
  }, [selected?.id])

  const filtered = useMemo(() => {
    const q = globalSearch.toLowerCase().trim()
    return orders.filter(o => (stateFilter === 'ALL' || o.state === stateFilter) && (!q || o.id.toLowerCase().includes(q) || o.customerEmail.toLowerCase().includes(q) || o.items.some(i => i.sku.toLowerCase().includes(q))))
  }, [orders, stateFilter, globalSearch])

  async function created(order: Order) {
    setCreateOpen(false)
    setOrders(prev => [order, ...prev.filter(o => o.id !== order.id)])
    setSelected(order)
    notify('success', `Order #${shortId(order.id)} created and published to Kafka`)
    onChanged()
  }

  return <div className="page-stack">
    <div className="page-title-row"><div><span className="eyebrow">Order Service · :8081</span><h1>Orders</h1><p>Create idempotent orders and follow each one through the asynchronous fulfillment pipeline.</p></div><Button icon="plus" onClick={() => setCreateOpen(true)}>Create order</Button></div>

    <section className="panel">
      <div className="toolbar"><div className="filter-tabs">{['ALL','CREATED','INVENTORY_RESERVED','PICKING','SHIPPED','INVENTORY_REJECTED'].map(state => <button key={state} className={stateFilter === state ? 'active' : ''} onClick={() => setStateFilter(state)}>{state === 'ALL' ? 'All' : stateLabel(state)}</button>)}</div><button className="icon-button" onClick={() => void load()} title="Refresh"><Icon name="refresh"/></button></div>
      {loading ? <div className="skeleton-list"><div className="skeleton-row"/><div className="skeleton-row"/><div className="skeleton-row"/></div> : filtered.length ? <div className="table-wrap"><table className="data-table orders-table"><thead><tr><th>Order ID</th>{admin && <th>Customer</th>}<th>Basket</th><th>State</th><th>Progress</th><th>Created</th><th/></tr></thead><tbody>{filtered.map(order => <tr key={order.id} onClick={() => setSelected(order)}><td><strong className="mono">#{shortId(order.id,10)}</strong><small className="version">v{order.version}</small></td>{admin && <td>{order.customerEmail}</td>}<td><div className="sku-stack">{order.items.slice(0,3).map(i => <span key={i.sku}>{i.sku} × {i.quantity}</span>)}{order.items.length > 3 && <b>+{order.items.length-3}</b>}</div></td><td><StatusPill state={order.state}/></td><td><div className="progress-cell"><div className="progress-track"><i style={{width:`${orderProgress[order.state]}%`}} className={order.state === 'INVENTORY_REJECTED' ? 'danger' : ''}/></div><span>{orderProgress[order.state]}%</span></div></td><td>{formatDate(order.createdAt)}</td><td><button className="row-arrow"><Icon name="chevron" size={16}/></button></td></tr>)}</tbody></table></div> : <EmptyState icon="cart" title="No matching orders" text="Try another filter or create a new order." action={<Button icon="plus" onClick={() => setCreateOpen(true)}>Create order</Button>}/>}
    </section>

    {createOpen && <CreateOrderModal token={token} inventory={inventory} onClose={() => setCreateOpen(false)} onCreated={created} notify={notify}/>}
    {selected && <OrderDetailModal order={selected} fulfillment={fulfillment} shipment={shipment} onClose={() => { setSelected(null); setFulfillment(null); setShipment(null) }} notify={notify}/>}
  </div>
}

function CreateOrderModal({ token, inventory, onClose, onCreated, notify }: { token: string; inventory: InventoryItem[]; onClose: () => void; onCreated: (order: Order) => void; notify: (type:'success'|'error'|'info', text:string)=>void }) {
  const [lines, setLines] = useState([{ sku: inventory[0]?.sku ?? 'SKU-A', quantity: 1 }])
  const [key, setKey] = useState<string>(crypto.randomUUID());
  const [busy, setBusy] = useState(false)
  const addLine = () => setLines(prev => [...prev, { sku: inventory.find(i => !prev.some(p => p.sku === i.sku))?.sku ?? '', quantity: 1 }])
  const total = lines.reduce((s,l) => s + Number(l.quantity || 0), 0)
  function benchmark() {
    const picks = inventory.filter(i => /^SKU-[A-H]$/.test(i.sku)).slice(0, 8)

    if (picks.length < 8) {
      notify('error', 'A* benchmark basket requires SKU-A through SKU-H to be available')
      return
    }

    setLines(picks.map(i => ({ sku: i.sku, quantity: 1 })))
  }
  async function submit(e: React.FormEvent) {
    e.preventDefault()
    const items = lines.filter(l => l.sku.trim()).map(l => ({ sku: l.sku.trim().toUpperCase(), quantity: Number(l.quantity) }))
    if (!items.length) return notify('error', 'Add at least one SKU')
    if (new Set(items.map(i => i.sku)).size !== items.length) return notify('error', 'Each SKU can appear only once in an order')
    setBusy(true)
    try { onCreated(await api.createOrder(token, items, key)) }
    catch (err) { notify('error', err instanceof Error ? err.message : 'Order creation failed') }
    finally { setBusy(false) }
  }
  return <Modal title="Create fulfillment order" subtitle="The request is protected by a Redis idempotency key and immediately publishes orders.created." onClose={onClose} wide>
    <form onSubmit={submit} className="order-builder">
      <div className="builder-head"><div><strong>Order basket</strong><span>{lines.length} SKUs · {total} units</span></div><div><Button type="button" variant="ghost" icon="spark" onClick={benchmark}>Load A* benchmark basket</Button><Button type="button" variant="secondary" icon="plus" onClick={addLine}>Add line</Button></div></div>
      <div className="line-editor"><div className="line-editor-head"><span>SKU</span><span>Available</span><span>Quantity</span><span/></div>{lines.map((line,index) => {
        const stock = inventory.find(i => i.sku === line.sku)
        return <div className="line-row" key={index}><select value={line.sku} onChange={e => setLines(prev => prev.map((l,i)=> i===index ? {...l,sku:e.target.value}:l))}><option value="">Choose SKU</option>{inventory.map(item => <option key={item.sku} value={item.sku}>{item.sku} — {item.name}</option>)}</select><span className={`availability ${stock?.availableQuantity === 0 ? 'empty' : ''}`}>{stock ? stock.availableQuantity : '—'}</span><input type="number" min="1" max={stock?.availableQuantity || 999} value={line.quantity} onChange={e => setLines(prev => prev.map((l,i)=>i===index?{...l,quantity:Number(e.target.value)}:l))}/><button type="button" className="icon-button danger-icon" disabled={lines.length===1} onClick={() => setLines(prev => prev.filter((_,i)=>i!==index))}><Icon name="trash" size={17}/></button></div>
      })}</div>
      <div className="advanced-field"><label>Idempotency key <span>safe retry identifier</span><input value={key} onChange={e=>setKey(e.target.value)}/></label><button type="button" className="tiny-link" onClick={() => setKey(crypto.randomUUID())}>Generate new key</button></div>
      <div className="modal-actions"><Button type="button" variant="ghost" onClick={onClose}>Cancel</Button><Button type="submit" disabled={busy || lines.some(l=>!l.sku)}>{busy ? 'Publishing…' : `Place order · ${total} units`}</Button></div>
    </form>
  </Modal>
}

function OrderDetailModal({ order, fulfillment, shipment, onClose, notify }: { order: Order; fulfillment: Fulfillment|null; shipment: Shipment|null; onClose:()=>void; notify:(type:'success'|'error'|'info',text:string)=>void }) {
  const steps = [
    {label:'Order created', done:true, detail:'Persisted in PostgreSQL'},
    {label:'Inventory reserved', done:['INVENTORY_RESERVED','PICKING','SHIPPED'].includes(order.state), detail:'PESSIMISTIC_WRITE row locks'},
    {label:'Picking optimized', done:order.state==='SHIPPED' || fulfillment?.state==='COMPLETED', detail:'A* warehouse route'},
    {label:'Shipment created', done:order.state==='SHIPPED', detail:'Kafka shipping.created'}
  ]
  return <Modal title={`Order #${shortId(order.id,12)}`} subtitle={`${order.customerEmail} · ${formatDate(order.createdAt)}`} onClose={onClose} wide>
    <div className="detail-grid">
      <div className="detail-main">
        <div className="detail-topline"><StatusPill state={order.state}/><button className="copy-id" onClick={() => { void copyText(order.id); notify('info','Order ID copied') }}><Icon name="copy" size={15}/>Copy full ID</button></div>
        <div className="timeline">{steps.map((step,i)=><div key={step.label} className={`timeline-step ${step.done?'done':''}`}><span>{step.done?'✓':i+1}</span><div><strong>{step.label}</strong><small>{step.detail}</small></div></div>)}</div>
        <SectionHead eyebrow="Basket" title={`${order.items.length} SKUs`}/>
        <div className="basket-list">{order.items.map(item => <div key={item.sku}><span className="sku-box">{item.sku}</span><strong>× {item.quantity}</strong></div>)}</div>
        {fulfillment && <><SectionHead eyebrow="A* picking" title="Optimized route"/><WarehouseRoute route={fulfillment.pickRoute} height={320}/></>}
      </div>
      <aside className="detail-side">
        <div className="side-card"><span>Order version</span><strong>v{order.version}</strong><small>Hibernate optimistic lock</small></div>
        <div className="side-card"><span>Optimized distance</span><strong>{fulfillment?.optimizedDistance ?? '—'}</strong><small>warehouse grid cells</small></div>
        <div className="side-card"><span>Sequential baseline</span><strong>{fulfillment?.sequentialDistance ?? '—'}</strong><small>request-order route</small></div>
        <div className="side-card accent"><span>Travel reduction</span><strong>{fulfillment?.improvementPercent != null ? `${fulfillment.improvementPercent.toFixed(1)}%` : '—'}</strong><small>A* route optimization</small></div>
        <div className="tracking-card"><span>Shipment</span>{shipment ? <><strong>{shipment.trackingNumber}</strong><StatusPill state={shipment.status}/><small>Shipment #{shortId(shipment.id)}</small></> : <><strong>Waiting</strong><small>Created after picking.completed</small></>}</div>
      </aside>
    </div>
  </Modal>
}
