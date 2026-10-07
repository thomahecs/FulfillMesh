import { useEffect, useState } from 'react'
import { checkHealth } from '../api'
import { Button, Icon, SectionHead } from '../components/UI'
import type { ServiceHealth } from '../types'

const topics = [
  ['orders.created','Order → Inventory'],
  ['inventory.reserved','Inventory → Fulfillment'],
  ['inventory.rejected','Inventory → Order'],
  ['picking.completed','Fulfillment → Shipping'],
  ['shipping.created','Shipping → Order']
]

export default function SystemPage({ initialHealth }: { initialHealth: ServiceHealth[] }) {
  const [health,setHealth]=useState(initialHealth)
  const [checking,setChecking]=useState(false)
  async function refresh(){setChecking(true);setHealth(await checkHealth());setChecking(false)}
  useEffect(()=>{setHealth(initialHealth)},[initialHealth])
  useEffect(()=>{void refresh();const timer=setInterval(()=>void refresh(),5000);return()=>clearInterval(timer)},[])
  const allUp=health.length===4&&health.every(h=>h.online)

  return <div className="page-stack">
    <div className="page-title-row"><div><span className="eyebrow">Distributed runtime</span><h1>System Mesh</h1><p>Live health, service topology, Kafka topics, and local infrastructure for the FulfillMesh backend.</p></div><Button variant="secondary" icon="refresh" onClick={()=>void refresh()} disabled={checking}>{checking?'Checking…':'Run health check'}</Button></div>
    <section className={`system-banner ${allUp?'up':'degraded'}`}><div><span className="live-dot"/><strong>{allUp?'All application services operational':'Service mesh degraded'}</strong><p>{allUp?'Order, inventory, fulfillment, and shipping APIs are responding.':'Start the missing Spring Boot service(s) in IntelliJ and retry.'}</p></div><span>{health.filter(h=>h.online).length}/4</span></section>
    <div className="service-health-grid">{health.map(h=><article className="health-card" key={h.key}><div className="health-head"><div className={`service-icon service-${h.key}`}><Icon name={h.key==='order'?'cart':h.key==='inventory'?'warehouse':h.key==='fulfillment'?'route':'truck'} size={22}/></div><span className={`health-status ${h.online?'online':'offline'}`}>{h.online?'ONLINE':'OFFLINE'}</span></div><h3>{h.name}</h3><div className="health-details"><div><span>Port</span><strong>{h.port}</strong></div><div><span>Health</span><strong>/actuator/health</strong></div><div><span>Latency</span><strong>{h.latencyMs==null?'—':`${h.latencyMs} ms`}</strong></div></div></article>)}</div>
    <div className="system-grid">
      <section className="panel"><SectionHead eyebrow="Kafka" title="Event backbone"/><div className="topic-list">{topics.map(([topic,flow])=><div key={topic}><span className="topic-dot"/><div><strong>{topic}</strong><small>{flow}</small></div><span>JSON</span></div>)}</div></section>
      <section className="panel"><SectionHead eyebrow="State" title="Data layer"/><div className="infra-list"><div><div className="infra-icon">PG</div><div><strong>PostgreSQL 16</strong><small>schemas: orders · inventory · fulfillment · shipping</small></div><span>5432</span></div><div><div className="infra-icon">R</div><div><strong>Redis 7.4</strong><small>HTTP + event idempotency keys</small></div><span>6379</span></div><div><div className="infra-icon">K</div><div><strong>Apache Kafka</strong><small>asynchronous fulfillment workflow</small></div><span>29092</span></div></div></section>
      <section className="panel panel-wide"><SectionHead eyebrow="Architecture" title="Request + event flow"/><div className="architecture-flow"><div className="arch-client"><Icon name="user"/><strong>React Console</strong><small>Vite :5173</small></div><b>REST + JWT</b><div className="arch-services"><span>Order :8081</span><span>Inventory :8082</span><span>Fulfillment :8083</span><span>Shipping :8084</span></div><b>events</b><div className="arch-kafka"><Icon name="bolt"/><strong>Kafka</strong><small>5 topics</small></div></div></section>
    </div>
  </div>
}
