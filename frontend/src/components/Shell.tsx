import type { AuthClaims, ServiceHealth } from '../types'
import { Icon } from './UI'

export type PageKey = 'dashboard' | 'orders' | 'inventory' | 'fulfillment' | 'shipping' | 'system'

const nav: { key: PageKey; label: string; icon: string }[] = [
  { key: 'dashboard', label: 'Command Center', icon: 'grid' },
  { key: 'orders', label: 'Orders', icon: 'cart' },
  { key: 'inventory', label: 'Inventory', icon: 'warehouse' },
  { key: 'fulfillment', label: 'Fulfillment Lab', icon: 'route' },
  { key: 'shipping', label: 'Shipping', icon: 'truck' },
  { key: 'system', label: 'System Mesh', icon: 'pulse' }
]

export default function Shell({ claims, page, setPage, search, setSearch, health, onLogout, children }: {
  claims: AuthClaims
  page: PageKey
  setPage: (page: PageKey) => void
  search: string
  setSearch: (value: string) => void
  health: ServiceHealth[]
  onLogout: () => void
  children: React.ReactNode
}) {
  const online = health.filter(h => h.online).length
  const admin = claims.roles?.includes('ROLE_ADMIN')
  return <div className="app-shell">
    <header className="topbar">
      <button className="brand-lockup" onClick={() => setPage('dashboard')}><div className="mesh-logo"><span/><span/><span/></div><div><strong>FulfillMesh</strong><small>Operations Console</small></div></button>
      <div className="global-search"><span className="search-scope">All</span><input value={search} onChange={e => setSearch(e.target.value)} placeholder="Search order ID, SKU, tracking number…"/><button onClick={() => page === 'dashboard' && setPage('orders')}><Icon name="search" size={20}/></button></div>
      <div className="top-actions">
        <div className={`mesh-health ${online === 4 ? 'healthy' : ''}`}><span className="live-dot"/><div><small>Service mesh</small><strong>{online}/4 online</strong></div></div>
        <div className="account-pill"><div className="avatar">{claims.sub?.slice(0, 1).toUpperCase()}</div><div><small>{admin ? 'Administrator' : 'Operator'}</small><strong>{claims.sub}</strong></div></div>
        <button className="logout-button" onClick={onLogout} title="Sign out"><Icon name="logout"/></button>
      </div>
    </header>
    <nav className="subnav">
      <div className="nav-inner">{nav.map(item => <button key={item.key} className={page === item.key ? 'active' : ''} onClick={() => setPage(item.key)}><Icon name={item.icon} size={17}/>{item.label}</button>)}<div className="nav-spacer"/><span className="env-tag"><span/>LOCAL</span></div>
    </nav>
    <main className="main-content">{children}</main>
  </div>
}
