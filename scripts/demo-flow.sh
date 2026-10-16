#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

BASE_ORDER="${BASE_ORDER:-http://localhost:8081}"
BASE_INVENTORY="${BASE_INVENTORY:-http://localhost:8082}"
BASE_FULFILLMENT="${BASE_FULFILLMENT:-http://localhost:8083}"
BASE_SHIPPING="${BASE_SHIPPING:-http://localhost:8084}"

TOKEN="$(
  curl -fsS -X POST "$BASE_ORDER/api/auth/login" \
    -H 'Content-Type: application/json' \
    -d '{"email":"demo@fulfillmesh.local","password":"demo12345"}' \
  | python3 -c 'import json,sys; print(json.load(sys.stdin)["token"])'
)"
echo 'Logged in.'
echo 'Inventory snapshot:'
curl -fsS "$BASE_INVENTORY/api/inventory" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool

ORDER_JSON="$(
  curl -fsS -X POST "$BASE_ORDER/api/orders" \
    -H 'Content-Type: application/json' \
    -H "Authorization: Bearer $TOKEN" \
    -H "Idempotency-Key: demo-$(date +%s)-$$" \
    -d @sample-data/order-benchmark.json
)"
echo 'Created order:'
echo "$ORDER_JSON" | python3 -m json.tool
ORDER_ID="$(echo "$ORDER_JSON" | python3 -c 'import json,sys; print(json.load(sys.stdin)["id"])')"
echo 'Waiting for Kafka workflow to finish...'
STATUS='CREATED'
for _ in $(seq 1 45); do
  STATUS="$(curl -fsS "$BASE_ORDER/api/orders/$ORDER_ID" \
    -H "Authorization: Bearer $TOKEN" \
    | python3 -c 'import json,sys; print(json.load(sys.stdin)["state"])')"
  echo "  order state: $STATUS"
  if [[ "$STATUS" == 'SHIPPED' ]]; then break; fi
  if [[ "$STATUS" == 'INVENTORY_REJECTED' ]]; then
    echo 'Order rejected due to inventory; reset the demo database before retrying.' >&2
    exit 1
  fi
  sleep 2
done
if [[ "$STATUS" != 'SHIPPED' ]]; then
  echo 'ERROR: Order did not reach SHIPPED state before timeout' >&2
  exit 1
fi

echo 'Fulfillment route:'
curl -fsS "$BASE_FULFILLMENT/api/fulfillments/orders/$ORDER_ID" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool

echo 'Shipment:'
curl -fsS "$BASE_SHIPPING/api/shipments/orders/$ORDER_ID" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
echo "PASS: order $ORDER_ID was shipped through the Kafka pipeline."
