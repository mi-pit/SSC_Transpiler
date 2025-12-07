#!/bin/bash

set -e
set -o pipefail

# --- Paths ---
ANTLR_JAR="SSC-C/antlr-4.13.2-complete.jar"
SRC_DIR="SSC-C/src/main/java"
TARGET_DIR="SSC-C/target/classes"
MANIFEST="SSC-C/manifest.txt"
OUTPUT_JAR="sscc.jar"

# --- 1. Refresh grammar ---
echo "Refreshing ANTLR grammar..."
./refresh-grammar.sh

# --- 2. Compile Java sources ---
echo "Compiling Java sources..."
mkdir -p "$TARGET_DIR"
find "$SRC_DIR" -name "*.java" > "SSC-C/target/sources.txt"
javac -cp "$ANTLR_JAR" -d "$TARGET_DIR" @SSC-C/target/sources.txt

# --- 3. Build fat jar ---
echo "Building fat jar..."
TMP_DIR="SSC-C/target/tmp-fatjar"
rm -rf "$TMP_DIR"
mkdir -p "$TMP_DIR"

# Unpack ANTLR into temp directory
pushd "$TMP_DIR" > /dev/null
jar xf "../../../$ANTLR_JAR"
popd > /dev/null

# Create fat jar (your classes + ANTLR)
jar cfm "$OUTPUT_JAR" "$MANIFEST" -C "$TARGET_DIR" . -C "$TMP_DIR" .

# --- Done ---
echo "Fat jar built: $OUTPUT_JAR"
