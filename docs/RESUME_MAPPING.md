# Resume claim -> implementation map

This file exists so the project can be checked against the resume bullets without hunting through every package.

## Event-driven order and fulfillment backend

Resume idea: order creation, reservation, picking, shipment processing, asynchronous fulfillment.

Implemented in:

- `order-service/.../OrderApplicationService.java`
- `inventory-service/.../InventoryEventProcessor.java`
- `inventory-service/.../InventoryReservationService.java`
- `fulfillment-service/.../FulfillmentProcessor.java`
- `shipping-service/.../ShippingProcessor.java`
- `platform-common/.../events/*`
- `docker-compose.yml`

Kafka topics:

```text
orders.created
inventory.reserved
inventory.rejected
picking.completed
shipping.created
```

## Docker and AWS EC2/RDS layout

Implemented in:

- root `Dockerfile`
- root `docker-compose.yml`
- `deploy/aws/docker-compose.aws.yml`
- `deploy/aws/README.md`
- `deploy/aws/.env.aws.example`

The code is deployment-ready, but an actual AWS account deployment still has to be done with the owner's credentials and billing settings.

## PostgreSQL locking + optimistic versioning + Redis idempotency

Row-level lock:

- `InventoryRepository.findBySkuForUpdate`
- `InventoryReservationService.reserve`

The reservation code locks SKUs in sorted order and validates every row before changing stock.

Optimistic version fields:

- `OrderEntity.version`
- `InventoryItem.version`
- `FulfillmentTask.version`
- `Shipment.version`

Redis:

- `OrderApplicationService` stores HTTP idempotency keys.
- Inventory, fulfillment, shipping, and order event consumers store processed event ids.

## Kafka decoupling

Every workflow transition is a Kafka event. Inventory does not call fulfillment over HTTP, and fulfillment does not call shipping over HTTP.

The shared event contracts are under:

```text
platform-common/src/main/java/com/fulfillmesh/common/events
```

## A* route engine and 10,000-order benchmark

Implementation:

- `WarehouseGrid`
- `AStarRouter`
- `PickingRoutePlanner`
- `RoutingBenchmarkTest`
- `docs/ROUTING_BENCHMARK.md`

Eight-item regression test: `102 -> 80` grid steps (21.57%).

Separate **10,000 generated orders** (18–22 walkable stops; seed 20261016):
- A*-distance nearest neighbor: 1,255,014 grid steps
- Sequential aisle-sorted: 1,865,602 grid steps
- Optimized: 1,098,176 grid steps
- Reduction vs nearest: **12.4969%**
- Reduction vs sequential: **41.1356%**

All figures come from `scripts/benchmark-routing.sh`, saved in
`reports/routing-benchmark.json`. These are simulated grid steps, not
measured warehouse operations. Detailed assumptions: `docs/ROUTING_BENCHMARK.md`.


## Spring Security + JWT

Implementation:

- `/api/auth/register`
- `/api/auth/login`
- `JwtTokenService`
- one stateless JWT filter/security configuration per service

Order creation, inventory reads/admin updates, fulfillment queries, and shipment queries require a bearer token. Inventory writes require the admin role.

## JUnit 5, Testcontainers, and coverage

Implementation:

- module unit tests under `src/test/java`
- `order-service/.../InfrastructureSmokeTest.java` starts PostgreSQL, Redis, and Kafka containers
- JaCoCo `coverage` Maven profile
- `scripts/run-coverage.sh`
- `inventory-service/.../ReservationLoadTest.java` (500 attempts / 100 units,
  saves actual p95 latency)
- `scripts/benchmark-concurrency.sh`

The coverage profile is configured with an 85% line threshold for core logic and excludes configuration/security/DTO/entity/controller glue.

## Evidence status (important)

The 10,000-order A* result is checked in the editing environment.
The new concurrency test, end-to-end Kafka flow, and post-edit JaCoCo gate
require running on Docker/Maven and **are not yet certified**. Avoid treating
unexecuted benchmark targets as measured results. See `docs/VERIFICATION.md`.
