package cz.muni.fi.sscc.mine;

import cz.muni.fi.sscc.mine.data.Declaration;
import cz.muni.fi.sscc.mine.data.FunctionDefinition;
import cz.muni.fi.sscc.mine.data.SSMember;
import cz.muni.fi.sscc.util.Util;
import cz.muni.fi.sscc.antlr.SSCBaseVisitor;
import cz.muni.fi.sscc.antlr.SSCParser;
import cz.muni.fi.sscc.mine.data.SuperStructRepre;
import org.antlr.v4.runtime.CommonTokenStream;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SSVisitor extends SSCBaseVisitor<Void> {
    private final CommonTokenStream tokens;

    private final Set<SuperStructRepre> superStructs = new HashSet<>();

    public SSVisitor(CommonTokenStream tokens) {
        this.tokens = tokens;
    }

    public Set<SuperStructRepre> getSuperStructs() {
        return superStructs;
    }

    @Override
    public Void visitSuperStructSpecifier(SSCParser.SuperStructSpecifierContext ctx) {
        String name = ctx.Identifier().getText();
        List<SSMember> memberList = new ArrayList<>();

        if (ctx.superStructBody() != null) {
            for (SSCParser.SuperStructMemberContext memberCtx : ctx.superStructBody().superStructMember()) {
                if (memberCtx.declaration() != null) {
                    String text = Util.getContextText(memberCtx, tokens);
                    memberList.add(SSMember.declaration(new Declaration(text)));
                } else {
                    assert memberCtx.functionDefinition() != null;

                    FunctionDefinition functionDefinition =
                            new FunctionDefinition(name, memberCtx.functionDefinition(), tokens);

                    memberList.add(SSMember.function(functionDefinition));
                }
            }
        }

        // Save to record
        SuperStructRepre repr = new SuperStructRepre(name, memberList);
        superStructs.add(repr);

        return visitChildren(ctx);
    }
}
