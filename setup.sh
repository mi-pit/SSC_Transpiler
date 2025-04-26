antlr4 SSC.g4 -Dlanguage=Python3 -visitor || exit 1

rm ./SSC.interp ./SSC.tokens ./SSCLexer.interp ./SSCLexer.tokens
