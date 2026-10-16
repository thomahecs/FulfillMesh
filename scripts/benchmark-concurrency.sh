#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if ! command -v mvn >/dev/null 2>&1; then
  echo 'Maven 3.9+ is required for the PostgreSQL/Testcontainers benchmark.' >&2
  exit 1
fi
if ! docker info >/dev/null 2>&1; then
  echo 'Start Docker Desktop to run PostgreSQL Testcontainers.' >&2
  exit 1
fi
# Pass REQUIRE_P95=1 only when enforcing the machine-dependent 150ms SLO.
strict=false
if [[ "${REQUIRE_P95:-0}" == "1" ]]; then strict=true; fi
set +e
mvn -B -pl inventory-service -am \
  -Dtest=ReservationLoadTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dfulfillmesh.require.p95="$strict" test
status=$?
set -e
source_report=inventory-service/target/benchmark/concurrency-500.json
if [[ -f "$source_report" ]]; then
  mkdir -p reports
  cp "$source_report" reports/concurrency-500.json
  echo "Benchmark report: reports/concurrency-500.json"
fi
exit "$status"
