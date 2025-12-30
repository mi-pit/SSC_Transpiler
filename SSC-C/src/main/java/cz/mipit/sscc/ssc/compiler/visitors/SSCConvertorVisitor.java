package cz.mipit.sscc.ssc.compiler.visitors;

import antlr.ssc.SSCBaseVisitor;
import antlr.ssc.SSCParser;
import cz.mipit.sscc.ssc.exceptions.SSCTranspilerException;
import cz.mipit.sscc.ssc.exceptions.children.AntlrException;
import cz.mipit.sscc.ssc.exceptions.children.SSCSyntaxException;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;

import static java.lang.System.lineSeparator;

/* todo?: SSCBaseVisitor< Pair<LineNumber, Line> > */
public abstract class SSCConvertorVisitor extends SSCBaseVisitor<String> {
    protected final CommonTokenStream tokens;
    private boolean hasErrors;

    protected SSCConvertorVisitor(CommonTokenStream tokens) {
        this.tokens = tokens;
        hasErrors = false;
    }

    @Override
    public String visitChildren(RuleNode node) {
        StringBuilder sb = new StringBuilder();
        int n = node.getChildCount();
        for (int i = 0; i < n; i++) {
            try {
                sb.append(node.getChild(i).accept(this));
            } catch (SSCSyntaxException e) {
                printErrorMessage(e);
                hasErrors = true;
            }
        }
        return sb.toString();
    }

    @Override
    public String visitTerminal(TerminalNode node) {
        if (node.getSymbol().getType() == Token.EOF) {
            return "";
        }

        return switch (node.getSymbol().getType()) {
            case SSCParser.Semi,
                 SSCParser.Directive,
                 SSCParser.LeftBrace,
                 SSCParser.RightBrace -> node.getText() + lineSeparator();

            default -> node.getText() + " ";
        };
    }

    @Override
    protected String defaultResult() {
        return "";
    }

    @Override
    public String visitErrorNode(ErrorNode node) {
        //if (opts.escalateAntlrExceptions) {
        //    hasErrors = true;
        //}

        // Don't throw! Let the user see the rest of the error nodes!
        printErrorMessage(new AntlrException(node.getSymbol(), tokens));

        return super.visitErrorNode(node);
    }

    private static void printErrorMessage(final SSCTranspilerException e) {
        System.err.println(e.getMessage());
    }

    public boolean hasNoErrors() {
        return !hasErrors;
    }
}
