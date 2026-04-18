package cz.mipit.sscc.ssc.compiler.visitors.convertors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.ssc.compiler.data.tmpl.Template;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TemplateDefinitionConvertor {
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

    public TemplateDefinitionConvertor(VisitorDispatcher dispatcher) {
        this.dispatcher = dispatcher;

        templates = new HashMap<>();
    }

    private String resolveTemplateName(String functionName, List<String> typeArgumentsConverted) {
        final StringBuilder sBuilder = new StringBuilder(
                functionName
        );

        for (String typeArg : typeArgumentsConverted) {
            sBuilder.append("_")
                    .append(typeArg);
        }

        return sBuilder.toString();
    }

    public String convertTemplateDispatch(SSCParser.TemplateDispatchContext ctx) {
        final List<String> typeArgs =
                ctx.typeArgument().stream().map(dispatcher::visitTypeArgument).toList();

        final String unqualified = getMangledFunctionName(ctx.Identifier());
        final String resolved = resolveTemplateName(
                unqualified,
                ctx.typeArgument().stream().map(this::convertTypeArgumentToShorthand).toList()
        );

        final Template tmpl = templates.get(unqualified);
        if (tmpl == null) {
            throw dispatcher.getSSCSyntaxException("Unknown template '" + resolved + "'", ctx);
        }

        dispatcher.methodsToEmit.add(
                tmpl.convert(typeArgs, resolved)
        );

        return resolved;
    }

    public void visitTemplateDefinition(SSCParser.FunctionTemplateDefinitionContext ctx) {
        final SSCParser.FunctionDefinitionContext funcDefCtx = ctx.functionDefinition();
        final SSCParser.DirectDeclaratorContext directDeclarator = funcDefCtx.declarator().directDeclarator();
        if (directDeclarator.Identifier() == null) {
            throw dispatcher.getSSCSyntaxException(
                    "No function name in template definition", directDeclarator);
        }
        final String unqualifiedName = getMangledFunctionName(directDeclarator.Identifier());
        dispatcher.pushFunction(unqualifiedName, ctx.functionDefinition());

        final List<String> identifiers = ctx.identifierList().Identifier().stream().map(dispatcher::visitTerminal).toList();

        final Template tmpl = new Template(
                unqualifiedName,
                funcDefCtx.attributeSpecifierSequence() == null
                        ? ""
                        : dispatcher.visitAttributeSpecifierSequence(funcDefCtx.attributeSpecifierSequence()),
                identifiers,
                List.of(dispatcher
                        .visitDeclarationSpecifiers(funcDefCtx.declarationSpecifiers())
                        .split(" ")),
                List.of(
                        List.of(dispatcher
                                .visitParameterTypeList(funcDefCtx.declarator().directDeclarator().parameterTypeList().getFirst())
                                .split(" ")
                        )
                ),
                List.of(dispatcher
                        .visitFunctionBody(funcDefCtx.functionBody())
                        .split(" "))
        );

        registerTemplate(tmpl);

        dispatcher.popFunction();
    }

    private String getMangledFunctionName(TerminalNode identifier) {
        return "SSC_TEMPLATE__" + dispatcher.visitTerminal(identifier);
    }


    private String convertTypeArgumentToShorthand(SSCParser.TypeArgumentContext ctx) {
        final String literal = dispatcher.getLiteral(ctx.typeSpecifier());

        String shorthand = TYPE_SHORTHANDS.getOrDefault(literal,
                COMPOUND_SHORTHANDS.getOrDefault(literal, literal));

        final StringBuilder resultBuilder = new StringBuilder(shorthand);

        for (SSCParser.TypeQualifierContext typeQual : ctx.typeQualifier()) {
            resultBuilder.append(
                    QUALIFIER_SHORTHANDS.get(
                            dispatcher.getLiteral(typeQual)
                    )
            );
        }

        for (SSCParser.PointerContext pointer : ctx.pointer()) {
            resultBuilder.append("p");
            if (pointer.typeQualifierList() != null) {
                for (var typeQual : pointer.typeQualifierList()) {
                    resultBuilder.append(
                            QUALIFIER_SHORTHANDS.get(dispatcher.getLiteral(typeQual))
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
