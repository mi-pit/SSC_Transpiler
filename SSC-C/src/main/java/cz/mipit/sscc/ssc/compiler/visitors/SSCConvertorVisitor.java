package cz.mipit.sscc.ssc.compiler.visitors;

import antlr.ssc.SSCLexer;
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

    public boolean hasNoErrors() {
        return !hasErrors;
    }

    @Override
    protected String defaultResult() {
        return "";
    }


    @Override
    public String visitTerminal(TerminalNode node) {
        return switch (node.getSymbol().getType()) {
            case Token.EOF -> "";
            case SSCParser.Superstruct -> "struct";
            case SSCParser.FlagsSet -> "enum";

            case SSCParser.Then -> "?";

            default -> node.getText();
        };
    }


    private int level = 0;

    @Override
    public String visitChildren(RuleNode node) {
        final StringBuilder builder = new StringBuilder();

        final boolean isOffset = node instanceof SSCParser.FunctionDefinitionContext
                || node instanceof SSCParser.SuperStructSpecifierContext
                || node instanceof SSCParser.StructOrUnionContext
                || node instanceof SSCParser.EnumSpecifierContext
                || (node instanceof TerminalNode terminalNode
                && terminalNode.getSymbol().getType() == SSCLexer.LeftBrace);

        if (isOffset) {
            level++;
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            try {
                final var child = node.getChild(i);
                final String childText = child.accept(this);

                if (!builder.isEmpty()
                        && builder.charAt(builder.length() - 1) != '\n'
                        && !childText.equals(";")) {
                    builder.append(" ");
                }

                builder.append(childText);

                if ((node instanceof TerminalNode terminalNode
                        && terminalNode.getSymbol().getType() == SSCLexer.LeftBrace)
                        || child instanceof SSCParser.DeclarationContext
                        || child instanceof SSCParser.ExternalDeclarationContext
                        || child instanceof SSCParser.StatementContext) {
                    builder
                            .append(System.lineSeparator())
                            .append(SSCCUtil.Text.INDENT.repeat(level));
                }
            } catch (final SSCTranspilerException e) {
                hasErrors = true;
                System.err.println(e.getMessage());
            }
        }
        if (isOffset) {
            level--;
        }

        return builder.toString();
    }

    protected SSCSyntaxException getSSCSyntaxException(String message, ParserRuleContext ctx) {
        return new SSCSyntaxException(message, ctx, tokens, currentFile);
    }
}
