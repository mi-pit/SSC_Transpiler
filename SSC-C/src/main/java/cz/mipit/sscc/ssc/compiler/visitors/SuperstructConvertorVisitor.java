package cz.mipit.sscc.ssc.compiler.visitors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.ssc.compiler.data.Field;
import cz.mipit.sscc.ssc.compiler.data.FunctionDefinition;
import cz.mipit.sscc.ssc.compiler.data.SSMember;
import cz.mipit.sscc.ssc.compiler.data.SuperStruct;
import cz.mipit.sscc.ssc.exceptions.SSCSyntaxException;
import cz.mipit.sscc.util.ContextText;
import org.antlr.v4.runtime.CommonTokenStream;

import java.util.*;
import java.util.stream.Collectors;


public class SuperstructConvertorVisitor extends SSCConvertorVisitor {
    private final Set<SuperStruct> superStructs = new HashSet<>();

    public SuperstructConvertorVisitor(CommonTokenStream tokens) {
        super(tokens);
    }

    public Set<SuperStruct> getSuperStructs() {
        return superStructs;
    }

    @Override
    public String visitSuperStructSpecifier(SSCParser.SuperStructSpecifierContext ctx) {
        final String thisSSName = ctx.Identifier().getText();
        final List<SSMember> memberList = new ArrayList<>();

        if (ctx.superStructBody() == null) {
            // Usage in expression (e.g. `sizeof( superstruct )`)
            return "superstruct " + thisSSName + " ";
        }

        for (SSCParser.SuperStructMemberContext memberCtx : ctx.superStructBody().superStructMember()) {
            processMemberCtx(memberCtx, memberList, thisSSName);
        }

        // Save to record
        final SuperStruct superStruct = new SuperStruct(thisSSName, memberList);
        if (!superStructs.add(superStruct)) {
            throw new SSCSyntaxException("Superstruct with name " + superStruct.name() + " already exists", ctx, tokens);
        }

        return superStruct.convert();
    }

    private void processMemberCtx(final SSCParser.SuperStructMemberContext memberCtx,
                                  final List<SSMember> memberList,
                                  final String thisSSName) {
        final var declSpecsCtx = (memberCtx.functionDefinition() != null
                ? memberCtx.functionDefinition().declarationSpecifiers()
                : memberCtx.declaration().declarationSpecifiers());
        final var declSpecs = declSpecsCtx.declarationSpecifier();

        final boolean isPrivate = declSpecs
                .stream()
                .map(SSCParser.DeclarationSpecifierContext::Private)
                .anyMatch(Objects::nonNull);

        final List<SSCParser.DeclarationSpecifierContext> noPrivateSpecs = declSpecs
                .stream()
                .filter(spec -> spec.Private() == null)
                .toList();

        if (memberCtx.declaration() != null) {
            final var declList = memberCtx.declaration().initDeclaratorList();

            final String noPrivateSpecsString = noPrivateSpecs
                    .stream()
                    .map(s -> ContextText.getLiteral(s, tokens))
                    .collect(Collectors.joining(" "));
            final String fieldData = noPrivateSpecsString +
                    (declList == null
                            ? ""
                            : " " + ContextText.getLiteral(declList, tokens)) +
                    ";";

            final Field field = new Field(isPrivate, fieldData);
            memberList.add(SSMember.field(field));

        } else if (memberCtx.functionDefinition() != null) {
            final boolean isStatic = hasStaticDeclSpec(declSpecs);
            final boolean isPure = hasPureDeclSpec(declSpecs);

            final List<String> withoutCustom = getDeclSpecsWithoutCustom(noPrivateSpecs);

            final FunctionDefinition functionDefinition = FunctionDefinition.fromSemiParsedContext(
                    isStatic,
                    isPure,
                    isPrivate,
                    withoutCustom,
                    memberCtx.functionDefinition(),
                    tokens,
                    thisSSName
            );

            memberList.add(SSMember.function(functionDefinition));
        }
    }

    private static boolean hasPureDeclSpec(List<SSCParser.DeclarationSpecifierContext> declSpecs) {
        for (SSCParser.DeclarationSpecifierContext declSpec : declSpecs) {
            SSCParser.FunctionSpecifierContext funcSpec = declSpec.functionSpecifier();
            if (funcSpec != null) {
                if (funcSpec.Pure() != null) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * filter out type names & {@code pure} and {@code static}
     */
    private List<String> getDeclSpecsWithoutCustom(List<SSCParser.DeclarationSpecifierContext> noPrivateSpecs) {
        final List<String> withoutCustom = new ArrayList<>();
        for (SSCParser.DeclarationSpecifierContext declSpec : noPrivateSpecs) {
            if (declSpec.typeSpecifier() == null
                    && (declSpec.functionSpecifier() == null
                    || declSpec.functionSpecifier().Pure() == null)
                    && (declSpec.storageClassSpecifier() == null
                    || declSpec.storageClassSpecifier().Static() == null)) {
                String contextText = ContextText.getLiteral(declSpec, tokens);
                withoutCustom.add(contextText);
            }
        }
        return withoutCustom;
    }

    private static boolean hasStaticDeclSpec(List<SSCParser.DeclarationSpecifierContext> declSpecs) {
        for (SSCParser.DeclarationSpecifierContext declarationSpecifierContext : declSpecs) {
            SSCParser.StorageClassSpecifierContext storageSpec = declarationSpecifierContext.storageClassSpecifier();
            if (storageSpec != null) {
                if (storageSpec.Static() != null) {
                    return true;
                }
            }
        }
        return false;
    }
}
