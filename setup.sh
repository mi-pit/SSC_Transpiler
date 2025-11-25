antlr SSC.g4 -Dlanguage=Java -visitor || exit 1

rm ./SSC.interp ./SSC.tokens ./SSCLexer.interp ./SSCLexer.tokens
