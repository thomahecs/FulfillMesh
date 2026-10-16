# FulfillMesh

FulfillMesh is a distributed order-fulfillment platform built with Spring Boot services and a React operations console. It follows a typical warehouse workflow: an order is placed, inventory is reserved, a picking route is planned, and a shipment is created. Kafka carries events between services, so the workflow does not depend on a chain of synchronous service-to-service calls.

The whole project can run on a laptop. PostgreSQL, Redis, and Kafka run in Docker; the four Spring Boot services can run either in IntelliJ IDEA or in Docker.

## Stack

Java 21, Spring Boot, PostgreSQL, JPA/Hibernate, Redis, Apache Kafka, Spring Security, JWT, React, TypeScript, Vite, Docker, JUnit 5, Testcontainers, JaCoCo.

## Services

| Module | Port | Responsibility |
| --- | ---: | --- |
| `order-service` | 8081 | Login/registration, order creation, order state |
| `inventory-service` | 8082 | Stock management, row locking, reservations |
| `fulfillment-service` | 8083 | Picking tasks, A* warehouse routing |
| `shipping-service` | 8084 | Shipment creation and tracking numbers |
| `platform-common` | - | Shared events, topic names, JWT helper |
| `frontend` | 5173 | React/TypeScript operations console |

Each service owns a separate schema in the local PostgreSQL instance. This is not the same as running a dedicated database server for every service, but it separates table ownership without adding much overhead to local development.

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

Kafka lets inventory, picking, and shipping process work independently. When a downstream service is temporarily unavailable, the previous service does not have to wait on a synchronous HTTP request.

## Concurrency and duplicate protection

Inventory reservations use PostgreSQL `PESSIMISTIC_WRITE` row locks. The service locks the requested SKUs in a consistent sorted order, checks the quantities, and only then decrements stock. This prevents concurrent reservation workers from using the same available units twice.

Order, inventory, fulfillment, and shipment entities also have Hibernate `@Version` fields for optimistic version checks during state updates.

Redis provides idempotency for both HTTP requests and Kafka consumers. Creating an order requires an `Idempotency-Key`; consumers store processed event IDs with a TTL. Retrying an HTTP request returns the existing order, while duplicate Kafka events do not trigger a second reservation or shipment.

## A* picking route

A* finds obstacle-aware paths through a 20 x 20 warehouse grid. For orders with up to 10 picking stops, the optimizer uses Held–Karp to find the best visit order. Larger orders start with nearest-neighbor and sequential routes, apply 2-opt improvements, and keep the shorter candidate.

The original deterministic eight-item test compares 102 grid steps for sequential picking with 80 for the optimized route.

A separate seeded benchmark generates 10,000 orders and can be rerun with:

```bash
./scripts/benchmark-routing.sh
cat reports/routing-benchmark.json
```

Across 10,000 generated orders containing 18–22 items each, total simulated walking distance was 12.4969% lower than nearest-neighbor and 41.1356% lower than sequential aisle-sorted picking. The benchmark setup and its limitations are documented in `docs/ROUTING_BENCHMARK.md`.

## Fastest macOS launch: one Docker command

Start Docker Desktop and, if possible, allocate 8–10 GB of RAM to Docker. From the repository root, run:

```bash
./scripts/local-up.sh
```

The script builds and starts PostgreSQL, Redis, Kafka, all four Spring Boot services, and the React frontend. Once the APIs are healthy, it also runs an end-to-end example order.

Open **http://localhost:5173** when the script finishes. Use `demo@fulfillmesh.local` / `demo12345` to log in. This is a local demo account.

To run the order-to-shipping example again:

```bash
./scripts/demo-flow.sh
```

For Java unit tests, PostgreSQL/Redis/Kafka integration tests, the JaCoCo coverage check, the 500-request concurrency check, and the frontend build, install JDK 21 and Maven 3.9+, then run:

```bash
./scripts/verify-all.sh
```

`docs/VERIFICATION.md` explains what each check covers and what it does not.

## Run on macOS with IntelliJ IDEA

### 1. Install the local tools

You need:

- IntelliJ IDEA
- JDK 21
- Docker Desktop
- Maven 3.9+ to run Maven commands from Terminal

Install Java and Maven with Homebrew:

```bash
brew install openjdk@21
brew install maven
```

Install Docker Desktop separately from Docker's website and start it before bringing up the infrastructure.

### 2. Open the project

If you keep your career projects on the Desktop, the folder can be located at:

```text
~/Desktop/CareerProject/FulfillMesh
```

