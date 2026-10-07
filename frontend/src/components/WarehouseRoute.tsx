import { useMemo } from 'react'
import { Icon } from './UI'

export type RoutePoint = { x: number; y: number }

export function parseRoute(route?: string | null): RoutePoint[] {
  if (!route) return []
  const points: RoutePoint[] = []
  const regex = /GridPoint\[x=(\d+), y=(\d+)\]/g
  let match: RegExpExecArray | null
  while ((match = regex.exec(route))) points.push({ x: Number(match[1]), y: Number(match[2]) })
  return points
}

export default function WarehouseRoute({ route, height = 420 }: { route?: string | null; height?: number }) {
  const points = useMemo(() => parseRoute(route), [route])
  const maxCoord = 19
  const polyline = points.map(p => `${(p.x / maxCoord) * 100},${(p.y / maxCoord) * 100}`).join(' ')
  const uniqueStops = points.filter((p, index) => {
    if (index === 0 || index === points.length - 1) return true
    const prev = points[index - 1]
    const next = points[index + 1]
    return (p.x - prev.x) !== (next.x - p.x) || (p.y - prev.y) !== (next.y - p.y)
  })

  if (!points.length) return <div className="route-empty"><Icon name="route" size={30}/><strong>Route not available yet</strong><span>The A* path appears after inventory reservation and picking complete.</span></div>

  return <div className="warehouse-map" style={{ height }}>
    <div className="warehouse-grid" />
    <svg className="route-svg" viewBox="-3 -3 106 106" preserveAspectRatio="none">
      <polyline points={polyline} fill="none" stroke="currentColor" strokeWidth="1.45" vectorEffect="non-scaling-stroke" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
    {uniqueStops.map((p, i) => <div key={`${p.x}-${p.y}-${i}`} className={`route-node ${i === 0 ? 'route-start' : ''}`} style={{ left: `${(p.x / maxCoord) * 100}%`, top: `${(p.y / maxCoord) * 100}%` }} title={`(${p.x}, ${p.y})`}><span>{i === 0 ? 'P' : ''}</span></div>)}
    <div className="map-legend"><span><i className="legend-packing"/>Packing station</span><span><i className="legend-path"/>Optimized A* path</span></div>
  </div>
}
