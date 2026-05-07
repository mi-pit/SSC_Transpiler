package cz.mipit.sscc.ssc.compiler.visitors.convertors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.ssc.compiler.data.Token;
import cz.mipit.sscc.ssc.compiler.data.tmpl.Template;
import cz.mipit.sscc.ssc.compiler.visitors.BaseConvertorVisitor;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private final Map<String, Template> templates;

    private final VisitorDispatcher dispatcher;

    public TemplateConvertor(VisitorDispatcher dispatcher) {
        this.dispatcher = dispatcher;

        templates = new HashMap<>();
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
        final String unqualified = getMangledFunctionName(dispatcher.visitTerminal(ctx.Identifier()));
        final String resolved = typeSpecifyTemplateName(
                unqualified,
                ctx.typeArgument()
                        .stream()
                        .map(o -> TemplateConvertor.convertTypeArgumentToShorthand(o, dispatcher))
                        .toList()
        );

        final Template tmpl = templates.get(unqualified);
        if (tmpl == null) {
            throw dispatcher.getSSCSyntaxException("Unknown template '" + resolved + "'", ctx);
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

        final String tmplConverted = tmpl.convert(ctx.typeArgument());
        dispatcher.addMethodToEmit(tmplConverted);

        return resolved;
    }

    public void visitTemplateDefinition(SSCParser.FunctionTemplateDefinitionContext ctx) {
        final SSCParser.FunctionDefinitionContext funcDefCtx = ctx.functionDefinition();
        final SSCParser.DirectDeclaratorContext directDeclarator = funcDefCtx.declarator().directDeclarator();
        if (directDeclarator.Identifier() == null) {
            throw dispatcher.getSSCSyntaxException(
                    "No function name in template definition", directDeclarator);
        }
        final String unqualifiedName = getMangledFunctionName(
                dispatcher.visitTerminal(directDeclarator.Identifier())
        );
        dispatcher.pushFunction(unqualifiedName, ctx.functionDefinition());

        final List<String> typeAliasIdentifiers = ctx.Identifier()
                .stream()
                .map(dispatcher::visitTerminal)
                .toList();

        final List<Token> bodyTokens = getBodyTokens(funcDefCtx, typeAliasIdentifiers);

        final Template tmpl = new Template(dispatcher,
                unqualifiedName,
                funcDefCtx.attributeSpecifierSequence() == null
                        ? ""
                        : dispatcher.visitAttributeSpecifierSequence(funcDefCtx.attributeSpecifierSequence()),
                typeAliasIdentifiers,
                List.of(dispatcher
                        .visitDeclarationSpecifiers(funcDefCtx.declarationSpecifiers())
                        .split(" ")),
                List.of(
                        List.of(dispatcher
                                .visitParameterTypeList(funcDefCtx.declarator().directDeclarator().parameterTypeList().getFirst())
                                .split(" ")
                        )
                ),
                bodyTokens
        );

        registerTemplate(tmpl);

        dispatcher.popFunction();
    }

    private List<Token> getBodyTokens(
            SSCParser.FunctionDefinitionContext funcDefCtx,
            List<String> typeAliasIdentifiers
    ) {
        final List<Token> bodyTokens = new ArrayList<>();
        getBodyTokens(bodyTokens, funcDefCtx.functionBody(), typeAliasIdentifiers);
        return bodyTokens;
    }

    private void getBodyTokens(
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

            switch (raw) {
                case SSCParser.LambdaFunctionContext lf -> tokens.add(Token.other(dispatcher.visitLambdaFunction(lf)));
                case SSCParser.SuperStructSpecifierContext sss ->
                        tokens.add(Token.other(dispatcher.visitSuperStructSpecifier(sss)));

                case SSCParser.TemplateDispatchContext tmplDispatchCtx -> {
                    final Template tmpl = templates.get(
                            getMangledFunctionName(dispatcher.visitTerminal(tmplDispatchCtx.Identifier()))
                    );
                    if (tmpl == null) {
                        throw dispatcher.getSSCSyntaxException(
                                "Unknown template '" + tmplDispatchCtx.Identifier() + "'", ctx);
                    }
                    tokens.add(Token.template(tmpl));
                }
                case SSCParser.TypeSpecifierContext typeSpecCtx -> {
                    final String typeSpecString = dispatcher.visitTypeSpecifier(typeSpecCtx);
                    final Token token = typeAliasIdentifiers.contains(typeSpecString)
                            ? Token.type(typeSpecString)
                            : Token.other(typeSpecString);

                    tokens.add(token);
                }
                default -> getBodyTokens(tokens, prc, typeAliasIdentifiers);
            }

        }
    }

    /// Creates a generic (not type specified) template name from an identifier
    public static String getMangledFunctionName(String identifier) {
        return "SSC_TEMPLATE__" + identifier;
    }

    public static String convertTypeArgumentToShorthand(SSCParser.TypeArgumentContext ctx, BaseConvertorVisitor visitor) {
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
        templates.put(template.getName(), template);
    }
}
