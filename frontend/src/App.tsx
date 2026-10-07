import { useEffect, useMemo, useState } from 'react'
import { checkHealth } from './api'
import Shell, { type PageKey } from './components/Shell'
import ToastStack from './components/ToastStack'
import AuthPage from './pages/AuthPage'
import DashboardPage from './pages/DashboardPage'
import OrdersPage from './pages/OrdersPage'
import InventoryPage from './pages/InventoryPage'
import FulfillmentPage from './pages/FulfillmentPage'
import ShippingPage from './pages/ShippingPage'
import SystemPage from './pages/SystemPage'
import type { ServiceHealth, ToastMessage } from './types'
import { parseToken } from './utils'

const TOKEN_KEY = 'fulfillmesh.token'

export default function App() {
  const [token, setToken] = useState(() => localStorage.getItem(TOKEN_KEY) ?? '')
  const [page, setPage] = useState<PageKey>('dashboard')
  const [search, setSearch] = useState('')
  const [health, setHealth] = useState<ServiceHealth[]>([])
  const [toasts, setToasts] = useState<ToastMessage[]>([])
  const [refreshSignal, setRefreshSignal] = useState(0)
  const claims = useMemo(() => token ? parseToken(token) : null, [token])

  function authenticate(nextToken: string) {
    localStorage.setItem(TOKEN_KEY, nextToken)
    setToken(nextToken)
  }

  function logout() {
    localStorage.removeItem(TOKEN_KEY)
    setToken('')
    setPage('dashboard')
    setSearch('')
  }

  function notify(type: ToastMessage['type'], text: string) {
    const id = Date.now() + Math.round(Math.random() * 1000)
    setToasts(prev => [...prev, { id, type, text }])
    window.setTimeout(() => setToasts(prev => prev.filter(t => t.id !== id)), 4200)
  }

  useEffect(() => {
    if (!token) return
    let mounted = true
    const update = async () => { const next = await checkHealth(); if (mounted) setHealth(next) }
    void update()
    const timer = window.setInterval(() => void update(), 10000)
    return () => { mounted = false; clearInterval(timer) }
  }, [token])

  if (!token || !claims) return <AuthPage onAuthenticated={authenticate} />

  const admin = claims.roles?.includes('ROLE_ADMIN') ?? false
  const changed = () => setRefreshSignal(v => v + 1)

  return <>
    <Shell claims={claims} page={page} setPage={setPage} search={search} setSearch={setSearch} health={health} onLogout={logout}>
      {page === 'dashboard' && <DashboardPage token={token} admin={admin} health={health} globalSearch={search} navigate={setPage} refreshSignal={refreshSignal}/>}
      {page === 'orders' && <OrdersPage token={token} admin={admin} globalSearch={search} notify={notify} onChanged={changed}/>}
      {page === 'inventory' && <InventoryPage token={token} admin={admin} globalSearch={search} notify={notify} onChanged={changed}/>}
      {page === 'fulfillment' && <FulfillmentPage token={token} admin={admin} globalSearch={search} notify={notify}/>}
      {page === 'shipping' && <ShippingPage token={token} admin={admin} globalSearch={search} notify={notify}/>}
      {page === 'system' && <SystemPage initialHealth={health}/>}
    </Shell>
    <ToastStack messages={toasts} dismiss={id => setToasts(prev => prev.filter(t => t.id !== id))}/>
  </>
}
