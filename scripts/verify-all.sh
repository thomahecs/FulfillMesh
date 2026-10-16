#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
for executable in java javac mvn npm docker; do
  if ! command -v "$executable" >/dev/null 2>&1; then
    echo "Missing prerequisite: $executable" >&2
    exit 1
  fi
done
if ! docker info >/dev/null 2>&1; then
  echo 'Docker Desktop is not running.' >&2
  exit 1
fi
java -version
mvn -B -Pcoverage clean verify
./scripts/benchmark-routing.sh
if [[ ! -f inventory-service/target/benchmark/concurrency-500.json ]]; then
  echo 'Missing concurrency benchmark report after Maven tests' >&2
  exit 1
fi
cp inventory-service/target/benchmark/concurrency-500.json reports/concurrency-500.json
python3 - <<'PYVERIFY'
import json
from pathlib import Path
sample = json.loads(Path('reports/concurrency-500.json').read_text())
assert sample['requests'] == 500
assert sample['initial_stock'] == 100
assert sample['successful_reservations'] == 100
assert sample['oversells'] == 0
p95 = sample['end_to_end_p95_ms']
print(f'End-to-end PostgreSQL reservation p95: {p95:.3f} ms')
if p95 > 150.0:
    raise SystemExit('FAIL: Measured end-to-end p95 is higher than the 150ms resume threshold')
PYVERIFY
(cd frontend && npm ci && npm run build)
echo 'All test and build commands completed successfully.'
echo 'Review reports/routing-benchmark.json and reports/concurrency-500.json.'
