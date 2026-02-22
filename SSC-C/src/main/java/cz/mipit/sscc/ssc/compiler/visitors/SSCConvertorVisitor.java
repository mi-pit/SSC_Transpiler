package cz.mipit.sscc.ssc.compiler.visitors;

import antlr.ssc.SSCParser;
import antlr.ssc.SSCParserBaseVisitor;
import cz.mipit.sscc.Logger;
import cz.mipit.sscc.Main;
import cz.mipit.sscc.file.InputFile;
import cz.mipit.sscc.ssc.exceptions.SSCTranspilerException;
import cz.mipit.sscc.ssc.exceptions.children.SSCSyntaxException;
import cz.mipit.sscc.util.SSCCUtil;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;

import static java.lang.System.lineSeparator;

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
        final StringBuilder sb = new StringBuilder();
        final int n = node.getChildCount();
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

    private int level = 0;

    @Override
    public String visitTerminal(TerminalNode node) {
        if (node.getSymbol().getType() == Token.EOF) {
            return "";
        }

        if (Main.TOKEN_DEBUG) {
            final Token token = node.getSymbol();
            final String symbolicName = SSCParser.VOCABULARY.getSymbolicName(token.getType());
            Logger.info("token %s ~> %s", node.getText(), symbolicName);
        }

        final String text = node.getText();
        final String whitespace = switch (node.getSymbol().getType()) {
            case SSCParser.Semi -> lineSeparator() + "    ".repeat(level);
            case SSCParser.LeftBrace -> lineSeparator() + "    ".repeat(++level);
            case SSCParser.RightBrace -> lineSeparator() + "    ".repeat(level > 0 ? --level : level);

            default -> " ";
        };

        return text + whitespace;
    }

    @Override
    public String visitSscIncludeDirective(SSCParser.SscIncludeDirectiveContext ctx) {
        return SSCCUtil.Text.getLiteral(ctx, tokens) + lineSeparator();
    }

    protected static void printErrorMessage(final SSCTranspilerException e) {
        System.err.println(e.getMessage());
    }

    public boolean hasNoErrors() {
        return !hasErrors;
    }
}
