import { useEffect, useMemo, useState } from 'react'
import { api } from '../api'
import WarehouseRoute from '../components/WarehouseRoute'
import { Button, EmptyState, Icon, MetricCard, SectionHead, StatusPill } from '../components/UI'
import type { Fulfillment } from '../types'
import { formatDate, shortId } from '../utils'

export default function FulfillmentPage({ token, admin, globalSearch, notify }: { token:string; admin:boolean; globalSearch:string; notify:(type:'success'|'error'|'info',text:string)=>void }) {
  const [items,setItems]=useState<Fulfillment[]>([])
  const [selected,setSelected]=useState<Fulfillment|null>(null)
  const [orderId,setOrderId]=useState('')
  const [loading,setLoading]=useState(admin)

  async function loadAll(){if(!admin)return;setLoading(true);try{const data=await api.fulfillments(token);setItems(data);if(!selected&&data.length)setSelected(data[0])}catch(err){notify('error',err instanceof Error?err.message:'Fulfillment load failed')}finally{setLoading(false)}}
  useEffect(()=>{void loadAll()},[token,admin])

  const filtered=useMemo(()=>{const q=globalSearch.trim().toLowerCase();return !q?items:items.filter(i=>i.orderId.toLowerCase().includes(q)||i.id.toLowerCase().includes(q))},[items,globalSearch])
  const completed=items.filter(i=>i.state==='COMPLETED')
  const avg=completed.length?completed.reduce((s,i)=>s+(i.improvementPercent??0),0)/completed.length:0

  async function lookup(e:React.FormEvent){e.preventDefault();try{const found=await api.fulfillmentByOrder(token,orderId.trim());setSelected(found);if(!items.some(i=>i.id===found.id))setItems(prev=>[found,...prev])}catch(err){notify('error',err instanceof Error?err.message:'Fulfillment not found')}}

  return <div className="page-stack">
    <div className="page-title-row"><div><span className="eyebrow">Fulfillment Service · :8083</span><h1>Fulfillment Lab</h1><p>Inspect A* warehouse routing, baseline distance, optimized paths, and task state transitions.</p></div>{admin&&<Button variant="secondary" icon="refresh" onClick={()=>void loadAll()}>Refresh tasks</Button>}</div>
    <div className="lookup-bar"><form onSubmit={lookup}><Icon name="search" size={18}/><input value={orderId} onChange={e=>setOrderId(e.target.value)} placeholder="Lookup fulfillment by full order UUID"/><Button type="submit">Inspect order</Button></form></div>
    {admin && <div className="metrics-grid compact"><MetricCard label="Tasks" value={items.length} sub="fulfillment records" icon="route" tone="blue"/><MetricCard label="Completed" value={completed.length} sub="optimized routes" icon="shield" tone="teal"/><MetricCard label="Avg reduction" value={`${avg.toFixed(1)}%`} sub="vs sequential picking" icon="spark" tone="amber"/><MetricCard label="Algorithm" value="A*" sub="20 × 20 grid" icon="bolt" tone="purple"/></div>}
    <div className="fulfillment-layout">
      {admin && <section className="panel task-list-panel"><SectionHead eyebrow="Tasks" title="Picking queue"/>{loading?<div className="skeleton-list"><div className="skeleton-row"/><div className="skeleton-row"/></div>:filtered.length?<div className="task-list">{filtered.map(item=><button key={item.id} className={selected?.id===item.id?'active':''} onClick={()=>setSelected(item)}><div><strong>Order #{shortId(item.orderId,10)}</strong><small>{formatDate(item.createdAt)}</small></div><StatusPill state={item.state}/><span className="task-gain">{item.improvementPercent!=null?`−${item.improvementPercent.toFixed(1)}%`:'—'}</span></button>)}</div>:<EmptyState icon="route" title="No fulfillment tasks" text="Create an order with available inventory to generate a route."/>}</section>}
      <section className={`panel route-panel ${!admin?'route-panel-full':''}`}>
        {selected ? <>
          <SectionHead eyebrow={`Order #${shortId(selected.orderId,12)}`} title="Warehouse route" action={<StatusPill state={selected.state}/>}/>
          <WarehouseRoute route={selected.pickRoute}/>
          <div className="route-metrics"><div><span>Optimized</span><strong>{selected.optimizedDistance ?? '—'}</strong><small>grid cells</small></div><div><span>Sequential</span><strong>{selected.sequentialDistance ?? '—'}</strong><small>baseline cells</small></div><div className="gain"><span>Reduction</span><strong>{selected.improvementPercent!=null?`${selected.improvementPercent.toFixed(2)}%`:'—'}</strong><small>travel saved</small></div><div><span>Version</span><strong>v{selected.version}</strong><small>optimistic lock</small></div></div>
        </> : <EmptyState icon="route" title="Select a fulfillment task" text={admin?'Choose a task from the queue or lookup an order ID.':'Paste one of your order UUIDs above to inspect its route.'}/>}
      </section>
    </div>
  </div>
}
