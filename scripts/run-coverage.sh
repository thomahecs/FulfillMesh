#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mvn -Pcoverage clean verify
echo
echo "JaCoCo reports are under each module's target/site/jacoco directory."
