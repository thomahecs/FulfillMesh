import { useEffect, useMemo, useState } from 'react'
import { api } from '../api'
import { Button, EmptyState, Icon, MetricCard, SectionHead, StatusPill } from '../components/UI'
import type { Shipment } from '../types'
import { copyText, formatDate, shortId } from '../utils'

export default function ShippingPage({ token, admin, globalSearch, notify }: { token:string; admin:boolean; globalSearch:string; notify:(type:'success'|'error'|'info',text:string)=>void }) {
  const [shipments,setShipments]=useState<Shipment[]>([])
  const [orderId,setOrderId]=useState('')
  const [lookup,setLookup]=useState<Shipment|null>(null)
  const [loading,setLoading]=useState(admin)

  async function load(){if(!admin)return;setLoading(true);try{setShipments(await api.shipments(token))}catch(err){notify('error',err instanceof Error?err.message:'Shipment load failed')}finally{setLoading(false)}}
  useEffect(()=>{void load()},[token,admin])
  const filtered=useMemo(()=>{const q=globalSearch.trim().toLowerCase();return !q?shipments:shipments.filter(s=>s.orderId.toLowerCase().includes(q)||s.trackingNumber.toLowerCase().includes(q)||s.id.toLowerCase().includes(q))},[shipments,globalSearch])
  const labelReady=shipments.filter(s=>s.status==='LABEL_READY').length
  const inTransit=shipments.filter(s=>s.status==='IN_TRANSIT').length
  async function search(e:React.FormEvent){e.preventDefault();try{setLookup(await api.shipmentByOrder(token,orderId.trim()))}catch(err){notify('error',err instanceof Error?err.message:'Shipment not found')}}

  return <div className="page-stack">
    <div className="page-title-row"><div><span className="eyebrow">Shipping Service · :8084</span><h1>Shipping</h1><p>Track shipment records created asynchronously after picking.completed reaches the shipping worker.</p></div>{admin&&<Button variant="secondary" icon="refresh" onClick={()=>void load()}>Refresh shipments</Button>}</div>
    <div className="lookup-bar"><form onSubmit={search}><Icon name="search" size={18}/><input value={orderId} onChange={e=>setOrderId(e.target.value)} placeholder="Lookup shipment by full order UUID"/><Button type="submit">Track order</Button></form></div>
    {lookup && <section className="tracking-hero"><div className="tracking-icon"><Icon name="truck" size={34}/></div><div className="tracking-main"><span>Tracking number</span><strong>{lookup.trackingNumber}</strong><button onClick={()=>{void copyText(lookup.trackingNumber);notify('info','Tracking number copied')}}><Icon name="copy" size={14}/>Copy</button></div><div className="tracking-meta"><StatusPill state={lookup.status}/><span>Order #{shortId(lookup.orderId,12)}</span><small>{formatDate(lookup.createdAt)}</small></div></section>}
    {admin && <>
      <div className="metrics-grid compact"><MetricCard label="Shipments" value={shipments.length} sub="total generated" icon="truck" tone="blue"/><MetricCard label="Label ready" value={labelReady} sub="ready for carrier" icon="shield" tone="teal"/><MetricCard label="In transit" value={inTransit} sub="carrier handoff" icon="route" tone="purple"/><MetricCard label="Tracking prefix" value="FM-" sub="local demo carrier" icon="box" tone="amber"/></div>
      <section className="panel"><SectionHead eyebrow="Shipment ledger" title="Recent shipments"/>{loading?<div className="skeleton-list"><div className="skeleton-row"/><div className="skeleton-row"/></div>:filtered.length?<div className="shipment-grid">{filtered.map(s=><article className="shipment-card" key={s.id}><div className="shipment-head"><div className="truck-badge"><Icon name="truck"/></div><StatusPill state={s.status}/></div><span>Tracking</span><h3>{s.trackingNumber}</h3><div className="shipment-lines"><div><span>Order</span><strong className="mono">#{shortId(s.orderId,12)}</strong></div><div><span>Created</span><strong>{formatDate(s.createdAt)}</strong></div><div><span>Version</span><strong>v{s.version}</strong></div></div><Button variant="ghost" icon="copy" onClick={()=>{void copyText(s.trackingNumber);notify('info','Tracking number copied')}}>Copy tracking</Button></article>)}</div>:<EmptyState icon="truck" title="No shipments yet" text="Shipments appear after a fulfillment task completes."/>}</section>
    </>}
    {!admin && !lookup && <section className="panel"><EmptyState icon="truck" title="Track one of your orders" text="Paste the order UUID above. A shipment is created automatically when picking completes."/></section>}
  </div>
}
