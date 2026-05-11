package cz.mipit.sscc.ssc.compiler.visitors.convertors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.Main;
import cz.mipit.sscc.ssc.compiler.data.Token;
import cz.mipit.sscc.ssc.compiler.data.tmpl.Template;
import cz.mipit.sscc.ssc.compiler.visitors.BaseConvertorVisitor;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TemplateConvertor {
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

    private final VisitorDispatcher dispatcher;

    public TemplateConvertor(VisitorDispatcher dispatcher) {
        this.dispatcher = dispatcher;

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
    public String convertTemplateDispatch(SSCParser.TemplateDispatchContext ctx) {
        Main.logger.printDebug(() -> "Converting template dispatch: " + dispatcher.getLiteral(ctx));

        final String unqualified = getMangledFunctionName(dispatcher.visitTerminal(ctx.Identifier()));
        final String resolved = typeSpecifyTemplateName(
                unqualified,
                ctx.typeArgument()
                        .stream()
                        .map(o -> TemplateConvertor.convertTypeArgumentToShorthand(o, dispatcher))
                        .toList()
        );
        Main.logger.printDebug(() -> "\tResolved template call name: " + resolved);

        final Template tmpl = dispatcher.data.templates().get(unqualified);
        Main.logger.printDebug(() -> "\t\tTemplate: " + tmpl);
        if (tmpl == null) {
            throw dispatcher.getSSCSyntaxException("Unknown template '" + resolved + "'", ctx);
        }

        if (alreadyEmitted.contains(resolved)) {
            Main.logger.printDebug("\tAlready emitted template '" + resolved + "'");
            return resolved;
        }

        final List<SSCParser.TypeArgumentContext> typeArgs = ctx.typeArgument();
        final Map<String, String> typeArgMap = new HashMap<>();
        if (!(tmpl.getTypeArgumentAliases().size() == ctx.typeArgument().size())) {
            throw dispatcher.getSSCSyntaxException("Invalid number of type arguments", ctx);
        }
        for (int i = 0; i < typeArgs.size(); i++) {
            final String actualType = dispatcher.visitTypeArgument(typeArgs.get(i));
            final String typeAlias = tmpl.getTypeArgumentAliases().get(i);

            if (typeArgMap.put(typeAlias, actualType) != null) {
                throw dispatcher.getSSCSyntaxException("Duplicate type alias", ctx);
            }
        }
        Main.logger.printDebug(() -> "\tType argument map: '" + typeArgMap + "'");

        final List<SSCParser.TypeArgumentContext> typeArgumentCtxList = ctx.typeArgument();
        Main.logger.printDebug(() ->
                "\tCalled Type Arguments (literal): "
                        + typeArgumentCtxList
                        .stream()
                        .map(dispatcher::getLiteral)
                        .toList()
        );
        final String tmplConverted = tmpl.convert(typeArgumentCtxList);
        dispatcher.addMethodToEmit(tmplConverted);
        alreadyEmitted.add(resolved);

        return resolved;
    }

    public String visitTemplateDefinition(SSCParser.FunctionTemplateDefinitionContext ctx) {
        final SSCParser.FunctionDefinitionContext funcDefCtx = ctx.functionDefinition();
        final SSCParser.DirectDeclaratorContext directDeclarator = funcDefCtx.functionHeader().declarator().directDeclarator();
        if (directDeclarator.Identifier() == null) {
            throw dispatcher.getSSCSyntaxException(
                    "No function name in template definition", directDeclarator);
        }
        final String unqualifiedName = getMangledFunctionName(
                dispatcher.visitTerminal(directDeclarator.Identifier())
        );
        dispatcher.pushFunction(unqualifiedName, ctx.functionDefinition());

        final List<String> typeAliasIdentifiers = ctx
                .templateTypes()
                .Identifier()
                .stream()
                .map(dispatcher::visitTerminal)
                .toList();

        final List<Token> bodyTokens = getBodyTokens(funcDefCtx, typeAliasIdentifiers);

        final Template tmpl = new Template(dispatcher,
                unqualifiedName,
                funcDefCtx.functionHeader().attributeSpecifierSequence() == null
                        ? ""
                        : dispatcher.visitAttributeSpecifierSequence(funcDefCtx.functionHeader().attributeSpecifierSequence()),
                typeAliasIdentifiers,
                List.of(dispatcher
                        .visitDeclarationSpecifiers(funcDefCtx.functionHeader().declarationSpecifiers())
                        .split(" ")),
                List.of(
                        List.of(dispatcher
                                .visitParameterTypeList(funcDefCtx.functionHeader().declarator().directDeclarator().parameterTypeList().getFirst())
                                .split(" ")
                        )
                ),
                bodyTokens
        );

        registerTemplate(tmpl);

        dispatcher.popFunction();

        return ""; // templates only exist when called
    }

    private List<Token> getBodyTokens(
            SSCParser.FunctionDefinitionContext funcDefCtx,
            List<String> typeAliasIdentifiers
    ) {
        final List<Token> bodyTokens = new ArrayList<>();
        getBodyTokens_(bodyTokens, funcDefCtx.functionBody(), typeAliasIdentifiers);
        return bodyTokens;
    }

    private void getBodyTokens_(
            final List<Token> tokens,
            final ParserRuleContext ctx,
            final List<String> typeAliasIdentifiers
    ) {
        for (ParseTree raw : ctx.children) {
            if (raw instanceof TerminalNode terminal) {
                final String visited = dispatcher.visitTerminal(terminal);

                // Parser sometimes parses type names as identifiers
                // Therefore we do this horribleness
                if (terminal.getSymbol().getType() == SSCParser.Identifier
                        && typeAliasIdentifiers.contains(visited)) {
                    tokens.add(Token.type(visited));
                } else {
                    tokens.add(Token.other(dispatcher.visitTerminal(terminal)));
                }

                continue;
            }

            if (!(raw instanceof ParserRuleContext prc)) {
                throw new IllegalStateException("ParseTree not instanceof ParserRuleContext (`" + raw.getText() + "`)");
            }

            switch (prc) {
                case SSCParser.LambdaFunctionContext lf -> tokens.add(Token.other(dispatcher.visitLambdaFunction(lf)));
                case SSCParser.SuperStructSpecifierContext sss ->
                        tokens.add(Token.other(dispatcher.visitSuperStructSpecifier(sss)));

                case SSCParser.TemplateDispatchContext tmplDispatchCtx -> {
                    final String unqualifiedName = dispatcher.visitTerminal(tmplDispatchCtx.Identifier());
                    final String mangled = getMangledFunctionName(unqualifiedName);
                    final Template tmpl = dispatcher.data.templates().get(mangled);
                    if (tmpl == null) {
                        throw dispatcher.getSSCSyntaxException(
                                "Unknown template '" + tmplDispatchCtx.Identifier() + "'", ctx);
                    }

                    final List<SSCParser.TypeArgumentContext> typeArgs = tmplDispatchCtx.typeArgument();
                    final List<Token> typeArgStrings = typeArgs.stream()
                            .map(dispatcher::visitTypeArgument)
                            .map(typeArg -> {
                                if (typeAliasIdentifiers.contains(typeArg)) {
                                    return Token.type(typeArg);
                                } else {
                                    return Token.other(typeArg);
                                }
                            })
                            .toList();

                    tokens.add(Token.template(
                            tmpl,
                            typeArgs
                    ));
                }

                case SSCParser.TypeSpecifierContext typeSpecCtx -> {
                    final String typeSpecString = dispatcher.visitTypeSpecifier(typeSpecCtx);
                    final Token token = typeAliasIdentifiers.contains(typeSpecString)
                            ? Token.type(typeSpecString)
                            : Token.other(typeSpecString);

                    tokens.add(token);
                }

                default -> getBodyTokens_(tokens, prc, typeAliasIdentifiers);
            }
        }
    }

    /// Creates a generic (not type specified) template name from an identifier
    public static String getMangledFunctionName(String identifier) {
        return "SSC_TEMPLATE__" + identifier;
    }

    public static String convertTypeArgumentToShorthand(String ctx) {
        final List<String> ls = List.of(ctx.split("\\s+"));
        final StringBuilder builder = new StringBuilder();
        for (String s : ls) {
            builder.append(
                    TYPE_SHORTHANDS.getOrDefault(
                            s,
                            COMPOUND_SHORTHANDS.getOrDefault(
                                    s,
                                    QUALIFIER_SHORTHANDS.getOrDefault(s, s)
                            )
                    )
            );
        }

        return builder.toString();
    }

    public static String convertTypeArgumentToShorthand(
            SSCParser.TypeArgumentContext ctx,
            BaseConvertorVisitor visitor
    ) {
        final String literal = visitor.getLiteral(ctx.typeSpecifier());

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

    private void registerTemplate(Template template) {
        dispatcher.data.templates().put(template.getName(), template);
    }
}
