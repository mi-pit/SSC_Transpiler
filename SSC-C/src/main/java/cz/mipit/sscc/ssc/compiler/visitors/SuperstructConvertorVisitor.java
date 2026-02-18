package cz.mipit.sscc.ssc.compiler.visitors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.file.InputFile;
import cz.mipit.sscc.ssc.compiler.data.ss.Field;
import cz.mipit.sscc.ssc.compiler.data.ss.FunctionDefinition;
import cz.mipit.sscc.ssc.compiler.data.ss.SSMember;
import cz.mipit.sscc.ssc.compiler.data.ss.SuperStruct;
import cz.mipit.sscc.ssc.compiler.data.var.TypedVariable;
import cz.mipit.sscc.util.SSCCUtil;
import org.antlr.v4.runtime.CommonTokenStream;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class SuperstructConvertorVisitor extends SSCConvertorVisitor {
    private final Set<SuperStruct> superStructs = new HashSet<>();

    public SuperstructConvertorVisitor(CommonTokenStream tokens, InputFile currentFile) {
        super(tokens, currentFile);
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
            throw getSSCSyntaxException("Superstruct with name " + superStruct.name() + " already exists", ctx);
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
                .anyMatch(ds -> ds.functionSpecifier() != null
                        && ds.functionSpecifier().Private() != null);

        final List<SSCParser.DeclarationSpecifierContext> noPrivateSpecs = declSpecs
                .stream()
                .filter(declSpec -> declSpec.functionSpecifier() == null
                        || declSpec.functionSpecifier().Private() == null)
                .toList();
        if (noPrivateSpecs.isEmpty()) {
            throw getSSCSyntaxException("No type specifier for superstruct member", memberCtx);
        }

        if (memberCtx.declaration() == null) {
            processMemberDeclaration(memberCtx, memberList, thisSSName, declSpecs, noPrivateSpecs, isPrivate);
            return;
        }

        processMemberFunction(memberCtx, memberList, noPrivateSpecs, isPrivate);
    }

    private void processMemberFunction(SSCParser.SuperStructMemberContext memberCtx,
                                       List<SSMember> memberList,
                                       List<SSCParser.DeclarationSpecifierContext> noPrivateSpecs,
                                       boolean isPrivate) {
        final SSCParser.InitDeclaratorListContext initDeclaratorList =
                memberCtx.declaration().initDeclaratorList();

        final List<String> type = noPrivateSpecs
                .stream()
                .map(s -> SSCCUtil.Text.getLiteral(s, tokens))
                .toList();

        if (initDeclaratorList.initDeclarator().isEmpty()) {
            throw getSSCSyntaxException(
                    "Init declarator empty `" + SSCCUtil.Text.getLiteral(memberCtx, tokens) + "`",
                    initDeclaratorList
            );
        }

        for (SSCParser.InitDeclaratorContext initDecl : initDeclaratorList.initDeclarator()) {
            if (initDecl.initializer() != null) {
                throw getSSCSyntaxException(
                        "Cannot initialize superstruct field (must use a constructor)",
                        initDecl.initializer()
                );
            }
            final SSCParser.DeclaratorContext declarator = initDecl.declarator();
            if (declarator.directDeclarator().Identifier() == null) {
                throw getSSCSyntaxException(
                        "Field has no identifier",
                        declarator.directDeclarator()
                );
            }

            final int ptrs;
            if (declarator.pointer().isEmpty()) {
                ptrs = 0;
            } else {
                assert declarator.pointer().size() == 1;
                ptrs = declarator.pointer().get(0).Star().size();
            }
            final String name = declarator.directDeclarator().Identifier().getText();

            final Field field = new Field(isPrivate, new TypedVariable(type, ptrs, name));
            memberList.add(SSMember.field(field));
        }
    }

    private void processMemberDeclaration(SSCParser.SuperStructMemberContext memberCtx, List<SSMember> memberList, String thisSSName, List<SSCParser.DeclarationSpecifierContext> declSpecs, List<SSCParser.DeclarationSpecifierContext> noPrivateSpecs, boolean isPrivate) {
        assert memberCtx.functionDefinition() != null;

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
                thisSSName,
                currentFile
        );

        memberList.add(SSMember.function(functionDefinition));
    }

    private static boolean hasPureDeclSpec(List<SSCParser.DeclarationSpecifierContext> declSpecs) {
        for (SSCParser.DeclarationSpecifierContext declSpec : declSpecs) {
            final SSCParser.FunctionSpecifierContext funcSpec = declSpec.functionSpecifier();
            if (funcSpec != null && funcSpec.Pure() != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * filter out types & {@code pure} and {@code static}
     */
    private List<String> getDeclSpecsWithoutCustom(List<SSCParser.DeclarationSpecifierContext> noPrivateSpecs) {
        final List<String> withoutCustom = new ArrayList<>();
        for (SSCParser.DeclarationSpecifierContext declSpec : noPrivateSpecs) {
            if (declSpec.typeSpecifier() == null
                    && (declSpec.functionSpecifier() == null || declSpec.functionSpecifier().Pure() == null)
                    && (declSpec.storageClassSpecifier() == null || declSpec.storageClassSpecifier().Static() == null)) {
                withoutCustom.add(SSCCUtil.Text.getLiteral(declSpec, tokens));
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
