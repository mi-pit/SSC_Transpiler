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
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;

/**
 * Abstract class for low-level visitor stuff.
 * <p>
 * Other visitors should extend this one.
 * </p>
 */
public abstract class BaseConvertorVisitor extends SSCParserBaseVisitor<String> {
    protected final CommonTokenStream tokens;
    protected final InputFile currentFile;

    private boolean hasErrors;

    protected BaseConvertorVisitor(CommonTokenStream tokens, InputFile currentFile) {
        this.tokens = tokens;
        this.currentFile = currentFile;

        hasErrors = false;
    }

    public boolean hasNoErrors() {
        return !hasErrors;
    }

    public SSCSyntaxException getSSCSyntaxException(String message, ParserRuleContext ctx) {
        return new SSCSyntaxException(message, ctx, tokens, currentFile);
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

    /// Also formats the result
    @Override
    public String visitChildren(RuleNode node) {
        final StringBuilder builder = new StringBuilder();

        boolean isOffset = node instanceof SSCParser.CompoundStatementContext
                || node instanceof SSCParser.SuperStructSpecifierContext
                || node instanceof SSCParser.StructOrUnionContext
                || node instanceof SSCParser.EnumSpecifierContext
                || node instanceof SSCParser.IterationStatementContext
                || node instanceof SSCParser.SelectionStatementContext
                || nodeIsTerminal(node, SSCParser.LeftBrace);

        if (isOffset) {
            level++;
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            final ParseTree child = node.getChild(i);

            final boolean shouldLinebreak = child instanceof SSCParser.DeclarationContext
                    || child instanceof SSCParser.ExternalDeclarationContext
                    || child instanceof SSCParser.StatementContext;
            if (shouldLinebreak) {
                if (!builder.isEmpty() && builder.charAt(builder.length() - 1) == ' ') {
                    builder.deleteCharAt(builder.length() - 1);
                }
                builder
                        .append(System.lineSeparator())
                        .append(SSCCUtil.Text.INDENT.repeat(level));
            }

            final boolean isClosingBrace = nodeIsTerminal(child, SSCParser.RightBrace);
            if (isClosingBrace) {
                builder
                        .append(System.lineSeparator())
                        .append(SSCCUtil.Text.INDENT.repeat(Math.max(0, level - 1)));
            }

            final String childText;

            try {
                childText = visit(child);
            } catch (final SSCTranspilerException e) {
                hasErrors = true;
                System.err.println(e.getMessage());
                continue;
            }

            if (!builder.isEmpty()
                    && builder.charAt(builder.length() - 1) != '\n'
                    && !childText.equals(";")) {
                builder.append(" ");
            }

            builder.append(childText);

            if (isClosingBrace) {
                builder.append(System.lineSeparator());
            }
        }
        if (isOffset) {
            level--;
        }

        return builder.toString();
    }

    private boolean nodeIsTerminal(ParseTree ctx, int val) {
        return ctx instanceof TerminalNode t && t.getSymbol().getType() == val;
    }

    public String getLiteral(final ParserRuleContext ctx) {
        return SSCCUtil.Text.getLiteral(ctx, tokens);
    }

    public String getLiteral(final RuleNode node) {
        if (node instanceof TerminalNode t)
            return t.getText();
        if (node instanceof ParserRuleContext p)
            return SSCCUtil.Text.getLiteral(p, tokens);

        return "";
    }
}
