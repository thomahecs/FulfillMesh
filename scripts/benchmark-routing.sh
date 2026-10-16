#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p .benchmark-classes reports
# Route engine is pure Java and can be checked independently of Spring services.
javac -d .benchmark-classes \
  platform-common/src/main/java/com/fulfillmesh/common/events/{ReservedItemPayload,PickStopPayload}.java \
  fulfillment-service/src/main/java/com/fulfillmesh/fulfillment/routing/*.java \
  benchmarks/src/main/java/com/fulfillmesh/fulfillment/routing/RoutingBenchmarkMain.java \
  benchmarks/src/main/java/com/fulfillmesh/fulfillment/routing/RoutingSelfCheckMain.java
java -cp .benchmark-classes com.fulfillmesh.fulfillment.routing.RoutingSelfCheckMain
java -Xmx768m -cp .benchmark-classes com.fulfillmesh.fulfillment.routing.RoutingBenchmarkMain reports/routing-benchmark.json
