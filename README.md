# FulfillMesh

FulfillMesh is a distributed order-fulfillment platform with a Spring Boot service mesh and a React operations console. I built it around a normal warehouse flow: an order comes in, stock is reserved, a pick route is planned, and a shipment is created. The services do not call each other directly for the workflow. They pass events through Kafka instead.

The project is intentionally runnable on one laptop. PostgreSQL, Redis, and Kafka run in Docker, while the four Spring Boot services can either run from IntelliJ or from Docker too.

## Stack

Java 21, Spring Boot, PostgreSQL, JPA/Hibernate, Redis, Apache Kafka, Spring Security, JWT, React, TypeScript, Vite, Docker, JUnit 5, Testcontainers, JaCoCo.

## Services

| Module | Port | Main job |
| --- | ---: | --- |
| `order-service` | 8081 | login/register, create orders, order state |
| `inventory-service` | 8082 | stock, row locking, reservations |
| `fulfillment-service` | 8083 | picking task, A* warehouse routing |
| `shipping-service` | 8084 | shipment creation and tracking number |
| `platform-common` | - | shared events, topic names, JWT helper |
| `frontend` | 5173 | React/TypeScript operations console |

The local PostgreSQL instance has a separate schema for every service. This is not the same as giving every service its own database server, but it keeps table ownership separate without making the laptop setup annoying.

## Main workflow

```text
POST /api/orders
      |
      | orders.created
      v
inventory-service
      |
      | inventory.reserved
      v
fulfillment-service
      |
      | picking.completed
      v
shipping-service
      |
      | shipping.created
      v
order-service -> SHIPPED
```

Kafka makes inventory, picking, and shipping independent workers. If one of them is temporarily down, the earlier service does not need to make a synchronous HTTP call to it.

## Concurrency and duplicate protection

Inventory reservation uses PostgreSQL `PESSIMISTIC_WRITE` row locks. The service locks requested SKUs in sorted order, checks all quantities, and only then decrements stock. Two reservation workers cannot both read the same old quantity and oversell it.

Order, inventory, fulfillment, and shipment entities also have Hibernate `@Version` fields. Those are used for optimistic version checks on normal state changes.

Redis handles idempotency in two places. Order creation requires an `Idempotency-Key`, and Kafka consumers keep processed event ids with a TTL. Retried HTTP requests return the same order, and duplicate Kafka deliveries do not reserve or ship twice.

## A* picking route

The fulfillment service has a 20 x 20 warehouse grid with shelf cells that cannot be crossed. `AStarRouter` finds the shortest walkable path between two points.

For a multi-item order, the baseline visits items in request order. The optimized planner repeatedly chooses the remaining item with the shortest A* path from the current position.

The fixed benchmark in `RoutingBenchmarkTest` is:

```text
sequential route: 102 cells
optimized route:   80 cells
reduction:          21.5686% (22% rounded)
```

This is backed by a deterministic test, not a number written only in the README.

## Run on macOS with IntelliJ IDEA

### 1. Install the local tools

You need:

- IntelliJ IDEA
- JDK 21
- Docker Desktop
- Maven 3.9+ if you want to run Maven from Terminal

Homebrew commands:

```bash
brew install openjdk@21
brew install maven
```

Docker Desktop is installed separately from Docker's website. Start Docker Desktop before the next step.

### 2. Open the project

Put the folder here if you are keeping it with the other career projects:

```text
~/Desktop/CareerProject/FulfillMesh
```

In IntelliJ, open the **FulfillMesh folder itself**. IntelliJ should detect the root `pom.xml` and import all Maven modules.

Check:

```text
File -> Project Structure -> Project SDK -> 21
```

If Maven did not import automatically, open the Maven tool window and click **Reload All Maven Projects**.

### 3. Start PostgreSQL, Redis, and Kafka

From the project root:

```bash
cp .env.example .env
./scripts/infra-up.sh
docker compose ps
```

Wait until PostgreSQL, Redis, and Kafka are healthy.

The default local connections are:

```text
PostgreSQL: localhost:5432
Redis:      localhost:6379
Kafka:      localhost:29092
```

