#!/bin/bash

set -e
set -o pipefail

# --- Base directory: where this script is ---
BASE_DIR="$(cd "$(dirname "$0")" && pwd)"

# --- Paths relative to BASE_DIR ---
ANTLR_JAR="$BASE_DIR/SSC-C/antlr-4.13.2-complete.jar"
SRC_DIR="$BASE_DIR/SSC-C/src/main/java"
TARGET_DIR="$BASE_DIR/SSC-C/target/classes"
MANIFEST="$BASE_DIR/SSC-C/manifest.txt"
OUTPUT_JAR="$BASE_DIR/sscc.jar"
SOURCES_LIST="$BASE_DIR/SSC-C/target/sources.txt"
TMP_DIR="$BASE_DIR/SSC-C/target/tmp-fatjar"

# --- 1. Refresh grammar ---
echo "Refreshing ANTLR grammar..."
"$BASE_DIR/refresh-grammar.sh"

# --- 2. Compile Java sources ---
echo "Compiling Java sources..."
mkdir -p "$TARGET_DIR"
find "$SRC_DIR" -name "*.java" > "$SOURCES_LIST"
javac -cp "$ANTLR_JAR" -d "$TARGET_DIR" @"$SOURCES_LIST"

# --- 3. Build fat jar ---
echo "Building fat jar..."
rm -rf "$TMP_DIR"
mkdir -p "$TMP_DIR"

# Unpack ANTLR into temp directory
pushd "$TMP_DIR" > /dev/null
jar xf "$ANTLR_JAR"
popd > /dev/null

# Create fat jar (your classes + ANTLR)
jar cfm "$OUTPUT_JAR" "$MANIFEST" -C "$TARGET_DIR" . -C "$TMP_DIR" .

rm -r "$TMP_DIR"

# --- Done ---
echo "Fat jar built: $OUTPUT_JAR"
