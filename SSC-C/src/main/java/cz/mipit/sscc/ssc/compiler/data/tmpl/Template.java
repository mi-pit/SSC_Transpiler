package cz.mipit.sscc.ssc.compiler.data.tmpl;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.ssc.compiler.data.Token;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;
import cz.mipit.sscc.ssc.compiler.visitors.convertors.TemplateConvertor;

import java.util.List;
import java.util.stream.Collectors;

public class Template {
    private final VisitorDispatcher dispatcher;

    private final String name;

    private final String fnAttributes;
    private final List<Token> returnType;
    private final List<List<Token>> parameters;
    private final List<Token> body;

    private final List<String> typeArgumentAliases;

    public List<String> getTypeArgumentAliases() {
        return typeArgumentAliases;
    }


    public Template(
            final VisitorDispatcher dispatcher,
            final String name,
            final String fnAttributes,
            final List<String> typeArgumentAliases,
            final List<String> returnType,
            final List<List<String>> parameters,
            final List<Token> functionTokens
    ) {
        this.dispatcher = dispatcher;

        this.name = name;
        this.fnAttributes = fnAttributes;
        this.typeArgumentAliases = typeArgumentAliases;
        this.returnType = returnType.stream().map(
                s -> typeArgumentAliases.contains(s)
                        ? Token.type(s)
                        : Token.other(s)
        ).toList();
        this.parameters = parameters.stream().map(
                strings -> strings.stream().map(
                        s -> typeArgumentAliases.contains(s)
                                ? Token.type(s)
                                : Token.other(s)
                ).toList()
        ).toList();

        this.body = functionTokens;
    }

    public String getName() {
        return name;
    }

    public String convert(List<SSCParser.TypeArgumentContext> calledTypeArguments) {
        final List<String> params = parameters
                .stream()
                .map(
                        l -> l.stream().map(
                                tok -> tok.convertTemplate(calledTypeArguments, this, dispatcher)
                        ).collect(Collectors.joining(" "))
                )
                .toList();
        final String resolvedName = TemplateConvertor.typeSpecifyTemplateName(
                name,
                calledTypeArguments.stream()
                        .map(o -> TemplateConvertor.convertTypeArgumentToShorthand(o, dispatcher))
                        .toList()
        );

        final String convertedReturnType = returnType.stream()
                .map(t -> t.convertTemplate(calledTypeArguments, this, dispatcher))
                .collect(Collectors.joining(" "));

        final String convertedBody = body.stream()
                .map(t -> t.convertTemplate(calledTypeArguments, this, dispatcher))
                .collect(Collectors.joining(" "));

        return fnAttributes +
                " " +
                convertedReturnType +
                " " + resolvedName +
                "(" +
                String.join(", ", params) +
                ")" +
                convertedBody
                ;
    }
}
