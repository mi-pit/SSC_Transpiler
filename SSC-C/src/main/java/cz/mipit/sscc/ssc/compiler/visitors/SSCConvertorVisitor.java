package cz.mipit.sscc.ssc.compiler.visitors;

import antlr.ssc.SSCParser;
import antlr.ssc.SSCParserBaseVisitor;
import cz.mipit.sscc.file.InputFile;
import cz.mipit.sscc.ssc.exceptions.SSCTranspilerException;
import cz.mipit.sscc.ssc.exceptions.children.SSCSyntaxException;
import cz.mipit.sscc.util.SSCCUtil;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.StringJoiner;

import static java.lang.System.lineSeparator;

/**
 * Abstract class for low-level visitor stuff.
 * <p>
 * Other visitors should extend this one.
 * </p>
 */
public abstract class SSCConvertorVisitor extends SSCParserBaseVisitor<String> {
    protected final CommonTokenStream tokens;
    protected final InputFile currentFile;

    private boolean hasErrors;

    protected SSCConvertorVisitor(CommonTokenStream tokens, InputFile currentFile) {
        this.tokens = tokens;
        this.currentFile = currentFile;

        hasErrors = false;
    }

    protected SSCSyntaxException getSSCSyntaxException(String message, ParserRuleContext ctx) {
        return new SSCSyntaxException(message, ctx, tokens, currentFile);
    }

    @Override
    protected String defaultResult() {
        return "";
    }

    @Override
    public String visitChildren(RuleNode node) {
        final StringJoiner builder = new StringJoiner(lineSeparator());
        final int n = node.getChildCount();
        for (int i = 0; i < n; i++) {
            try {
                builder.add(node.getChild(i).accept(this));
            } catch (final SSCSyntaxException e) {
                printErrorMessage(e);
                hasErrors = true;
            }
        }
        return builder.toString();
    }

    @Override
    public String visitTerminal(TerminalNode node) {
        return switch (node.getSymbol().getType()) {
            case Token.EOF -> "";

            case SSCParser.Superstruct -> "struct";

            default -> node.getText();
        };
    }

    @Override
    public String visitSscIncludeDirective(SSCParser.SscIncludeDirectiveContext ctx) {
        return SSCCUtil.Text.getLiteral(ctx, tokens) + lineSeparator();
    }

    private static void printErrorMessage(final SSCTranspilerException e) {
        System.err.println(e.getMessage());
    }

    public boolean hasNoErrors() {
        return !hasErrors;
    }
}
