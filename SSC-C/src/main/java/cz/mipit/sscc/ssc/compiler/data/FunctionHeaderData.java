package cz.mipit.sscc.ssc.compiler.data;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.ssc.compiler.data.ss.SuperStruct;

import java.util.List;

public record FunctionHeaderData(
        boolean isStatic,
        boolean isPure,
        List<String> cDeclarationSpecifiers,
        SSCParser.DeclaratorContext declarator,
        String unqualifiedName,
        SuperStruct superStruct
) {
}
