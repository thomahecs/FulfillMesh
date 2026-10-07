#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
docker compose up -d postgres redis kafka
echo "PostgreSQL, Redis, and Kafka are starting."
echo "Wait until 'docker compose ps' shows them as healthy before running the Spring apps."
