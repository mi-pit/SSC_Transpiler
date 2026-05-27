package cz.mipit.sscc.ssc.compiler.visitors.convertors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.Main;
import cz.mipit.sscc.ssc.compiler.data.tmpl.Template;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TemplateDispatchConvertor extends AbstractConvertor<SSCParser.TemplateDispatchContext> {
    private static final Map<String, String> TYPE_SHORTHANDS = Map.ofEntries(
            // type
            Map.entry("void", "v"),

            Map.entry("bool", "b"),
            Map.entry("signed", "S"),
            Map.entry("unsigned", "U"),

            Map.entry("char", "c"),
            Map.entry("signed char", "Sc"),
            Map.entry("unsigned char", "Uc"),

            Map.entry("short", "Ss"),
            Map.entry("unsigned short", "Us"),

            Map.entry("int", "Si"),
            Map.entry("unsigned int", "Ui"),

            Map.entry("long", "Sl"),
            Map.entry("unsigned long", "Ul"),

            Map.entry("long long", "Sll"),
            Map.entry("unsigned long long", "Ull"),

            Map.entry("float", "f"),
            Map.entry("double", "d"),
            Map.entry("long double", "ld"),

            // Qual
            Map.entry("const", "C"),
            Map.entry("volatile", "V"),
            Map.entry("restrict", "R"),
            Map.entry("_Atomic", "A"),

            // Compound
            Map.entry("struct", "s"),
            Map.entry("object", "o"),
            Map.entry("enum", "e"),
            Map.entry("flagset", "f"),
            Map.entry("union", "u")
    );

    private final Set<String> alreadyEmitted;

    public TemplateDispatchConvertor(VisitorDispatcher dispatcher) {
        super(dispatcher, SSCParser.TemplateDispatchContext.class);

        alreadyEmitted = new HashSet<>();
    }

    public static String typeSpecifyTemplateName(String functionName, List<String> typeArgumentsConverted) {
        final StringBuilder sBuilder = new StringBuilder(
                functionName
        );

        for (int i = 0; i < typeArgumentsConverted.size(); i++) {
            final String typeArg = typeArgumentsConverted.get(i);
            sBuilder.append(i == 0 ? "__" : "_")
                    .append(typeArg);
        }

        return sBuilder.toString();
    }

    // Called `func<t1, t2>`
    @Override
    public String convert(SSCParser.TemplateDispatchContext ctx) {
        Main.logger.printDebug(() -> "Converting template dispatch: " + dispatcher.getLiteral(ctx));
        final List<SSCParser.TypeArgumentContext> typeArgs = ctx.templateDispatchTypeArguments().typeArgument();

        final String nameRaw = dispatcher.visit(ctx.Identifier());
        Main.logger.printDebug(() -> "\tRaw name: " + nameRaw);
        final String nameUnqualifiedMangled = mangleFunctionName(nameRaw);
        Main.logger.printDebug(() -> "\tUnqualified but mangled name: " + nameUnqualifiedMangled);
        final String nameTypeResolved = typeSpecifyTemplateName(
                nameUnqualifiedMangled,
                typeArgs.stream()
                        .map(o -> TemplateDispatchConvertor.convertTypeArgumentToShorthand(o, dispatcher))
                        .toList()
        );
        Main.logger.printDebug(() -> "\tResolved template call name: " + nameTypeResolved);

        final Template tmpl = dispatcher.data.templates().get(nameRaw);
        if (tmpl == null) {
            Main.logger.printDebug(() -> "\t\tTemplate '" + nameRaw + "' not found");
            throw dispatcher.getSSCLanguageException("Unknown template '" + nameUnqualifiedMangled + "'", ctx);
        }
        Main.logger.printDebug(() -> "\t\tTemplate found");

        if (alreadyEmitted.contains(nameTypeResolved)) {
            Main.logger.printDebug("\tAlready emitted template '" + nameTypeResolved + "'; no need to re-emit.");
            return nameTypeResolved;
        }

        final List<SSCParser.TemplateDefinitionContext> tmplContexts = tmpl.contexts();

        for (final SSCParser.TemplateDefinitionContext tmplContext : tmplContexts) {
            final Map<String, String> typeArgMap = new HashMap<>();
            if (tmplContext.templateHeader().templateTypes().Identifier().size() != typeArgs.size()) {
                throw dispatcher.getSSCLanguageException("Invalid number of type arguments", ctx);
            }

            for (int i = 0; i < typeArgs.size(); i++) {
                final String actualType = dispatcher.visit(typeArgs.get(i));
                final TerminalNode typeAliasIdent = tmplContext.templateHeader().templateTypes().Identifier().get(i);
                final String typeAlias = dispatcher.visit(typeAliasIdent);

                if (typeArgMap.put(typeAlias, actualType) != null) {
                    throw dispatcher.getSSCLanguageException("Duplicate type alias", ctx);
                }
            }

            dispatcher.addReplacements(typeArgMap);
            Main.logger.printDebug(() -> "\tType replacements: '" + typeArgMap + "'");
            Main.logger.printDebug(() ->
                    "\tCalled Type Arguments (literal): "
                    + typeArgs
                            .stream()
                            .map(dispatcher::getLiteral)
                            .toList()
            );

            final String toReplace = dispatcher.visit(ctx.Identifier());
            dispatcher.addReplacement(toReplace, nameTypeResolved);
            Main.logger.printDebug(() -> "\tAdded identifier replacement: `" + toReplace
                                         + "` -> `" + nameTypeResolved + "`");

            final String tmplConverted;
            if (tmplContext.functionDefinition() != null) {
                tmplConverted = dispatcher.visit(tmplContext.functionDefinition());
            } else if (tmplContext.superStructInterface() != null) {
                tmplConverted = dispatcher.visit(tmplContext.superStructInterface());
            } else {
                tmplConverted = dispatcher.visit(tmplContext.superStructSpecifier());
            }

            dispatcher.addExternalDeclarationToEmitBefore(tmplConverted);
            alreadyEmitted.add(nameTypeResolved);

            dispatcher.removeReplacements(typeArgMap);
            dispatcher.removeReplacement(toReplace);

            Main.logger.printDebug(() -> "\tRemoved identifier replacement: `" + toReplace + "`");
        }

        return nameTypeResolved;
    }

    /// Creates a generic (not type specified) template name from an identifier
    private static String mangleFunctionName(String identifier) {
        return "SSCtmpl__" + identifier;
    }

    public static String convertTypeArgumentToShorthand(
            SSCParser.TypeArgumentContext ctx,
            VisitorDispatcher dispatcher
    ) {
        final StringBuilder builder = new StringBuilder();
        for (ParseTree child : ctx.children) {
            final String type = dispatcher.visit(child);
            final String converted = convertTypeToShorthand(type);
            builder.append(converted);
        }

        return builder.toString();
    }

    /**
     * If `type` is present as a key in {@link TemplateDispatchConvertor#TYPE_SHORTHANDS}, return the value.
     * Else, return a string converted char-by-char according to this contract:
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
    private static String convertTypeToShorthand(
            String type
    ) {
        if (TYPE_SHORTHANDS.containsKey(type)) {
            return TYPE_SHORTHANDS.get(type);
        }

        final StringBuilder builder = new StringBuilder();
        for (final char ch : type.toCharArray()) {
            if (Character.isAlphabetic(ch) || ch == '_') {
                builder.append(ch);
            } else if (ch == '*') {
                builder.append('p');
            } else {
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
