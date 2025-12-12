package cz.muni.fi.sscc.visitors;

import antlr.SSCBaseVisitor;
import antlr.SSCParser;
import cz.muni.fi.sscc.exceptions.AntlrException;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;

public abstract class ConvertorVisitor extends SSCBaseVisitor<String> {
    protected final CommonTokenStream tokens;
    private boolean hasErrors = false;

    protected ConvertorVisitor(CommonTokenStream tokens) {
        this.tokens = tokens;
    }

    @Override
    public String visitChildren(RuleNode node) {
        StringBuilder sb = new StringBuilder();
        int n = node.getChildCount();
        for (int i = 0; i < n; i++) {
            sb.append(node.getChild(i).accept(this));
        }
        return sb.toString();
    }

    @Override
    public String visitExternalDeclaration(SSCParser.ExternalDeclarationContext ctx) {
        if (ctx.functionDefinition() != null) {
            return super.visitExternalDeclaration(ctx) + "\n";
        }
        return super.visitExternalDeclaration(ctx);
    }

    @Override
    public String visitTerminal(TerminalNode node) {
        if (node.getSymbol().getType() == Token.EOF) {
            return "";  // ignore <EOF>
        }

        return switch (node.getSymbol().getType()) {
            case SSCParser.LeftBrace -> "{\n";
            case SSCParser.RightBrace -> "}\n";
            case SSCParser.Semi -> ";\n";

            default -> node.getText() + " ";
        };
    }

    @Override
    protected String defaultResult() {
        return "";
    }

    @Override
    public String visitErrorNode(ErrorNode node) {
        hasErrors = true;
        // Don't throw! Let the user see the rest of the error nodes!
        System.err.println(new AntlrException(node.getSymbol(), tokens).getMessage());
        return super.visitErrorNode(node);
    }

    public boolean hasNoErrors() {
        return !hasErrors;
    }
}
