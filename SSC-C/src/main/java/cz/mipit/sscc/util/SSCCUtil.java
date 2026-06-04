package cz.mipit.sscc.util;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;
import cz.mipit.sscc.ssc.exceptions.data.EnumeratedLine;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static cz.mipit.sscc.util.SSCCUtil.Maths.digitsOf;
import static java.lang.System.lineSeparator;

public final class SSCCUtil {
    public static TerminalNode getIdentifierFromDeclarator(SSCParser.DeclaratorContext declarator) {
        // declarator: (pointer declarationSpecifiers?)* directDeclarator
        while (declarator.directDeclarator().declarator() != null) {
            declarator = declarator.directDeclarator().declarator();
        }

        return declarator.directDeclarator().Identifier();
    }

    // vcSpecificModifer? pointer
    // vcSpecificModifer? pointer? directAbstractDeclarator gccDeclaratorExtension*
    public static String insertIdentifierIntoDeclarator(
            VisitorDispatcher dispatcher,
            SSCParser.AbstractDeclaratorContext ctx,
            String identifier
    ) {
        if (ctx == null) {
            return identifier;
        }

        // vcSpecificModifer? pointer
        if (ctx.directAbstractDeclarator() == null) {
            return dispatcher.visit(ctx) + " " + identifier;
        }

        final String vcSpecMod = ctx.vcSpecificModifer() != null
                ? dispatcher.visit(ctx.vcSpecificModifer()) + " "
                : "";
        final String pointer = ctx.pointer() != null
                ? dispatcher.visit(ctx.pointer()) + " "
                : "";

        return vcSpecMod + pointer + insertIdentifierIntoDeclarator(dispatcher, ctx.directAbstractDeclarator(), identifier);
    }

    // '(' abstractDeclarator ')' gccDeclaratorExtension*
    // '[' typeQualifierList? assignmentExpression? ']'
    // '[' 'static' typeQualifierList? assignmentExpression ']'
    // '[' typeQualifierList 'static' assignmentExpression ']'
    // '[' '*' ']'
    // '(' parameterTypeList ')' gccDeclaratorExtension*
    // directAbstractDeclarator '[' typeQualifierList? assignmentExpression? ']'
    // directAbstractDeclarator '[' 'static' typeQualifierList? assignmentExpression ']'
    // directAbstractDeclarator '[' typeQualifierList 'static' assignmentExpression ']'
    // directAbstractDeclarator '[' '*' ']'
    // directAbstractDeclarator '(' parameterTypeList ')' gccDeclaratorExtension*
    public static String insertIdentifierIntoDeclarator(
            VisitorDispatcher dispatcher,
            SSCParser.DirectAbstractDeclaratorContext ctx,
            String identifier
    ) {
        if (ctx.abstractDeclarator() != null) {
            return "( %s ) %s".formatted(
                    insertIdentifierIntoDeclarator(dispatcher, ctx.abstractDeclarator(), identifier),
                    getRestOfChildren(dispatcher, ctx, 3)
            );
        }

        if (ctx.directAbstractDeclarator() != null) {
            final String dirAbsDecl = insertIdentifierIntoDeclarator(dispatcher, ctx.directAbstractDeclarator(), identifier);
            return dirAbsDecl + getRestOfChildren(dispatcher, ctx, 1);
        }

        return identifier + " " + getRestOfChildren(dispatcher, ctx, 0);
    }


    private static String getRestOfChildren(
            final VisitorDispatcher dispatcher, final ParseTree node,
            final int offset
    ) {
        final StringBuilder buf = new StringBuilder();
        for (int i = offset; i < node.getChildCount(); i++) {
            final ParseTree child = node.getChild(i);
            buf.append(dispatcher.visit(child));
        }
        return buf.toString();
    }


    public static String createNameWithID(
            final String sscIdentifier,
            final AtomicLong id,
            final String surroundingFunctionName
    ) {
        return String.format("%s_id%019d_%s", sscIdentifier, id.getAndIncrement(), surroundingFunctionName);
    }

    public static class Text {
        public static final String INDENT = "    ";

        public static boolean charMayBePartOfIdentifier(char c) {
            return charMayBePartOfIdentifier(0, c);
        }

        public static boolean charMayBePartOfIdentifier(int indexWithinIdentifier, char c) {
            return (c >= 'a' && c <= 'z') ||
                   (c >= 'A' && c <= 'Z') ||
                   (c >= '0' && c <= '9') ||
                   c == '_';
        }

        /**
         * Returns the exact text corresponding to a ParserRuleContext.
         * Works for any context.
         */
        public static String getLiteral(ParserRuleContext ctx, CommonTokenStream tokens) {
            final int start = ctx.getStart().getTokenIndex();
            final int stop = ctx.getStop().getTokenIndex();
            return tokens.getText(Interval.of(start, stop));
        }

        /**
         * Retrieves lines before and after the given token.
         *
         * @return {@link ArrayList} of {@code before + 1 + after}-many {@link EnumeratedLine}s
         */
        public static List<EnumeratedLine> getLinesAroundToken(
                final Token token,
                final CommonTokenStream tokens,
                final int before,
                final int after
        ) {
            final String fullText = tokens.getTokenSource().getInputStream().toString();
            final String[] lines = fullText.split(lineSeparator(), -1);

            final int lineIndex = token.getLine() - 1;
            final int start = Math.max(0, lineIndex - before);
            final int end = Math.min(lines.length - 1, lineIndex + after);

            final List<EnumeratedLine> ls = new ArrayList<>();
            for (int i = start; i <= end; i++) {
                ls.add(new EnumeratedLine(i + 1, lines[i]));
            }

            return ls;
        }

        public static int getLineNumberLength(final int min, final int max) {
            return Math.max(1, Math.max(digitsOf(min), digitsOf(max)));
        }
    }

    public static class Maths {
        public static int digitsOf(int num) {
            int ndigs = 0;
            while (num > 0) {
                num /= 10;
                ++ndigs;
            }

            return ndigs;
        }

        public static boolean isPowerOfTwo(long l) {
            return (l & (l - 1)) == 0;
        }
    }


    private SSCCUtil() {
    }
}
