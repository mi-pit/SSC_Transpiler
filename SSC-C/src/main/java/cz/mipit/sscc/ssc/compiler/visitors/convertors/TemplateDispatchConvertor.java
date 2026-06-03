package cz.mipit.sscc.ssc.compiler.visitors.convertors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.Main;
import cz.mipit.sscc.ssc.compiler.data.ss.SuperStruct;
import cz.mipit.sscc.ssc.compiler.data.tmpl.Template;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;
import cz.mipit.sscc.util.collection.Enumerated;
import cz.mipit.sscc.util.collection.Enumerator;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TemplateDispatchConvertor extends AbstractConvertor<SSCParser.TemplateDispatchContext> {
    private final Set<String> alreadyEmitted;

    public TemplateDispatchConvertor(VisitorDispatcher dispatcher) {
        super(dispatcher, SSCParser.TemplateDispatchContext.class);

        alreadyEmitted = new HashSet<>();
    }

    // Called `func<t1, t2>`
    @Override
    public String convert(SSCParser.TemplateDispatchContext ctx) {
        Main.logger.printDebug(() -> "Converting template dispatch: " + dispatcher.getLiteral(ctx));
        final List<SSCParser.TypeArgumentContext> typeArgs = ctx.templateDispatchTypeArguments().typeArgument();

        final String nameRaw = dispatcher.getLiteral(ctx.Identifier());
        Main.logger.printDebug(() -> "\tRaw name: " + nameRaw);
        final String nameUnqualifiedMangled = mangleFunctionName(nameRaw);
        Main.logger.printDebug(() -> "\tUnqualified but mangled name: " + nameUnqualifiedMangled);
        final String nameTypeResolved = typeSpecifyTemplateName(
                nameUnqualifiedMangled,
                typeArgs.stream()
                        .map(o -> TemplateDispatchConvertor.convertTypeArgumentToShorthand(dispatcher, o))
                        .toList()
        );
        Main.logger.printDebug(() -> "\tResolved template call name: " + nameTypeResolved);

        final Template tmpl = dispatcher.data.templates().get(nameRaw);
        if (tmpl == null) {
            Main.logger.printDebug(() -> "\t\tTemplate '" + nameRaw + "' not found");
            throw dispatcher.getSSCLanguageException(
                    "Unknown template '" + nameRaw
                    + "<" + ctx.templateDispatchTypeArguments().typeArgument().size() + ">'", ctx
            );
        }
        Main.logger.printDebug(() -> "\t\tTemplate found");

        if (alreadyEmitted.contains(nameTypeResolved)) {
            Main.logger.printDebug("\tAlready emitted template '" + nameTypeResolved + "'; no need to re-emit.");
            return nameTypeResolved;
        }
        alreadyEmitted.add(nameTypeResolved);

        for (final SSCParser.TemplateDefinitionContext tmplContext : tmpl.contexts()) {
            final Map<String, String> typeArgMap = new HashMap<>();
            final int templateTypeArgs = tmplContext.templateHeader().templateTypes().Identifier().size();
            final int calledTypeArgs = typeArgs.size();
            if (templateTypeArgs != calledTypeArgs) {
                throw dispatcher.getSSCLanguageException(
                        "Invalid number of type arguments (expected %d, got %d)".formatted(templateTypeArgs, calledTypeArgs),
                        ctx.templateDispatchTypeArguments()
                );
            }

            final List<TerminalNode> aliasIdentifiers = tmplContext.templateHeader().templateTypes().Identifier();
            for (int i = 0; i < typeArgs.size(); i++) {
                final String typeAlias = dispatcher.getLiteral(aliasIdentifiers.get(i));
                final String actualType = dispatcher.visit(typeArgs.get(i));

                if (typeArgMap.put(typeAlias, actualType) != null) {
                    throw dispatcher.getSSCLanguageException("Duplicate type alias", ctx);
                }
            }

            dispatcher.pushReplacementsFrame();

            dispatcher.addReplacements(typeArgMap);
            Main.logger.printDebug(() -> "\tType replacements: '" + typeArgMap + "'");
            Main.logger.printDebug(() ->
                    "\tCalled Type Arguments (literal): "
                    + typeArgs
                            .stream()
                            .map(dispatcher::getLiteral)
                            .toList()
            );

            dispatcher.addReplacement(nameRaw, nameTypeResolved);
            Main.logger.printDebug(() -> "\tAdded identifier replacement: `" + nameRaw
                                         + "` -> `" + nameTypeResolved + "`");

            dispatcher.data.templateStack.push(null);

            final String tmplConverted;
            if (tmplContext.functionDefinition() != null) {
                final Deque<SuperStruct> oldSuperstructStack = dispatcher.data.superstructStack;
                dispatcher.data.superstructStack = new ArrayDeque<>();

                tmplConverted = dispatcher.visit(tmplContext.functionDefinition());

                final Deque<SuperStruct> newSuperstructStack = dispatcher.data.superstructStack;

                dispatcher.data.superstructStack = oldSuperstructStack;
                for (final SuperStruct newSuperstruct : newSuperstructStack) {
                    oldSuperstructStack.push(newSuperstruct);
                }

            } else if (tmplContext.superStructInterface() != null) {
                tmplConverted = dispatcher.visit(tmplContext.superStructInterface());
            } else {
                tmplConverted = dispatcher.visit(tmplContext.superStructSpecifier());
            }
            dispatcher.data.templateStack.pop();

            dispatcher.addExternalDeclarationToEmitBefore(tmplConverted);

            dispatcher.removeReplacements(typeArgMap);
            dispatcher.removeReplacement(nameRaw);
            Main.logger.printDebug(() -> "\tRemoved identifier replacement: `" + nameRaw + "`");
            dispatcher.popReplacementsFrame();
        }

        return nameTypeResolved;
    }

    /// Creates a generic (not type specified) template name from an identifier
    public static String mangleFunctionName(String identifier) {
        return "__ssc_tmpl_" + identifier;
    }

    private static String typeSpecifyTemplateName(String functionName,
                                                  List<String> typeArgumentsConverted) {
        final StringBuilder sBuilder = new StringBuilder(
                functionName
        );

        for (Enumerated<String> s : new Enumerator<>(typeArgumentsConverted)) {
            sBuilder.append("_")
                    .append(s.index() + 1)
                    .append(s.item());
        }

        return sBuilder.toString();
    }

    public static String convertTypeArgumentToShorthand(
            VisitorDispatcher dispatcher,
            SSCParser.TypeArgumentContext ctx
    ) {
        final StringBuilder builder = new StringBuilder();
        for (ParseTree child : ctx.children) {
            final String type = dispatcher.visit(child);
            final String converted = convertTypeToIdentifierPart(type);
            builder.append(converted);
        }

        return builder.toString();
    }

    /**
     * return a string converted char-by-char according to this contract:
     * <table>
     *   <tr>
     *     <th>Condition</th>
     *     <th>Conversion</th>
     *   </tr>
     *   <tr>
     *     <td>char is c-identifier-part</td>
     *     <td>no conversion</td>
     *   </tr>
     *   <tr>
     *     <td>char is '*'</td>
     *     <td>'p'</td>
     *   </tr>
     *   <tr>
     *     <td>any other char (invalid identifier part, not a pointer)</td>
     *     <td>'0'</td>
     *   </tr>
     * </table>
     *
     * @param type String representation of the type. Preferably should be only a single keyword (like `int` or `size_t`),
     *             but this function is made to
     */
    private static String convertTypeToIdentifierPart(
            String type
    ) {
        final StringBuilder builder = new StringBuilder();
        for (final char ch : type.toCharArray()) {
            if (Character.isAlphabetic(ch) || ch == '_') {
                builder.append(ch);
            } else if (ch == '*') {
                builder.append('p');
            } else if (!Character.isWhitespace(ch)) {
                // '0' is an invalid start of an identifier => must be coming from here
                builder.append('0');
            }
        }

        for (int i = 0; i < builder.length(); i++) {
            final char c = builder.charAt(i);

            if (Character.isAlphabetic(c) || c == '_')
                continue;
            if (i != 0 && Character.isDigit(c))
                continue;

            throw new IllegalStateException(
                    "Something went wrong while converting template type: \"" + type + "\". " +
                    "Convertor emitted an invalid c-identifier character '" + c + "'"
            );
        }

        return builder.toString();
    }
}
