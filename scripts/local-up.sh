#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if ! command -v docker >/dev/null 2>&1; then
  echo 'Docker Desktop is required; install/start it before running this script.' >&2
  exit 1
fi
if ! docker info >/dev/null 2>&1; then
  echo 'Start Docker Desktop and wait until the engine is ready.' >&2
  exit 1
fi
# Full local stack: 3 infrastructure services, 4 Spring APIs and the React UI.
docker compose up --build -d
for attempt in $(seq 1 90); do
  if curl -fsS http://localhost:8081/actuator/health 2>/dev/null | grep -q '"UP"' \
    && curl -fsS http://localhost:8082/actuator/health 2>/dev/null | grep -q '"UP"' \
    && curl -fsS http://localhost:8083/actuator/health 2>/dev/null | grep -q '"UP"' \
    && curl -fsS http://localhost:8084/actuator/health 2>/dev/null | grep -q '"UP"'; then
    echo 'All APIs are healthy. Web UI: http://localhost:5173'
    if [[ "${SKIP_DEMO:-0}" != "1" ]]; then
      ./scripts/demo-flow.sh
    fi
    exit 0
  fi
  sleep 3
done
echo 'A service did not become healthy. Inspect: docker compose ps && docker compose logs --tail=100' >&2
exit 1
