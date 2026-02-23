#!/bin/bash
set -e
set -o pipefail

BASE_DIR="$(cd "$(dirname "$0")" && pwd)"
ANTLR_OUT_BASE="$BASE_DIR/SSC-C/src/main/java/antlr/"

# Grammars to process (without .g4)
GRAMMARS=(
    "SSC"
)

prepend() {
    local file="$1"
    local text="$2"

    { printf "%s\n\n" "$text"; cat "$file"; } > "$file.tmp" \
        && mv "$file.tmp" "$file"
}

for GRAMMAR in "${GRAMMARS[@]}"; do
    echo "Generating ANTLR files for $GRAMMAR.g4"

    # Lowercase dir name (ssc / sscpp)
    GRAMMAR_LC="$(echo "$GRAMMAR" | tr '[:upper:]' '[:lower:]')"
    OUT_DIR="$ANTLR_OUT_BASE/$GRAMMAR_LC"

    mkdir -p "$OUT_DIR"

    # Generate parser
    antlr "$BASE_DIR/$GRAMMAR.g4" -Dlanguage=Java -visitor

    # Cleanup ANTLR side files
    rm -f \
        "$BASE_DIR/$GRAMMAR.interp" \
        "$BASE_DIR/$GRAMMAR.tokens" \
        "$BASE_DIR/${GRAMMAR}Lexer.interp" \
        "$BASE_DIR/${GRAMMAR}Lexer.tokens"

    FILES=(
        "${GRAMMAR}BaseListener.java"
        "${GRAMMAR}BaseVisitor.java"
        "${GRAMMAR}Lexer.java"
        "${GRAMMAR}Listener.java"
        "${GRAMMAR}Parser.java"
        "${GRAMMAR}Visitor.java"
    )

    for f in "${FILES[@]}"; do
        src="$BASE_DIR/$f"
        dst="$OUT_DIR/$f"

        prepend "$src" "package antlr.$GRAMMAR_LC; /* added by refresh-grammar.sh */"
        mv "$src" "$dst"
    done
done

echo "ANTLR grammars refreshed successfully."
