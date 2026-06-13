package cz.mipit.sscc.util;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

import static cz.mipit.sscc.util.SSCCUtil.Maths.digitsOf;

public final class SSCCUtil {
    public static TerminalNode getIdentifierFromDeclarator(SSCParser.DeclaratorContext declarator) {
        // declarator: (pointer declarationSpecifiers?)* directDeclarator
        while (declarator.directDeclarator().declarator() != null) {
            declarator = declarator.directDeclarator().declarator();
        }

        return declarator.directDeclarator().Identifier();
    }

    public static List<ParseTree> allMatching(ParseTree node, Predicate<ParseTree> matcher) {
        if (node == null) {
            return Collections.emptyList();
        }

        if (matcher.test(node)) {
            return List.of(node);
        }

        if (node instanceof TerminalNode) {
            return List.of();
        }

        final List<ParseTree> result = new ArrayList<>();
        for (int i = 0; i < node.getChildCount(); i++) {
            result.addAll(
                    allMatching(node.getChild(i), matcher)
            );
        }
        return result;
    }


    // declarator: (pointer declarationSpecifiers?)* directDeclarator
    public static String getDeclaratorForLambdaPassover(
            final VisitorDispatcher dispatcher,
            final SSCParser.DeclaratorContext declarator,
            final String newIdentifier,
            final boolean removeConstFromRightmostPointer
    ) {
        final StringJoiner joiner = new StringJoiner(" ");

        final SSCParser.PointerContext rightmostPointer = !removeConstFromRightmostPointer || declarator.pointer().isEmpty()
                ? null
                : declarator.pointer().getLast();

        for (final ParseTree child : declarator.children) {
            if (child instanceof SSCParser.DirectDeclaratorContext directDeclarator) {
                final String got = _getDeclaratorForLambdaPassover(
                        dispatcher,
                        getIdentifierFromDeclarator(declarator),
                        directDeclarator,
                        newIdentifier
                );
                joiner.add(got);
                break;
            }

            if (child == rightmostPointer) {
                assert child != null;
                joiner.add(
                        removeConstFromPointer(dispatcher, rightmostPointer)
                );
                continue;
            }
            joiner.add(dispatcher.visit(child));
        }

        return joiner.toString();
    }

    private static String removeConstFromPointer(
            final VisitorDispatcher dispatcher,
            final SSCParser.PointerContext rightmostPointer
    ) {
        final StringJoiner pointerJoiner = new StringJoiner(" ");
        // (('*' | '^') typeQualifierList?)+
        for (final ParseTree pointerChild : rightmostPointer.children) {
            if (!(pointerChild instanceof SSCParser.TypeQualifierListContext typeQualifiers)) {
                pointerJoiner.add(
                        dispatcher.visit(pointerChild)
                );
                continue;
            }

            for (final SSCParser.TypeQualifierContext typeQualifierContext : typeQualifiers.typeQualifier()) {
                if (typeQualifierContext.Const() != null) {
                    continue;
                }

                pointerJoiner.add(
                        dispatcher.visit(typeQualifierContext)
                );
            }
        }
        return pointerJoiner.toString();
    }

