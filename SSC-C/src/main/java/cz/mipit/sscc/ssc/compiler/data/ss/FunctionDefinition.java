package cz.mipit.sscc.ssc.compiler.data.ss;

import java.util.List;

public class FunctionDefinition {
    private final List<String> specs;
    private final boolean isStatic;
    private final boolean isPure;
    private final boolean isPrivate;
    private final String type;

    private final String unqualifiedName;

    private final List<String> params;
    private final String body; /* TODO?: List<String> for cc error messages */
    private final String superstructMemberOfName;

    /* TODO: parse in visitor */
    public FunctionDefinition(
            final boolean isStatic,
            final boolean isPure,
            final boolean isPrivate,
            final List<String> specsWithoutCustom,
            final String type,
            final String unqualifiedName,
            final List<String> params,
            final String body,
            final String superstructMemberOfName
    ) {
        this.specs = specsWithoutCustom;
        this.isStatic = isStatic;
        this.isPure = isPure;
        this.isPrivate = isPrivate;
        //this.type = parseType(ctx.declarationSpecifiers(), ctx.declarator());
        this.type = type;

        this.unqualifiedName = unqualifiedName;
        // this.originalName = parseName(ctx.declarator());

        //this.params = parseFunctionArgs(ctx.declarator());
        this.params = params;
        this.body = body;
        //this.body = convertor.visitFunctionBody(ctx.functionBody());
        this.superstructMemberOfName = superstructMemberOfName;

        if (!isStatic && params.size() == 1 && params.getFirst().equals("void")) {
            this.params.removeFirst();
        }
    }

    public String getDeclaration() {
        return getDeclaration(false) + ";";
    }

    public String getDefinition() {
        return getDeclaration(true) + " " + getBody();
    }

    private String getDeclaration(boolean willHaveBody) {
        final StringBuilder selfRef = new StringBuilder();
        if (!isStatic) {
            if (isPure) {
                selfRef.append("const ");
            }
            selfRef
                    .append("struct ")
                    .append(superstructMemberOfName)
                    .append(" *");

            if (willHaveBody) {
                selfRef.append("const this");
            }

            if (!params.isEmpty()) {
                selfRef.append(", ");
            }
        }

        final String specsString = "static " + String.join(" ", specs);

        return specsString
                + (specsString.isBlank() ? "" : " ")
                + type
                + " " + superstructMemberOfName + "__" + unqualifiedName
                + "(" + selfRef + String.join(", ", params) + ")";
    }

    private String getBody() {
        return body;
    }

    public String getUnqualifiedName() {
        return unqualifiedName;
    }

    public boolean isPrivate() {
        return isPrivate;
    }


    @Override
    public String toString() {
        return "FunctionDefinition{" + getDeclaration(true) + "}";
    }
}
