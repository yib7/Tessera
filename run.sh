#!/usr/bin/env bash
# Run Tessera, building the jar first if it is missing.
set -euo pipefail

cd "$(dirname "$0")"

JAR="dist/Tessera.jar"

if [ ! -f "$JAR" ]; then
    echo "Jar not found, building first..."
    ./build.sh
fi

echo "Launching Tessera..."
java -jar "$JAR"
