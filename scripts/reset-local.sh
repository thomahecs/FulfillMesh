#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
docker compose down -v --remove-orphans
echo "Local PostgreSQL and Redis data were removed."