### 4. Run the Spring services in IntelliJ

Run these main classes:

```text
com.fulfillmesh.order.OrderServiceApplication
com.fulfillmesh.inventory.InventoryServiceApplication
com.fulfillmesh.fulfillment.FulfillmentServiceApplication
com.fulfillmesh.shipping.ShippingServiceApplication
```

Shared IntelliJ run configurations are also under `.run/`.

The applications can start in any order after the three infrastructure containers are available.

### 5. Run a complete order

The local database automatically seeds two accounts:

```text
user:  demo@fulfillmesh.local / demo12345
admin: admin@fulfillmesh.local / admin12345
```

They are local demo credentials only.

The easiest end-to-end test is:

```bash
./scripts/demo-flow.sh
```

It logs in, prints inventory, creates the eight-item benchmark order, waits for the Kafka workflow, then prints the picking result and shipment.

You can also use `requests/FulfillMesh.http` from IntelliJ's HTTP client.

## Run everything in Docker

Instead of running Java from IntelliJ:

```bash
docker compose up -d --build
```

Then:

```bash
./scripts/demo-flow.sh
```

Stop it with:

```bash
docker compose down
```

Delete local database and Redis data with:

```bash
./scripts/reset-local.sh
```

## Tests

Normal tests:

```bash
mvn clean test
```

The project has unit tests for authentication, order idempotency, inventory reservation, shipping, A*, and route planning.

`InfrastructureSmokeTest` uses Testcontainers to start real PostgreSQL, Redis, and Kafka containers and checks all three integrations.

Coverage:

```bash
./scripts/run-coverage.sh
```

The `coverage` Maven profile has an 85% line coverage gate for the core code. Configuration/security/DTO/entity/HTTP glue is excluded so the number measures the service and algorithm logic rather than generated-style boilerplate.

## AWS

`deploy/aws/` contains an EC2 + RDS deployment layout and environment template.

I did not hard-code or automate account creation, IAM, VPC, EC2, or RDS provisioning because those steps need the owner's AWS account and can create charges. The application containers are prepared to use an RDS JDBC endpoint instead of the local PostgreSQL container. See `deploy/aws/README.md`.

## A few implementation notes

This is a portfolio-sized system, so there are a couple of deliberate limits:

- Kafka events are JSON.
- There is no payment service.
- Shipping uses a locally generated tracking number instead of a carrier API.
- The event publisher does not use a transactional outbox. For a production system where database/Kafka atomicity is critical, that would be the next thing to add.
- Redis and Kafka run on the EC2 host in the sample AWS layout. A larger deployment would normally use managed or clustered infrastructure.

More detail is in `docs/ARCHITECTURE.md` and `docs/RESUME_MAPPING.md`.

## Web operations console

The repository also includes a React + TypeScript operations console under `frontend/`. It is designed as a full control surface for the backend rather than a decorative landing page.

The console includes:

- login and registration with the existing JWT endpoints
- role-aware operator/admin navigation
- an order command center with idempotent order creation and live workflow polling
- inventory cards with add, edit, and safe delete actions for admins
- fulfillment task inspection with an interactive visualization of the stored A* pick route
- shipping lookup and tracking cards
- live health checks for all four Spring Boot services
- a Kafka/service topology view
- global search across orders, SKUs, and tracking data

The layout uses a dense commerce/operations pattern: dark navigation, a central search bar, compact tables, strong blue hierarchy, and small warm accent colors for high-value actions.

### Run the frontend

Node.js 20+ is enough. No CORS changes are required because Vite proxies each API family to the correct local Spring service.

```bash
cd frontend
npm install
npm run dev
```

Open:

```text
http://localhost:5173
```

Keep the existing local infrastructure and Spring services running:

```text
order-service       8081
inventory-service   8082
fulfillment-service 8083
shipping-service    8084
```

For the richest view, use the seeded admin account:

```text
admin@fulfillmesh.local / admin12345
```

A regular user can still create and track their own orders and read inventory, while inventory mutation and fleet-wide fulfillment/shipment lists remain admin-only.

Frontend production build:

```bash
make frontend-build
```
