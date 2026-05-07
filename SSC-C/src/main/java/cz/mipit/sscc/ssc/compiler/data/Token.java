package cz.mipit.sscc.ssc.compiler.data;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.ssc.compiler.data.tmpl.Template;
import cz.mipit.sscc.ssc.compiler.visitors.BaseConvertorVisitor;

import java.util.List;
import java.util.Objects;

public class Token {
    private enum Type {
        TEMPLATE_TYPE,
        TEMPLATE_REFERENCE,
        OTHER,
    }

    private final Type kind;

    private final String other;
    private final String templateType;
    private final Template template;

    private Token(
            Type kind,
            Template template,
            String templateType,
            String other
    ) {
        this.kind = Objects.requireNonNull(kind, "Type cannot be null");

        this.templateType = templateType;
        this.template = template;
        this.other = other;

        if (kind == Type.TEMPLATE_TYPE && templateType == null
                || kind == Type.TEMPLATE_REFERENCE && template == null
                || kind == Type.OTHER && other == null) {
            throw new IllegalArgumentException("Null data for token type '" + kind + "'");
        }
    }

    public static Token template(Template template) {
        return new Token(Type.TEMPLATE_REFERENCE, template, null, null);
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
            final BaseConvertorVisitor visitor
    ) {
        return switch (kind) {
            case TEMPLATE_TYPE -> {
                for (int i = 0; i < template.getTypeArgumentAliases().size(); i++) {
                    final String typeArgumentAlias = template.getTypeArgumentAliases().get(i);
                    // if "A" == this
                    //    return "int"
                    if (typeArgumentAlias.equals(this.templateType)) {
                        yield visitor.visitTypeArgument(actualTypeArgs.get(i));
                    }
                }
                throw new IllegalStateException(
                        "Type argument " + this.templateType + " not found (in " + template.getTypeArgumentAliases() + ")"
                );
            }

            case OTHER -> this.other;

            case TEMPLATE_REFERENCE -> this.template.convert(actualTypeArgs);
        };
    }
}
