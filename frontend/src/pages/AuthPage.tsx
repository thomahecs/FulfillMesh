import { useState } from 'react'
import { api } from '../api'
import { Button, Icon } from '../components/UI'

export default function AuthPage({ onAuthenticated }: { onAuthenticated: (token: string) => void }) {
  const [mode, setMode] = useState<'login' | 'register'>('login')
  const [email, setEmail] = useState('admin@fulfillmesh.local')
  const [password, setPassword] = useState('admin12345')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  async function submit(e: React.FormEvent) {
    e.preventDefault()
    setLoading(true); setError('')
    try {
      const result = mode === 'login' ? await api.login(email, password) : await api.register(email, password)
      onAuthenticated(result.token)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Authentication failed')
    } finally { setLoading(false) }
  }

  function useDemo(admin: boolean) {
    setMode('login')
    setEmail(admin ? 'admin@fulfillmesh.local' : 'demo@fulfillmesh.local')
    setPassword(admin ? 'admin12345' : 'demo12345')
  }

  return <div className="auth-page">
    <div className="auth-brand-panel">
      <div className="brand-lockup large"><div className="mesh-logo"><span/><span/><span/></div><div><strong>FulfillMesh</strong><small>Distributed Fulfillment Platform</small></div></div>
      <div className="auth-hero-copy">
        <span className="hero-kicker"><Icon name="spark" size={16}/> Event-driven operations console</span>
        <h1>From checkout to shipment, every warehouse step in one view.</h1>
        <p>Operate orders, inventory, A* picking routes, and shipping across four Spring Boot services backed by PostgreSQL, Redis, and Kafka.</p>
      </div>
      <div className="auth-flow">
        {['Order', 'Inventory', 'Fulfillment', 'Shipping'].map((item, i) => <div key={item} className="auth-flow-item"><span>{i + 1}</span><strong>{item}</strong>{i < 3 && <b>→</b>}</div>)}
      </div>
      <div className="auth-tech-row"><span>Java 21</span><span>Kafka</span><span>Redis</span><span>PostgreSQL</span><span>A* Routing</span></div>
    </div>

    <div className="auth-form-panel">
      <div className="auth-card">
        <div className="auth-card-head"><span className="eyebrow">Operations access</span><h2>{mode === 'login' ? 'Sign in to FulfillMesh' : 'Create an operator account'}</h2><p>{mode === 'login' ? 'Use the seeded demo account or your own credentials.' : 'New accounts are created with the standard user role.'}</p></div>
        <div className="segmented"><button className={mode === 'login' ? 'active' : ''} onClick={() => setMode('login')}>Sign in</button><button className={mode === 'register' ? 'active' : ''} onClick={() => setMode('register')}>Register</button></div>
        <form onSubmit={submit} className="auth-form">
          <label>Email address<input type="email" required value={email} onChange={e => setEmail(e.target.value)} placeholder="operator@example.com" /></label>
          <label>Password<input type="password" required minLength={8} value={password} onChange={e => setPassword(e.target.value)} placeholder="8+ characters" /></label>
          {error && <div className="form-error"><Icon name="alert" size={16}/>{error}</div>}
          <Button type="submit" className="btn-block" disabled={loading}>{loading ? 'Connecting…' : mode === 'login' ? 'Sign in' : 'Create account'}</Button>
        </form>
        <div className="demo-login"><span>Local demo accounts</span><div><button onClick={() => useDemo(true)}><Icon name="shield" size={15}/>Admin demo</button><button onClick={() => useDemo(false)}><Icon name="user" size={15}/>User demo</button></div></div>
        <div className="auth-note"><Icon name="pulse" size={16}/><span>Frontend requests are proxied by Vite to ports 8081–8084, so no CORS setup is needed.</span></div>
      </div>
    </div>
  </div>
}
