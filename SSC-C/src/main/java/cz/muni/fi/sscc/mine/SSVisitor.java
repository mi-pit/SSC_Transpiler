package cz.muni.fi.sscc.mine;

import cz.muni.fi.sscc.mine.data.Declaration;
import cz.muni.fi.sscc.mine.data.FunctionDefinition;
import cz.muni.fi.sscc.mine.data.SSMember;
import cz.muni.fi.sscc.util.Util;
import cz.muni.fi.sscc.antlr.SSCParser;
import cz.muni.fi.sscc.mine.data.SuperStructRepre;
import org.antlr.v4.runtime.CommonTokenStream;

import java.util.*;


public class SSVisitor extends ConvertorVisitor {
    private final Set<SuperStructRepre> superStructs = new HashSet<>();

    public SSVisitor(CommonTokenStream tokens) {
        super(tokens);
    }

    private static String convertSuperstructToStruct(SuperStructRepre ss) {
        final StringBuilder result = new StringBuilder();

        result.append(String.format("struct %s {\n", ss.name()));
        for (SSMember s : ss.member()) {
            if (s.isDeclaration()) {
                assert s.getData().getLeft().isPresent();
                result
                        .append("\t")
                        .append(s.getData().getLeft().get().data())
                        .append("\n");
            }
        }
        result.append("};\n");

        for (SSMember s : ss.member()) {
            if (s.isFunctionDefinition()) {
                assert s.getData().getRight().isPresent();
                result.append(s.getData().getRight().get().getText().stripLeading());
            }
        }

        return result.toString();
    }

    public Set<SuperStructRepre> getSuperStructs() {
        return superStructs;
    }

    @Override
    public String visitSuperStructSpecifier(SSCParser.SuperStructSpecifierContext ctx) {
        final String name = ctx.Identifier().getText();
        final List<SSMember> memberList = new ArrayList<>();

        if (ctx.superStructBody() == null) {
            // Usage in expression (e.g. `sizeof( superstruct )`)
            return "struct " + name + " ";
        }

        for (SSCParser.SuperStructMemberContext memberCtx : ctx.superStructBody().superStructMember()) {
            if (memberCtx.declaration() != null) {
                final String text = Util.getContextText(memberCtx, tokens);
                memberList.add(SSMember.declaration(new Declaration(text)));
            } else if (memberCtx.functionDefinition() != null) {
                FunctionDefinition functionDefinition =
                        new FunctionDefinition(name, memberCtx.functionDefinition(), tokens);

                memberList.add(SSMember.function(functionDefinition));
            }
        }

        // Save to record
        SuperStructRepre repr = new SuperStructRepre(name, memberList);
        superStructs.add(repr);

        return convertSuperstructToStruct(repr);
    }


    @Override
    public String visitDeclaration(SSCParser.DeclarationContext ctx) {
        var sss = ctx
                .declarationSpecifiers()
                .declarationSpecifier()
                .stream()
                .map(SSCParser.DeclarationSpecifierContext::typeSpecifier)
                .map(SSCParser.TypeSpecifierContext::superStructSpecifier)
                .filter(Objects::nonNull)
                .map(ss -> {
                    String str = visit(ss);
                    str = str.substring(0, str.length() - 1);
                    return str;
                });

        return super.visitDeclaration(ctx);
    }
}
