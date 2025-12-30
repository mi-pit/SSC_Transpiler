package cz.mipit.sscc.ssc.compiler.visitors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.ssc.compiler.data.Field;
import cz.mipit.sscc.ssc.compiler.data.FunctionDefinition;
import cz.mipit.sscc.ssc.compiler.data.SSMember;
import cz.mipit.sscc.ssc.compiler.data.SuperStruct;
import cz.mipit.sscc.ssc.exceptions.children.SSCSyntaxException;
import cz.mipit.sscc.util.SSCCUtil;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RuleContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;


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
            final SSCParser.InitDeclaratorListContext initDeclaratorList =
                    memberCtx.declaration().initDeclaratorList();

            final List<String> type = noPrivateSpecs
                    .stream()
                    .map(s -> SSCCUtil.Text.getLiteral(s, tokens))
                    .toList();

            if (initDeclaratorList == null) {
                memberList.add(
                        SSMember.field(
                                new Field(isPrivate,
                                        /* assume last "spec" is variable name
                                         * (parser doesn't know the difference between `typedef`ed name and Identifier) */
                                        noPrivateSpecs.subList(0, noPrivateSpecs.size() - 1)
                                                .stream().map(RuleContext::getText).toList(),
                                        false,
                                        noPrivateSpecs.get(noPrivateSpecs.size() - 1).getText())
                        )
                );
                return;
            }

            if (initDeclaratorList.initDeclarator().isEmpty()) {
                throw new SSCSyntaxException(
                        "Init declarator empty `" + SSCCUtil.Text.getLiteral(memberCtx, tokens) + "`",
                        initDeclaratorList, tokens
                );
            }
            for (SSCParser.InitDeclaratorContext initDecl : initDeclaratorList.initDeclarator()) {
                if (initDecl.initializer() != null) {
                    throw new SSCSyntaxException(
                            "Cannot initialize superstruct field (must use a constructor)",
                            initDecl.initializer(), tokens
                    );
                }
                final SSCParser.DeclaratorContext declarator = initDecl.declarator();
                final boolean ptr = declarator.pointer() != null;
                if (declarator.directDeclarator().Identifier() == null) {
                    throw new SSCSyntaxException(
                            "Field has no identifier",
                            declarator.directDeclarator(), tokens
                    );
                }
                final String name = declarator.directDeclarator().Identifier().getText();

                final Field field = new Field(isPrivate, type, ptr, name);
                memberList.add(SSMember.field(field));
            }
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
                String contextText = SSCCUtil.Text.getLiteral(declSpec, tokens);
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