    //	  Identifier attributeSpecifierSequence?
    //	| '(' declarator ')'
    //	| Identifier ':' DigitSequence         // bit field
    //	| vcSpecificModifer Identifier         // Visual C Extension
    //	| '(' vcSpecificModifer declarator ')' // Visual C Extension
    //	| gnuAttribute
    //    )
    //    ( '[' typeQualifierList? assignmentExpression? ']' attributeSpecifierSequence?
    //      | '[' 'static' typeQualifierList? assignmentExpression ']' attributeSpecifierSequence?
    //      | '[' typeQualifierList 'static' assignmentExpression ']' attributeSpecifierSequence?
    //      | '[' typeQualifierList? '*' ']' attributeSpecifierSequence?
    //      | '(' parameterTypeList ')' attributeSpecifierSequence?
    //    )*
    private static String _getDeclaratorForLambdaPassover(
            final VisitorDispatcher dispatcher,
            final TerminalNode identifierNode,
            final SSCParser.DirectDeclaratorContext directDeclarator,
            final String newIdentifier
    ) {
        final boolean hasAnyBrackets = !directDeclarator.LeftBracket().isEmpty();
        if (hasAnyBrackets) {
            dispatcher.addTerminalReplacement(identifierNode, String.format("( * %s )", newIdentifier));
        } else {
            dispatcher.addTerminalReplacement(identifierNode, newIdentifier);
        }

        final StringJoiner s = new StringJoiner(" ");
        {
            boolean hasFoundBrackets = false;
            boolean currentlyInFirstBrackets = false;
            for (final ParseTree child : directDeclarator.children) {
                if (child instanceof TerminalNode t) {
                    if (t.getSymbol().getType() == SSCParser.LeftBracket && !hasFoundBrackets) {
                        hasFoundBrackets = true;
                        currentlyInFirstBrackets = true;
                    }

                    if (currentlyInFirstBrackets && t.getSymbol().getType() == SSCParser.RightBracket) {
                        currentlyInFirstBrackets = false;
                        continue;
                    }
                }

                if (currentlyInFirstBrackets) {
                    continue;
                }

                s.add(
                        dispatcher.visit(child)
                );
            }
        }

        if (hasAnyBrackets) {
            dispatcher.removeTerminalReplacement(identifierNode);
        }

        return s.toString();
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
            return String.format(
                    "( %s ) %s",
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


    private static final AtomicLong IDS = new AtomicLong();

    public static String createNameWithID(
            final String sscIdentifier,
            final String surroundingFunctionName
    ) {
        return String.format("%s_id%d_%s", sscIdentifier, IDS.getAndIncrement(), surroundingFunctionName);
    }

    // specifierQualifierList abstractDeclarator?
    public static String createTypedef(
            final VisitorDispatcher dispatcher,
            final String prefix,
            final String surroundingFunctionName,
            final SSCParser.TypeNameContext typeName
    ) {
        // typeSpecifierQualifier+
        if (dispatcher.getLiteral(typeName).equals("void")) {
            return "void";
        }

        final List<ParseTree> identifier = allMatching(
                typeName.abstractDeclarator(),
                node -> node instanceof TerminalNode t && t.getSymbol().getType() == SSCParser.Identifier
        );
        identifier.forEach(node -> {
            throw dispatcher.getSSCLanguageException(
                    "Identifiers not allowed in lambda return types", node
            );
        });

        final String typedefIdentifier = createNameWithID(prefix, surroundingFunctionName);
        final String typedefDeclarator = insertIdentifierIntoDeclarator(dispatcher, typeName.abstractDeclarator(), typedefIdentifier);
        final String typedefSpecifiersQualifiers = dispatcher.visit(typeName.specifierQualifierList());
        final String typedef = "typedef " + typedefSpecifiersQualifiers + " " + typedefDeclarator + ";";
        dispatcher.addExternalDeclarationToEmitBefore(typedef);
        return typedefIdentifier;
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
     * For debugging
     *
     * @param tree   root
     * @param parser parser
     */
    public static void ASTPrint(ParseTree tree, Parser parser) {
        _astPrint(tree, parser, 0);
    }

    private static void _astPrint(ParseTree node, Parser parser, int indentation) {
        final String indent = "  ".repeat(indentation);

        final String nodeName;
        if (node instanceof RuleContext ctx) {
            final int ruleIndex = ctx.getRuleIndex();
            nodeName = parser.getRuleNames()[ruleIndex];
        } else {
            assert node instanceof TerminalNode;
            nodeName = '"' + node.getText() + '"';
        }

        System.out.println(indent + nodeName);

        for (int i = 0; i < node.getChildCount(); i++) {
            _astPrint(node.getChild(i), parser, indentation + 1);
        }
    }


    public static class Text {
        public static final String INDENT = "    ";

        public static boolean charMayBePartOfIdentifier(char c) {
            return charMayBePartOfIdentifier(0, c);
        }

        public static boolean charMayBePartOfIdentifier(int indexWithinIdentifier, char c) {
            return (c >= 'a' && c <= 'z') ||
                   (c >= 'A' && c <= 'Z') ||
                   (indexWithinIdentifier > 0 && (c >= '0' && c <= '9')) ||
                   (c == '_');
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
