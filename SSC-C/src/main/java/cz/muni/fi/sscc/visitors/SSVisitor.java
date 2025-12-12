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
import java.util.stream.Collectors;


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
            processMemberCtx(memberCtx, memberList, thisSSName);
        }

        // Save to record
        final SuperStructRepre repr = new SuperStructRepre(thisSSName, memberList);
        if (!superStructs.add(repr)) {
            throw new SSCSyntaxException("Superstruct with name " + repr.name() + " already exists", ctx, tokens);
        }

        return repr.convert();
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
                    .map(s -> Strings.getContextText(s, tokens))
                    .collect(Collectors.joining(" "));
            final String fieldData = noPrivateSpecsString +
                    (declList == null
                            ? ""
                            : " " + Strings.getContextText(declList, tokens)) +
                    ";";

            final Field field = new Field(isPrivate, fieldData);
            memberList.add(SSMember.field(field));

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

            final List<String> withoutCustom = noPrivateSpecs
                    .stream()
                    /* filter out type names */
                    .filter(declSpec -> declSpec.typeSpecifier() == null)
                    /* filter out pure and static */
                    .filter(s -> s.functionSpecifier() == null
                            || s.functionSpecifier().Pure() == null)
                    .filter(s -> s.storageClassSpecifier() == null
                            || s.storageClassSpecifier().Static() == null)
                    .map(s -> Strings.getContextText(s, tokens))
                    .toList();

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
}