Open the **FulfillMesh** folder in IntelliJ. It should detect the root `pom.xml` and import the Maven modules.

Check the project SDK:

```text
File -> Project Structure -> Project SDK -> 21
```

If the modules do not appear, open the Maven tool window and select **Reload All Maven Projects**.

### 3. Start PostgreSQL, Redis, and Kafka

From the project root:

```bash
cp .env.example .env
./scripts/infra-up.sh
docker compose ps
```

Wait for PostgreSQL, Redis, and Kafka to become healthy.

Default local endpoints:

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

Shared IntelliJ run configurations are available in `.run/`. Once the infrastructure containers are ready, the Spring applications can start in any order.

### 5. Run a complete order

The local database seeds two accounts:

```text
user:  demo@fulfillmesh.local / demo12345
admin: admin@fulfillmesh.local / admin12345
```

These credentials are for local demos only.

Run:

```bash
./scripts/demo-flow.sh
```

The script logs in, displays inventory, creates the eight-item benchmark order, waits for the Kafka workflow, and prints the picking result and shipment. You can also send requests from IntelliJ using `requests/FulfillMesh.http`.

## Run everything in Docker

To run the Java services in Docker instead of IntelliJ:

```bash
docker compose up -d --build
```

Then run the sample workflow:

```bash
./scripts/demo-flow.sh
```

Stop the containers with:

```bash
docker compose down
```

To delete local PostgreSQL and Redis data:

```bash
./scripts/reset-local.sh
```

## Tests

Run the standard test suite:

```bash
mvn clean test
```

Unit tests cover authentication, order idempotency, inventory reservations, shipping, A*, and route planning. The repository also includes a PostgreSQL load test with 500 reservation attempts against 100 units of stock.

The load test records p95 latency; it does not guarantee a particular result across machines. Run the benchmark and check its report before quoting a latency figure:

```bash
./scripts/benchmark-concurrency.sh
```

`InfrastructureSmokeTest` uses Testcontainers with real PostgreSQL, Redis, and Kafka containers to check those integrations.

To run the coverage check:

```bash
./scripts/run-coverage.sh
```

The Maven `coverage` profile enforces an 85% line-coverage threshold for core code. Rerun it after code changes. Configuration, security, DTO, entity, and HTTP glue classes are excluded so the reported figure focuses on service and algorithm logic rather than boilerplate.

## AWS

`deploy/aws/` contains an EC2 + RDS deployment layout and an environment template.

AWS account setup, IAM, VPC, EC2, and RDS provisioning are not automated or hard-coded; those steps require the owner's AWS account and may incur charges. The application containers can use an RDS JDBC endpoint in place of the local PostgreSQL container. Deployment details are in `deploy/aws/README.md`.

## A few implementation notes

This is a portfolio-sized project, and some parts are intentionally limited:

- Kafka events use JSON.
- There is no payment service.
- Shipment tracking numbers are generated locally; there is no carrier API integration.
- The event publisher does not use a transactional outbox. A production deployment requiring database/Kafka atomicity would need one.
- The example AWS layout runs Redis and Kafka on the EC2 host. A larger deployment would typically use managed or clustered infrastructure.

See `docs/ARCHITECTURE.md` and `docs/RESUME_MAPPING.md` for more details.

## Web operations console

The React/TypeScript console lives in `frontend/` and provides an interface for working with the backend, rather than just a project landing page.

It includes:

- Login and registration through the existing JWT endpoints
- Role-aware operator/admin navigation
- Order creation with idempotency keys and live workflow polling
- Inventory cards with add, edit, and safe delete actions for admins
- Fulfillment task details and an interactive view of the stored A* picking route
- Shipment lookup and tracking cards
- Health checks for all four Spring Boot services
- A Kafka/service topology view
- Search across orders, SKUs, and tracking data

The interface uses dark navigation, a central search bar, compact tables, blue visual hierarchy, and warm accents for primary actions.

### Run the frontend

Use Node.js 20 or later. Vite proxies each API group to its local Spring service, so no additional CORS changes are needed.

```bash
cd frontend
npm install
npm run dev
```

Open:

```text
http://localhost:5173
```

Keep the infrastructure and Spring services running on these ports:

```text
order-service       8081
inventory-service   8082
fulfillment-service 8083
shipping-service    8084
```

To access the admin controls, use:

```text
admin@fulfillmesh.local / admin12345
```

Regular users can create and track their own orders and view inventory. Inventory changes and system-wide fulfillment/shipment lists are restricted to admins.

Build the frontend for production with:

```bash
make frontend-build
```
