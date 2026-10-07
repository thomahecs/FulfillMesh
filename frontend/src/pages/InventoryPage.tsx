import { useEffect, useMemo, useState } from 'react'
import { api } from '../api'
import { Button, EmptyState, Icon, MetricCard, Modal, SectionHead, StatusPill } from '../components/UI'
import type { InventoryInput, InventoryItem } from '../types'

export default function InventoryPage({ token, admin, globalSearch, notify, onChanged }: {
  token: string
  admin: boolean
  globalSearch: string
  notify: (type:'success'|'error'|'info', text:string)=>void
  onChanged: ()=>void
}) {
  const [items, setItems] = useState<InventoryItem[]>([])
  const [filter, setFilter] = useState<'ALL'|'LOW'|'OUT'>('ALL')
  const [editing, setEditing] = useState<InventoryItem | 'new' | null>(null)
  const [loading, setLoading] = useState(true)

  async function load() {
    setLoading(true)
    try { setItems(await api.inventory(token)) }
    catch (err) { notify('error', err instanceof Error ? err.message : 'Inventory load failed') }
    finally { setLoading(false) }
  }
  useEffect(() => { void load() }, [token])

  const filtered = useMemo(() => {
    const q = globalSearch.toLowerCase().trim()
    return items.filter(i => {
      const matchText = !q || i.sku.toLowerCase().includes(q) || i.name.toLowerCase().includes(q)
      const matchFilter = filter === 'ALL' || (filter === 'LOW' && i.availableQuantity <= 5 && i.availableQuantity > 0) || (filter === 'OUT' && i.availableQuantity === 0)
      return matchText && matchFilter
    })
  }, [items, globalSearch, filter])

  const totalAvailable = items.reduce((s,i)=>s+i.availableQuantity,0)
  const totalReserved = items.reduce((s,i)=>s+i.reservedQuantity,0)
  const low = items.filter(i=>i.availableQuantity<=5).length

  async function remove(item: InventoryItem) {
    if (!confirm(`Delete ${item.sku} — ${item.name}? Reserved inventory cannot be deleted.`)) return
    try {
      await api.deleteInventory(token, item.sku)
      setItems(prev=>prev.filter(i=>i.sku!==item.sku))
      notify('success', `${item.sku} deleted`); onChanged()
    } catch (err) { notify('error', err instanceof Error ? err.message : 'Delete failed') }
  }

  return <div className="page-stack">
    <div className="page-title-row"><div><span className="eyebrow">Inventory Service · :8082</span><h1>Inventory</h1><p>Manage stock positions, warehouse coordinates, and reserved quantities protected by PostgreSQL row locks.</p></div>{admin && <Button icon="plus" onClick={()=>setEditing('new')}>Add inventory</Button>}</div>
    <div className="metrics-grid compact">
      <MetricCard label="SKUs" value={items.length} sub="warehouse catalog" icon="box" tone="blue"/>
      <MetricCard label="Available" value={totalAvailable} sub="ready to reserve" icon="warehouse" tone="teal"/>
      <MetricCard label="Reserved" value={totalReserved} sub="allocated to orders" icon="shield" tone="purple"/>
      <MetricCard label="Low stock" value={low} sub="5 units or fewer" icon="alert" tone="amber"/>
    </div>
    <section className="panel">
      <div className="toolbar"><div className="filter-tabs"><button className={filter==='ALL'?'active':''} onClick={()=>setFilter('ALL')}>All stock</button><button className={filter==='LOW'?'active':''} onClick={()=>setFilter('LOW')}>Low stock</button><button className={filter==='OUT'?'active':''} onClick={()=>setFilter('OUT')}>Out of stock</button></div><button className="icon-button" onClick={()=>void load()}><Icon name="refresh"/></button></div>
      {loading ? <div className="skeleton-list"><div className="skeleton-row"/><div className="skeleton-row"/><div className="skeleton-row"/></div> : filtered.length ? <div className="inventory-grid">{filtered.map(item => <article className="inventory-card" key={item.sku}>
        <div className="inventory-card-top"><div><span className="sku-large">{item.sku}</span><h3>{item.name}</h3></div><span className={`stock-badge ${item.availableQuantity===0?'out':item.availableQuantity<=5?'low':''}`}>{item.availableQuantity===0?'Out':item.availableQuantity<=5?'Low':'Healthy'}</span></div>
        <div className="stock-bars"><div><span>Available</span><strong>{item.availableQuantity}</strong></div><div><span>Reserved</span><strong>{item.reservedQuantity}</strong></div></div>
        <div className="warehouse-position"><div className="mini-grid"><i style={{left:`${(item.warehouseX/19)*100}%`,top:`${(item.warehouseY/19)*100}%`}}/></div><div><span>Warehouse coordinate</span><strong>({item.warehouseX}, {item.warehouseY})</strong><small>row version {item.version}</small></div></div>
        {admin && <div className="card-actions"><Button variant="ghost" icon="edit" onClick={()=>setEditing(item)}>Edit</Button><Button variant="ghost" icon="trash" onClick={()=>void remove(item)}>Delete</Button></div>}
      </article>)}</div> : <EmptyState icon="warehouse" title="No inventory matches" text="Change the filter or add a new warehouse SKU."/>}
    </section>
    {editing && <InventoryModal item={editing==='new'?null:editing} token={token} onClose={()=>setEditing(null)} onSaved={saved=>{setItems(prev=>[saved,...prev.filter(i=>i.sku!==saved.sku)].sort((a,b)=>a.sku.localeCompare(b.sku)));setEditing(null);notify('success',`${saved.sku} saved`);onChanged()}} notify={notify}/>}
  </div>
}

