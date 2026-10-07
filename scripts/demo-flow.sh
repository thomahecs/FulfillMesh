#!/usr/bin/env bash
set -euo pipefail

BASE_ORDER="${BASE_ORDER:-http://localhost:8081}"
BASE_INVENTORY="${BASE_INVENTORY:-http://localhost:8082}"
BASE_FULFILLMENT="${BASE_FULFILLMENT:-http://localhost:8083}"
BASE_SHIPPING="${BASE_SHIPPING:-http://localhost:8084}"

TOKEN="$(
  curl -sS -X POST "$BASE_ORDER/api/auth/login" \
    -H 'Content-Type: application/json' \
    -d '{"email":"demo@fulfillmesh.local","password":"demo12345"}' \
  | python3 -c 'import json,sys; print(json.load(sys.stdin)["token"])'
)"

echo "Logged in."
echo "Inventory snapshot:"
curl -sS "$BASE_INVENTORY/api/inventory" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool

ORDER_JSON="$(
  curl -sS -X POST "$BASE_ORDER/api/orders" \
    -H 'Content-Type: application/json' \
    -H "Authorization: Bearer $TOKEN" \
    -H "Idempotency-Key: demo-$(date +%s)" \
    -d @sample-data/order-benchmark.json
)"

echo
echo "Created order:"
echo "$ORDER_JSON" | python3 -m json.tool

ORDER_ID="$(echo "$ORDER_JSON" | python3 -c 'import json,sys; print(json.load(sys.stdin)["id"])')"
echo
echo "Waiting for Kafka workflow to finish..."

for _ in $(seq 1 30); do
  STATUS="$(
    curl -sS "$BASE_ORDER/api/orders/$ORDER_ID" \
      -H "Authorization: Bearer $TOKEN" \
    | python3 -c 'import json,sys; print(json.load(sys.stdin)["state"])'
  )"
  echo "  order state: $STATUS"
  if [ "$STATUS" = "SHIPPED" ]; then
    break
  fi
  sleep 1
done

echo
echo "Fulfillment route:"
curl -sS "$BASE_FULFILLMENT/api/fulfillments/orders/$ORDER_ID" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool

echo
echo "Shipment:"
curl -sS "$BASE_SHIPPING/api/shipments/orders/$ORDER_ID" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
