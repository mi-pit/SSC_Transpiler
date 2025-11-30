package cz.muni.fi.sscc.mine;

import cz.muni.fi.sscc.antlr.SSCBaseVisitor;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;

public abstract class ConvertorVisitor extends SSCBaseVisitor<String> {
    protected final CommonTokenStream tokens;

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
    public String visitTerminal(TerminalNode node) {
        if (node.getSymbol().getType() == Token.EOF) {
            return "";  // ignore <EOF>
        }

        return node.getText() + "\n";
    }

    @Override
    protected String defaultResult() {
        return "";
    }
}
