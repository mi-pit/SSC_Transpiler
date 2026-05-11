package cz.mipit.sscc.ssc.compiler.data;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.ssc.compiler.data.tmpl.Template;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;
import cz.mipit.sscc.ssc.compiler.visitors.convertors.TemplateConvertor;

import java.util.List;
import java.util.Objects;

public class Token {
    private enum Type {
        TEMPLATE_TYPE,
        TEMPLATE_REFERENCE,
        OTHER,
    }

    private final Type kind;

    private record TemplateDispatchData(
            Template template,
            List<SSCParser.TypeArgumentContext> typeArguments
    ) {
    }

    private final String other;
    private final String templateType;
    private final TemplateDispatchData templateDispatch;

    private Token(
            Type kind,
            TemplateDispatchData templateDispatch,
            String templateType,
            String other
    ) {
        this.kind = Objects.requireNonNull(kind, "Type cannot be null");

        this.templateType = templateType;
        this.templateDispatch = templateDispatch;
        this.other = other;

        if (kind == Type.TEMPLATE_TYPE && templateType == null
                || kind == Type.TEMPLATE_REFERENCE && templateDispatch == null
                || kind == Type.OTHER && other == null) {
            throw new IllegalArgumentException("Null data for token type '" + kind + "'");
        }
    }

    public static Token template(Template template, List<SSCParser.TypeArgumentContext> typeArgs) {
        return new Token(Type.TEMPLATE_REFERENCE, new TemplateDispatchData(template, typeArgs), null, null);
    }

    public static Token type(String type) {
        return new Token(Type.TEMPLATE_TYPE, null, type, null);
    }

    public static Token other(String other) {
        return new Token(Type.OTHER, null, null, other);
    }


    public String convertTemplate(
            final List<SSCParser.TypeArgumentContext> actualTypeArgs,
            final Template template,
            final VisitorDispatcher dispatcher
    ) {
        return switch (kind) {
            case TEMPLATE_TYPE -> {
                for (int i = 0; i < template.getTypeArgumentAliases().size(); i++) {
                    final String typeArgumentAlias = template.getTypeArgumentAliases().get(i);
                    // if "A" == this
                    //    return "int"
                    if (typeArgumentAlias.equals(this.templateType)) {
                        yield dispatcher.visitTypeArgument(actualTypeArgs.get(i));
                    }
                }
                throw new IllegalStateException(
                        "Type argument " + this.templateType + " not found (in " + template.getTypeArgumentAliases() + ")"
                );
            }

            case OTHER -> this.other;

            case TEMPLATE_REFERENCE -> {
                final List<String> translatedTypes = this
                        .templateDispatch
                        .typeArguments
                        .stream()
                        .map(t -> {
                            final String v = dispatcher.visitTypeArgument(t);
                            return actualTypeArgs.contains(t) ? Token.type(v) : Token.other(v);
                        })
                        .map(o -> TemplateConvertor.convertTypeArgumentToShorthand(
                                o.convertTemplate(actualTypeArgs, template, dispatcher)
                        ))
                        .toList();

                dispatcher.addMethodToEmit(
                        templateDispatch.template.convert(
                                templateDispatch.typeArguments
                        )
                );

                yield TemplateConvertor.typeSpecifyTemplateName(
                        this.templateDispatch.template.getName(),
                        translatedTypes
                );
            }
        };
    }

    @Override
    public String toString() {
        final String strtok = switch (kind) {
            case TEMPLATE_TYPE -> templateType;
            case OTHER -> other;
            case TEMPLATE_REFERENCE -> templateDispatch.toString();
        };

        return "Token(%s){%s}".formatted(kind, strtok);
    }
}
