#!/bin/bash
set -e
set -o pipefail

BASE_DIR="$(cd "$(dirname "$0")" && pwd)"
ANTLR_OUT_BASE="$BASE_DIR/grammar"

echo "Generating ANTLR files for SSC"

mkdir -p "$ANTLR_OUT_BASE"

antlr \
      "$BASE_DIR/grammar/SSCLexer.g4"  \
      "$BASE_DIR/grammar/SSCParser.g4" \
      -Dlanguage=Java                  \
      -visitor                         \
      -o "$ANTLR_OUT_BASE"             \
      -package "antlr.ssc"

mv "$BASE_DIR"/grammar/*.java "$BASE_DIR"/SSC-C/src/main/java/antlr/ssc

echo "ANTLR grammars refreshed successfully."
