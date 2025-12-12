#!/bin/bash

set -e

BASE_DIR="$(cd "$(dirname "$0")" && pwd)"

antlr "$BASE_DIR/SSC.g4" -Dlanguage=Java -visitor

rm "$BASE_DIR/SSC.interp"
rm "$BASE_DIR/SSC.tokens"
rm "$BASE_DIR/SSCLexer.interp"
rm "$BASE_DIR/SSCLexer.tokens"


prepend() {
    local file="$1"
    local text="$2"

    { printf "%s\n\n" "$text"; cat "$file"; } > "$file.tmp" \
        && mv "$file.tmp" "$file"
}

FILES=(
    "SSCBaseListener.java"
    "SSCBaseVisitor.java"
    "SSCLexer.java"
    "SSCListener.java"
    "SSCParser.java"
    "SSCVisitor.java"
)

for f in "${FILES[@]}"; do
    prepend "$BASE_DIR/$f" "package antlr; /* added to project in \`refresh-grammar.sh\` */"
    mv "$BASE_DIR/$f" "$BASE_DIR/SSC-C/src/main/java/antlr/$f"
    # echo Moved "$BASE_DIR/$f" "$BASE_DIR/SSC-C/src/main/java/antlr/$f"
done
