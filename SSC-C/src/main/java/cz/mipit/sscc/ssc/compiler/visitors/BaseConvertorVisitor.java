package cz.mipit.sscc.ssc.compiler.visitors;

import antlr.ssc.SSCParser;
import antlr.ssc.SSCParserBaseVisitor;
import cz.mipit.sscc.Logger;
import cz.mipit.sscc.Main;
import cz.mipit.sscc.file.File;
import cz.mipit.sscc.ssc.exceptions.SSCTranspilerException;
import cz.mipit.sscc.ssc.exceptions.children.SSCCallbackException;
import cz.mipit.sscc.ssc.exceptions.children.SSCLanguageException;
import cz.mipit.sscc.util.SSCCUtil;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.List;

/**
 * Abstract class for low-level visitor stuff.
 * <p>
 * Other visitors should extend this one.
 * </p>
 */
public abstract class BaseConvertorVisitor extends SSCParserBaseVisitor<String> {
    protected final CommonTokenStream tokens;
    protected final File currentFile;

    protected boolean hasErrors;

    protected BaseConvertorVisitor(CommonTokenStream tokens, File currentFile) {
        this.tokens = tokens;
        this.currentFile = currentFile;

        hasErrors = false;
    }

    public boolean hasNoErrors() {
        return !hasErrors;
    }

    protected void processTranspilerException(SSCTranspilerException e) {
        Main.logger.printException(e);
        hasErrors = true;
    }


    abstract public void debugPrintDump();

    abstract public String visit(ParseTree node);


    protected final String visitDefault(ParseTree node) {
        return super.visit(node);
    }

    @Override
    protected String defaultResult() {
        return "";
    }

    @Override
    public String visitTerminal(TerminalNode node) {
        if (node.getSymbol().getType() == Token.EOF) {
            return "";
        }

        return node.getText();
    }


    private int level = 0;

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
                processTranspilerException(e);
                continue;
            }

            if (!builder.isEmpty()
                && builder.charAt(builder.length() - 1) != '\n'
                && !childText.equals(";")) {
                builder.append(" ");
            }

            builder.append(childText);

            if (isClosingBrace || child instanceof SSCParser.TemplateHeaderContext) {
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

    /// Returns the literal input, including whitespace and SSC-only keywords. Only meant for debugging/exceptions
    public String getLiteral(final ParseTree node) {
        if (node instanceof TerminalNode t)
            return t.getText();
        if (node instanceof ParserRuleContext p)
            return SSCCUtil.Text.getLiteral(p, tokens);

        return "";
    }

    public SSCTranspilerException getSSCLanguageException(String message, ParseTree ctx) {
        if (ctx instanceof ParserRuleContext prc)
            return new SSCLanguageException(message, prc, tokens, currentFile);
        if (ctx instanceof TerminalNode t)
            return new SSCLanguageException(message, t, tokens, currentFile);

        throw new IllegalStateException("Invalid parse tree: " + ctx.getClass().getName());
    }

    public SSCTranspilerException getSSCCallbackException(
            String message, ParseTree curr, ParseTree old
    ) {
        return new SSCCallbackException(message, List.of(curr, old), tokens, currentFile);
    }


    public void warn(String message, ParseTree node) {
        Logger.warn(
                SSCTranspilerException.createMessage(
                        SSCTranspilerException.Type.Warning,
                        currentFile,
                        SSCTranspilerException.getErrorMessages(
                                message,
                                List.of(node),
                                tokens
                        ))
        );
    }

    // ('<<' | '>' '>')
    @Override
    final public String visitShiftOperator(SSCParser.ShiftOperatorContext ctx) {
        if (ctx.Greater().isEmpty()) {
            return this.visitChildren(ctx);
        }

        final String validString = ">>";

        final List<TerminalNode> rightShiftTokens = ctx.Greater();
        final String literal = this.getLiteral(ctx);

        if (rightShiftTokens.size() != 2 || !validString.equals(literal)) {
            throw this.getSSCLanguageException(
                    "Invalid operator: '" + literal + "'",
                    ctx
            );
        }

        return validString;
    }
}
