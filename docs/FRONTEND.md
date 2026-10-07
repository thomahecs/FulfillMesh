# FulfillMesh Operations Console

The frontend lives in `frontend/` and runs inside the same IntelliJ project as the Spring modules.

## Design

The UI is an operations dashboard rather than a marketing page. Its layout borrows familiar commerce-console patterns: a dark navy top bar, a wide global search field, a second-level navigation bar, dense data tables, compact cards, and strong blue hierarchy. Warm amber is used only as an accent so the application still reads as a blue enterprise platform.

No component framework is required. The console uses React, TypeScript, Vite, and hand-written CSS. That keeps the setup small and makes the visual work easy to inspect in a portfolio review.

## Backend coverage

| Backend capability | Frontend surface |
| --- | --- |
| JWT registration/login | Sign-in and registration screen |
| User/admin roles | Role-aware navigation and admin controls |
| Idempotent order creation | Order builder with generated idempotency key |
| Order lookup/state | Order ledger and auto-refreshing detail panel |
| Inventory listing | Inventory catalog and stock metrics |
| Inventory upsert | Admin add/edit inventory modal |
| Inventory delete | Admin safe-delete action; reserved stock is protected |
| PostgreSQL row locking | Explained in the inventory/order workflow surfaces |
| Fulfillment lookup | Fulfillment Lab lookup by order UUID |
| A* optimized route | Interactive warehouse route visualization |
| Sequential vs optimized metrics | Fulfillment metrics panel |
| Shipment lookup | Shipping tracker by order UUID |
| Service health | System Mesh health cards for ports 8081-8084 |
| Kafka workflow | Live architecture/event-topology panels |

Admin-only aggregate APIs were added for order operations, fulfillment tasks, and shipments so the console can act as a real operator dashboard instead of requiring IDs to be pasted one at a time.

## Local setup

Start Docker infrastructure and the four Spring Boot services exactly as before. Then:

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`.

The Vite development server proxies paths to the correct Spring Boot service:

```text
/api/auth, /api/orders  -> 8081
/api/inventory          -> 8082
/api/fulfillments       -> 8083
/api/shipments          -> 8084
/health/*               -> each service's /actuator/health
```

Because the browser talks to Vite on one origin, the backend does not need a local CORS configuration.

## Recommended demo

Sign in as:

```text
admin@fulfillmesh.local
admin12345
```

Then use this sequence:

1. Open **Inventory** and show the stock/warehouse coordinates.
2. Open **Orders**, click **Create order**, then **Load A* benchmark basket**.
3. Place the order. The detail view automatically polls while Kafka advances the workflow.
4. When picking completes, show the A* warehouse route and the optimized/sequential distances.
5. Open **Shipping** to show the generated tracking record.
6. Open **System Mesh** to show service health and the Kafka event topology.

This demo exposes the backend design directly through the UI instead of hiding the distributed-systems work behind generic CRUD screens.