function InventoryModal({ item, token, onClose, onSaved, notify }: { item: InventoryItem|null; token:string; onClose:()=>void; onSaved:(item:InventoryItem)=>void; notify:(type:'success'|'error'|'info',text:string)=>void }) {
  const [sku,setSku]=useState(item?.sku ?? '')
  const [form,setForm]=useState<InventoryInput>({name:item?.name??'',availableQuantity:item?.availableQuantity??10,warehouseX:item?.warehouseX??1,warehouseY:item?.warehouseY??1})
  const [busy,setBusy]=useState(false)
  async function submit(e:React.FormEvent){e.preventDefault();setBusy(true);try{onSaved(await api.upsertInventory(token,sku.trim().toUpperCase(),form))}catch(err){notify('error',err instanceof Error?err.message:'Save failed')}finally{setBusy(false)}}
  return <Modal title={item?`Edit ${item.sku}`:'Add warehouse SKU'} subtitle="Admin inventory writes use the same Spring Security/JWT backend as the rest of the platform." onClose={onClose}>
    <form className="form-grid" onSubmit={submit}>
      <label>SKU<input required disabled={!!item} value={sku} onChange={e=>setSku(e.target.value)} placeholder="SKU-M"/></label>
      <label>Product name<input required value={form.name} onChange={e=>setForm({...form,name:e.target.value})} placeholder="Product display name"/></label>
      <label>Available quantity<input required type="number" min="0" value={form.availableQuantity} onChange={e=>setForm({...form,availableQuantity:Number(e.target.value)})}/></label>
      <div className="field-pair"><label>Warehouse X<input required type="number" min="0" max="19" value={form.warehouseX} onChange={e=>setForm({...form,warehouseX:Number(e.target.value)})}/></label><label>Warehouse Y<input required type="number" min="0" max="19" value={form.warehouseY} onChange={e=>setForm({...form,warehouseY:Number(e.target.value)})}/></label></div>
      <div className="modal-actions"><Button type="button" variant="ghost" onClick={onClose}>Cancel</Button><Button type="submit" disabled={busy}>{busy?'Saving…':'Save inventory'}</Button></div>
    </form>
  </Modal>
}
