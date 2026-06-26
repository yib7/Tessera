#!/usr/bin/env bash
# Compile and run the headless logic tests. Exits non-zero on any failure.
set -euo pipefail

cd "$(dirname "$0")"

BIN_TEST="bin-test"

echo "Compiling sources and tests..."
rm -rf "$BIN_TEST"
mkdir -p "$BIN_TEST"
# shellcheck disable=SC2046
javac -d "$BIN_TEST" $(find src test -name '*.java')

echo "Running logic tests..."
java -cp "$BIN_TEST" tessera.LogicTests
