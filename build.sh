#!/usr/bin/env bash
# Compile Tessera and package a runnable jar into dist/Tessera.jar.
# Requires a JDK 21 (or newer) with javac and jar on PATH.
set -euo pipefail

cd "$(dirname "$0")"

SRC_DIR="src"
BIN_DIR="bin"
DIST_DIR="dist"
MAIN_CLASS="tessera.Tessera"
JAR_NAME="Tessera.jar"

echo "Cleaning previous build..."
rm -rf "$BIN_DIR" "$DIST_DIR"
mkdir -p "$BIN_DIR" "$DIST_DIR"

echo "Compiling sources..."
# shellcheck disable=SC2046
javac -d "$BIN_DIR" $(find "$SRC_DIR" -name '*.java')

echo "Packaging $JAR_NAME..."
jar --create --file "$DIST_DIR/$JAR_NAME" --main-class "$MAIN_CLASS" -C "$BIN_DIR" .

echo "Done: $DIST_DIR/$JAR_NAME"
echo "Run it with:  java -jar $DIST_DIR/$JAR_NAME   (or double-click the jar)"
