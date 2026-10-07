import type { ReactNode } from 'react'
import { stateLabel, stateTone } from '../utils'

export function Icon({ name, size = 18 }: { name: string; size?: number }) {
  const paths: Record<string, ReactNode> = {
    grid: <><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/></>,
    box: <><path d="M21 8l-9 5-9-5 9-5 9 5z"/><path d="M3 8v8l9 5 9-5V8"/><path d="M12 13v8"/></>,
    cart: <><circle cx="9" cy="20" r="1"/><circle cx="19" cy="20" r="1"/><path d="M3 4h2l2.4 10.2a2 2 0 002 1.6h7.7a2 2 0 001.9-1.4L21 7H6"/></>,
    warehouse: <><path d="M3 21V8l9-5 9 5v13"/><path d="M7 21v-8h10v8"/><path d="M7 10h.01M12 10h.01M17 10h.01"/></>,
    route: <><circle cx="5" cy="18" r="2"/><circle cx="19" cy="6" r="2"/><path d="M7 18c6 0 3-10 10-10"/><path d="M12 13l2-2 2 2"/></>,
    truck: <><path d="M3 6h11v10H3z"/><path d="M14 10h4l3 3v3h-7z"/><circle cx="7" cy="18" r="2"/><circle cx="18" cy="18" r="2"/></>,
    pulse: <><path d="M3 12h4l2-5 4 10 2-5h6"/></>,
    search: <><circle cx="11" cy="11" r="7"/><path d="M20 20l-4-4"/></>,
    plus: <><path d="M12 5v14M5 12h14"/></>,
    trash: <><path d="M4 7h16M9 7V4h6v3M7 7l1 13h8l1-13"/></>,
    edit: <><path d="M4 20l4-1 10-10-3-3L5 16l-1 4z"/><path d="M14 7l3 3"/></>,
    refresh: <><path d="M20 7v5h-5"/><path d="M4 17v-5h5"/><path d="M6.1 9A7 7 0 0118 7l2 5M4 12l2 5a7 7 0 0011.9-2"/></>,
    copy: <><rect x="8" y="8" width="11" height="11" rx="2"/><path d="M16 8V5a2 2 0 00-2-2H5a2 2 0 00-2 2v9a2 2 0 002 2h3"/></>,
    close: <><path d="M6 6l12 12M18 6L6 18"/></>,
    chevron: <><path d="M9 18l6-6-6-6"/></>,
    user: <><circle cx="12" cy="8" r="4"/><path d="M4 21a8 8 0 0116 0"/></>,
    shield: <><path d="M12 3l7 3v5c0 5-3 8-7 10-4-2-7-5-7-10V6l7-3z"/><path d="M9 12l2 2 4-4"/></>,
    bolt: <><path d="M13 2L5 14h6l-1 8 8-12h-6l1-8z"/></>,
    logout: <><path d="M10 17l5-5-5-5M15 12H3"/><path d="M14 3h5a2 2 0 012 2v14a2 2 0 01-2 2h-5"/></>,
    clock: <><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></>,
    alert: <><path d="M12 3L2 20h20L12 3z"/><path d="M12 9v4M12 17h.01"/></>,
    spark: <><path d="M12 2l1.4 4.6L18 8l-4.6 1.4L12 14l-1.4-4.6L6 8l4.6-1.4L12 2z"/><path d="M19 14l.8 2.2L22 17l-2.2.8L19 20l-.8-2.2L16 17l2.2-.8L19 14z"/></>
  }
  return <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{paths[name] ?? paths.grid}</svg>
}

export function Button({ children, variant = 'primary', icon, className = '', ...props }: React.ButtonHTMLAttributes<HTMLButtonElement> & { variant?: 'primary' | 'secondary' | 'ghost' | 'danger' | 'amber'; icon?: string }) {
  return <button className={`btn btn-${variant} ${className}`} {...props}>{icon && <Icon name={icon} size={16}/>}<span>{children}</span></button>
}

export function StatusPill({ state }: { state: string }) {
  return <span className={`status status-${stateTone(state)}`}><span className="status-dot" />{stateLabel(state)}</span>
}

export function MetricCard({ label, value, sub, icon, tone = 'blue' }: { label: string; value: string | number; sub?: string; icon: string; tone?: 'blue' | 'teal' | 'amber' | 'purple' }) {
  return <div className="metric-card">
    <div className={`metric-icon metric-${tone}`}><Icon name={icon} size={22}/></div>
    <div className="metric-content"><span>{label}</span><strong>{value}</strong>{sub && <small>{sub}</small>}</div>
  </div>
}

export function EmptyState({ icon = 'box', title, text, action }: { icon?: string; title: string; text: string; action?: ReactNode }) {
  return <div className="empty-state"><div className="empty-icon"><Icon name={icon} size={28}/></div><h3>{title}</h3><p>{text}</p>{action}</div>
}

export function Modal({ title, subtitle, onClose, children, wide = false }: { title: string; subtitle?: string; onClose: () => void; children: ReactNode; wide?: boolean }) {
  return <div className="modal-backdrop" onMouseDown={onClose}>
    <section className={`modal ${wide ? 'modal-wide' : ''}`} onMouseDown={e => e.stopPropagation()}>
      <header className="modal-head"><div><h2>{title}</h2>{subtitle && <p>{subtitle}</p>}</div><button className="icon-button" onClick={onClose}><Icon name="close"/></button></header>
      <div className="modal-body">{children}</div>
    </section>
  </div>
}

export function SectionHead({ eyebrow, title, action }: { eyebrow?: string; title: string; action?: ReactNode }) {
  return <div className="section-head"><div>{eyebrow && <span className="eyebrow">{eyebrow}</span>}<h2>{title}</h2></div>{action}</div>
}

export function LoadingBlock({ rows = 4 }: { rows?: number }) {
  return <div className="skeleton-list">{Array.from({ length: rows }).map((_, i) => <div key={i} className="skeleton-row" />)}</div>
}
