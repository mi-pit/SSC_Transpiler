antlr SSC.g4 -Dlanguage=Java -visitor || exit 1

rm ./SSC.interp ./SSC.tokens ./SSCLexer.interp ./SSCLexer.tokens


prepend() {
    local file="$1"
    local text="$2"

    { printf "%s\n" "$text"; cat "$file"; } > "$file.tmp" \
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
    prepend "$f" "package cz.muni.fi.sscc.antlr;"
    mv "$f" "SSC-C/src/main/java/cz/muni/fi/sscc/antlr/$f"
done
