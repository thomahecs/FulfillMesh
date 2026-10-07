# macOS + IntelliJ setup

This is the exact local setup I use for the project.

## 1. JDK 21

Check first:

```bash
java -version
javac -version
```

Both should show Java 21.

If Java 21 is missing:

```bash
brew install openjdk@21
```

Then in IntelliJ:

```text
File
-> Project Structure
-> Project
-> SDK
-> Add SDK
-> JDK
```

Choose the JDK 21 installation.

Also check:

```text
Settings
-> Build, Execution, Deployment
-> Build Tools
-> Maven
-> Runner
-> JRE = Project JDK (21)
```

## 2. Maven

IntelliJ has a bundled Maven, so Terminal Maven is optional.

For command-line Maven:

```bash
brew install maven
mvn -v
```

The output should show Java 21.

## 3. Docker Desktop

Install Docker Desktop for Mac and start it.

Check:

```bash
docker --version
docker compose version
docker info
```

`docker info` must work before Testcontainers or local infrastructure can start.

## 4. Project location

Recommended path:

```text
~/Desktop/CareerProject/FulfillMesh
```

Open the `FulfillMesh` folder in IntelliJ.

If IntelliJ asks whether to trust the project, trust it.

## 5. Maven import

Open the Maven tool window on the right side.

Click:

```text
Reload All Maven Projects
```

You should see these modules:

```text
platform-common
order-service
inventory-service
fulfillment-service
shipping-service
```

Do not open one service as a separate IntelliJ project. Open the root folder so Maven can resolve `platform-common`.

## 6. Local environment file

From the root:

```bash
cp .env.example .env
```

The `.env` file is ignored by Git.

For local development, the defaults are enough.

## 7. Start infrastructure

```bash
./scripts/infra-up.sh
```

Then:

```bash
docker compose ps
```

Expected containers:

```text
postgres
redis
kafka
```

Wait for them to become healthy.

Useful checks:

```bash
docker compose logs postgres
docker compose logs redis
docker compose logs kafka
```

## 8. Run services from IntelliJ

Run these four main classes:

```text
OrderServiceApplication
InventoryServiceApplication
FulfillmentServiceApplication
ShippingServiceApplication
```

Expected ports:

```text
8081 order
8082 inventory
8083 fulfillment
8084 shipping
```

Health checks:

```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
curl http://localhost:8084/actuator/health
```

Each should eventually return a JSON response containing `"status":"UP"`.

## 9. Run the demo

```bash
./scripts/demo-flow.sh
```

A successful run should end with:

```text
order state: SHIPPED
```

and print an A* fulfillment route plus a shipment tracking number.

## 10. Tests

Docker Desktop must be running because the integration tests use Testcontainers.

```bash
mvn clean test
```

Coverage:

```bash
./scripts/run-coverage.sh
```

## Common problems

### Port already in use

Check:

```bash
lsof -i :5432
lsof -i :6379
lsof -i :29092
lsof -i :8081
lsof -i :8082
lsof -i :8083
lsof -i :8084
```

Stop the old process or container before trying again.

### PostgreSQL starts but the service cannot connect

Check:

```bash
docker compose ps
docker compose logs postgres
```

If this is an old broken local database and there is no data you need:

```bash
./scripts/reset-local.sh
./scripts/infra-up.sh
```

### Kafka keeps reconnecting

Wait for the Kafka health check first:

```bash
docker compose ps
docker compose logs -f kafka
```

Local IntelliJ applications use `localhost:29092`. Dockerized applications use `kafka:9092`.

### IntelliJ cannot find `com.fulfillmesh.common`

The root Maven project was probably not imported. Re-open the root `FulfillMesh` directory and reload Maven.

### Testcontainers says it cannot find Docker

Open Docker Desktop, wait until it says the engine is running, then check:

```bash
docker info
```
