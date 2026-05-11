package cz.mipit.sscc.ssc.compiler.visitors.convertors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.Main;
import cz.mipit.sscc.ssc.compiler.data.tmpl.Template;
import cz.mipit.sscc.ssc.compiler.visitors.BaseConvertorVisitor;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TemplateDispatchConvertor extends AbstractConvertor<SSCParser.TemplateDispatchContext> {
    private static final Map<String, String> TYPE_SHORTHANDS = Map.ofEntries(
            Map.entry("bool", "b"),
            Map.entry("void", "v"),

            Map.entry("signed char", "Sc"),
            Map.entry("unsigned char", "Uc"),

            Map.entry("short", "Ss"),
            Map.entry("unsigned short", "Us"),

            Map.entry("int", "Si"),
            Map.entry("unsigned", "Ui"),

            Map.entry("long", "Sl"),
            Map.entry("unsigned long", "Ul"),

            Map.entry("long long", "Sll"),
            Map.entry("unsigned long long", "Ull"),

            Map.entry("float", "f"),
            Map.entry("double", "d"),
            Map.entry("long double", "ld")
    );

    private static final Map<String, String> QUALIFIER_SHORTHANDS = Map.ofEntries(
            Map.entry("const", "C"),
            Map.entry("volatile", "V"),
            Map.entry("restrict", "R"),
            Map.entry("_Atomic", "A")
    );

    private static final Map<String, String> COMPOUND_SHORTHANDS = Map.ofEntries(
            Map.entry("struct", "s"),
            Map.entry("object", "o"),
            Map.entry("enum", "e"),
            Map.entry("flagset", "f"),
            Map.entry("union", "u")
    );

    private final Set<String> alreadyEmitted;

    public TemplateDispatchConvertor(VisitorDispatcher dispatcher) {
        super(dispatcher);

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
        final List<SSCParser.TypeArgumentContext> typeArgs = ctx.typeArgument();

        final String nameRaw = dispatcher.visitTerminal(ctx.Identifier());
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
            throw dispatcher.getSSCSyntaxException("Unknown template '" + nameUnqualifiedMangled + "'", ctx);
        }
        Main.logger.printDebug(() -> "\t\tTemplate found");

        if (alreadyEmitted.contains(nameTypeResolved)) {
            Main.logger.printDebug("\tAlready emitted template '" + nameTypeResolved + "'; no need to re-emit.");
            return nameTypeResolved;
        }

        final Map<String, String> typeArgMap = new HashMap<>();
        if (tmpl.getContext().templateHeader().templateTypes().Identifier().size() != typeArgs.size()) {
            throw dispatcher.getSSCSyntaxException("Invalid number of type arguments", ctx);
        }

        for (int i = 0; i < typeArgs.size(); i++) {
            final String actualType = dispatcher.visitTypeArgument(typeArgs.get(i));
            final TerminalNode typeAliasIdent = tmpl.getContext().templateHeader().templateTypes().Identifier().get(i);
            final String typeAlias = dispatcher.visitTerminal(typeAliasIdent);

            if (typeArgMap.put(typeAlias, actualType) != null) {
                throw dispatcher.getSSCSyntaxException("Duplicate type alias", ctx);
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

        final String toReplace = dispatcher.visitTerminal(ctx.Identifier());
        dispatcher.addReplacement(toReplace, nameTypeResolved);
        Main.logger.printDebug(() -> "\tAdded identifier replacement: `" + toReplace
                + "` -> `" + nameTypeResolved + "`");

        final String tmplConverted = dispatcher.visitFunctionDefinition(
                tmpl.getContext().functionDefinition()
        );

        emitTemplateDispatch(tmplConverted, nameTypeResolved);

        dispatcher.removeReplacements(typeArgMap);
        dispatcher.removeReplacement(toReplace);

        Main.logger.printDebug(() -> "\tRemoved identifier replacement: `" + toReplace + "`");

        return nameTypeResolved;
    }

    private void emitTemplateDispatch(String tmplConverted, String nameTypeResolved) {
        dispatcher.addMethodToEmit(tmplConverted);
        alreadyEmitted.add(nameTypeResolved);
    }

    /// Creates a generic (not type specified) template name from an identifier
    public static String mangleFunctionName(String identifier) {
        return "SSC_TEMPLATE__" + identifier;
    }

    public static String convertTypeArgumentToShorthand(
            SSCParser.TypeArgumentContext ctx,
            BaseConvertorVisitor visitor
    ) {
        final String literal = visitor.visitTypeSpecifier(ctx.typeSpecifier());

        String shorthand = TYPE_SHORTHANDS.get(literal);
        if (shorthand == null) {
            final StringBuilder builder = new StringBuilder();
            final String[] spl = literal.split(" ");
            for (String sub : spl) {
                builder.append(COMPOUND_SHORTHANDS.getOrDefault(sub, sub));
            }
            shorthand = builder.toString();
        }

        final StringBuilder resultBuilder = new StringBuilder(shorthand);

        for (SSCParser.TypeQualifierContext typeQual : ctx.typeQualifier()) {
            resultBuilder.append(
                    QUALIFIER_SHORTHANDS.get(
                            visitor.getLiteral(typeQual)
                    )
            );
        }

        for (SSCParser.PointerContext pointer : ctx.pointer()) {
            resultBuilder.append("p");
            if (pointer.typeQualifierList() != null) {
                for (var typeQual : pointer.typeQualifierList()) {
                    resultBuilder.append(
                            QUALIFIER_SHORTHANDS.get(visitor.getLiteral(typeQual))
                    );
                }
            }
        }

        return resultBuilder.toString();
    }
}
