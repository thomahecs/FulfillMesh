import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

const healthProxy = (port: number) => ({
  target: `http://localhost:${port}`,
  changeOrigin: true,
  rewrite: (path: string) => path.replace(/^\/health\/[^/]+/, '/actuator/health')
})

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api/auth': { target: 'http://localhost:8081', changeOrigin: true },
      '/api/orders': { target: 'http://localhost:8081', changeOrigin: true },
      '/api/inventory': { target: 'http://localhost:8082', changeOrigin: true },
      '/api/fulfillments': { target: 'http://localhost:8083', changeOrigin: true },
      '/api/shipments': { target: 'http://localhost:8084', changeOrigin: true },
      '/health/order': healthProxy(8081),
      '/health/inventory': healthProxy(8082),
      '/health/fulfillment': healthProxy(8083),
      '/health/shipping': healthProxy(8084)
    }
  }
})
