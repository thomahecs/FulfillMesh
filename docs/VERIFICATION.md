# FulfillMesh verification and remaining checks

## Delivered changes

- Optimized routing now compares against a real A*-distance nearest-neighbor
  baseline. It uses exact Held-Karp ordering for up to 10 picks and a bounded
  2-opt search for longer orders.
- `scripts/benchmark-routing.sh` executes a seeded 10,000-order experiment and
  saves machine-readable totals and percentages.
- `ReservationLoadTest` uses a **real PostgreSQL 16 Testcontainer**, sends 500
  reservation attempts for 100 units, and asserts 100 successes, 400 rejects,
  and zero remaining/negative stock. It records service and wall-clock p95.
- `scripts/benchmark-concurrency.sh` saves the results to
  `reports/concurrency-500.json`. Set `REQUIRE_P95=1` to fail if end-to-end
  p95 is over **150 ms** on the measured machine.
- The Docker Compose full-stack profile now builds and serves React through
  Nginx at `http://localhost:5173`; 4 Java services are available on 8081–8084.
- Corrected PostgreSQL Testcontainers JDBC URL query separators.

## Verification completed in the editing environment

- Java 21 compilation of all main Java sources with the runtime dependency
  jars bundled in the uploaded project's original executable JARs.
- `tsc -b` passed using the frontend dependencies in the uploaded project.
- `scripts/benchmark-routing.sh` compiled and completed all 10,000 cases;
  see its committed JSON report.
- Parsed `docker-compose.yml` successfully as YAML.

## Not certified by the editing environment

**The editing environment has no Maven or Docker daemon, and does not have the
Linux binaries for Vite/Rollup from the user's macOS node_modules.** As a
result, the current edits have NOT passed `mvn clean verify`, the full Nginx
production build, real PostgreSQL 500-request test, live Kafka/Redis workflow,
or the JaCoCo 85% coverage gate here. The `tsc -b` check is not a substitute
for building Vite in Docker.

To certify on a real Docker + Maven environment (the first command strictly
enforces the 85% JaCoCo gate and measured end-to-end 150 ms p95 threshold;
the second starts the full stack and automatically runs an order to shipping):

```bash
./scripts/verify-all.sh
./scripts/local-up.sh
```

**Do not claim measured p95 <= 150 ms until `reports/concurrency-500.json`
actually shows it; a 500-request load test is hardware/configuration sensitive.**
If the number is higher, keep the report as evidence and investigate the
PostgreSQL transaction, connection pool, client concurrency and host load.

The uploaded code includes historical Surefire and JaCoCo reports from the
pre-edit build, but these are **not** evidence that this edited version passed
all tests or has >=85% core coverage.

## Known production limitations

- This is a single-node portfolio deployment with no Kafka transactional
  outbox. Kafka producer acknowledgements, DB commit and Redis idempotency
  markers are not atomic; a crash at the boundary can leave an order needing
  manual repair. This has not been claimed as exactly-once semantics.
- Inventory uses a single primary PostgreSQL instance; throughput, p95, and
  failure behavior will vary on other hardware and deployment sizes.
- AWS EC2/RDS templates are included but were not deployed or tested in this
  handoff. No cloud billable resources were provisioned.
