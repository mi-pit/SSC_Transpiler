package cz.muni.fi.sscc.visitors;

import antlr.SSCParser;
import cz.muni.fi.sscc.data.Field;
import cz.muni.fi.sscc.data.FunctionDefinition;
import cz.muni.fi.sscc.data.SSMember;
import cz.muni.fi.sscc.data.SuperStructRepre;
import cz.muni.fi.sscc.exceptions.SSCSyntaxException;
import cz.muni.fi.sscc.util.Strings;
import org.antlr.v4.runtime.CommonTokenStream;

import java.util.*;


public class SSVisitor extends ConvertorVisitor {
    private final Set<SuperStructRepre> superStructs = new HashSet<>();

    public SSVisitor(CommonTokenStream tokens) {
        super(tokens);
    }

    public Set<SuperStructRepre> getSuperStructs() {
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
            final var declSpecsCtx = (memberCtx.functionDefinition() != null
                    ? memberCtx.functionDefinition().declarationSpecifiers()
                    : memberCtx.declaration().declarationSpecifiers());
            final var declSpecs = declSpecsCtx.declarationSpecifier();

            final boolean isPrivate = declSpecs
                    .stream()
                    .map(SSCParser.DeclarationSpecifierContext::Private)
                    .anyMatch(Objects::nonNull);

            final var withoutCustom = declSpecs
                    .stream()
                    .filter(declSpec -> declSpec.Private() == null)
                    .map(s -> Strings.getContextText(s, tokens))
                    .filter(s -> !"static".equals(s))
                    .filter(s -> !"pure".equals(s))
                    .toList();

            if (memberCtx.declaration() != null) {
                memberList.add(SSMember.field(
                        new Field(isPrivate, String.join(" ", withoutCustom))
                ));
            } else if (memberCtx.functionDefinition() != null) {
                final boolean isStatic = declSpecs
                        .stream()
                        .map(SSCParser.DeclarationSpecifierContext::storageClassSpecifier)
                        .filter(Objects::nonNull)
                        .anyMatch(storageSpec -> storageSpec.Static() != null);

                final boolean isPure = declSpecs
                        .stream()
                        .map(SSCParser.DeclarationSpecifierContext::functionSpecifier)
                        .filter(Objects::nonNull)
                        .anyMatch(funcSpec -> funcSpec.Pure() != null);

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

        // Save to record
        final SuperStructRepre repr = new SuperStructRepre(thisSSName, memberList);
        if (!superStructs.add(repr)) {
            throw new SSCSyntaxException("Superstruct with name " + repr.name() + " already exists", ctx, tokens);
        }

        return repr.convert();
    }
}
