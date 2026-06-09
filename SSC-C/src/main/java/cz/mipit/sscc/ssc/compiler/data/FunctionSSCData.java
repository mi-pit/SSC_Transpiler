package cz.mipit.sscc.ssc.compiler.data;

import antlr.ssc.SSCParser;

import java.util.List;
import java.util.Objects;

public record FunctionSSCData(boolean isMeta, boolean isPure, boolean isPrivate) {
    public static FunctionSSCData fromDeclarationSpecifiers(List<SSCParser.DeclarationSpecifierContext> declSpecsLs) {
        final List<SSCParser.SuperstructMemberDeclarationSpecifierContext> declSpecs =
                declSpecsLs.stream()
                        .map(SSCParser.DeclarationSpecifierContext::superstructMemberDeclarationSpecifier)
                        .filter(Objects::nonNull)
                        .toList();

        boolean isMeta, isPure, isPrivate = isMeta = isPure = false;
        for (SSCParser.SuperstructMemberDeclarationSpecifierContext declSpec : declSpecs) {
            if (declSpec.StaticFunction() != null) {
                isMeta = true;
            } else if (declSpec.Private() != null) {
                isPrivate = true;
            } else if (declSpec.Pure() != null) {
                isPure = true;
            }
        }

        return new FunctionSSCData(isMeta, isPure, isPrivate);
    }
}
