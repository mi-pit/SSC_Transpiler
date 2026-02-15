package cz.mipit.sscc.ssc.compiler.visitors;

import antlr.ssc.SSCBaseVisitor;
import antlr.ssc.SSCParser;
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

public abstract class SSCConvertorVisitor extends SSCBaseVisitor<String> {
    protected final CommonTokenStream tokens;
    protected final InputFile currentFile;

    private boolean hasErrors;

    protected boolean inMacroDefinition;

    protected SSCConvertorVisitor(CommonTokenStream tokens, InputFile currentFile) {
        this.tokens = tokens;
        this.currentFile = currentFile;

        hasErrors = false;
        inMacroDefinition = false;
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

    @Override
    public String visitTerminal(TerminalNode node) {
        if (node.getSymbol().getType() == Token.EOF) {
            return "";
        }

        if (Main.TOKEN_DEBUG) {
            final Token token = node.getSymbol();
            final String symbolicName = SSCParser.VOCABULARY.getSymbolicName(token.getType());
            System.out.printf("token %s ~> %s%n", node.getText(), symbolicName);
        }

        return switch (node.getSymbol().getType()) {
            case SSCParser.Semi,
                 SSCParser.LeftBrace,
                 SSCParser.RightBrace -> node.getText() + lineSeparator();

            default -> node.getText() + " ";
        };
    }

    @Override
    public String visitStdIncludeDirective(SSCParser.StdIncludeDirectiveContext ctx) {
        return SSCCUtil.Text.getLiteral(ctx, tokens) + lineSeparator();
    }

    private static void printErrorMessage(final SSCTranspilerException e) {
        System.err.println(e.getMessage());
    }

    public boolean hasNoErrors() {
        return !hasErrors;
    }
}
