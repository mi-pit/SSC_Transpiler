package cz.mipit.sscc.ssc.compiler.data.ss;

import cz.mipit.sscc.util.ListBuilder;

import java.util.ArrayList;
import java.util.List;

public class FunctionDefinition {
    private final String unqualifiedName;

    private final boolean isStatic;
    private final boolean isPure;
    private final boolean isPrivate;
    private final List<String> cDeclarationSpecifiers;
    private final String type;

    private final List<String> params;
    private final String body;

    private final String superstructMemberOfName;

    /**
     * Declaration is cached since it's queried multiple times and immutable.
     */
    private final String declaration;

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
        this.cDeclarationSpecifiers = specsWithoutCustom;
        this.isStatic = isStatic;
        this.isPure = isPure;
        this.isPrivate = isPrivate;
        this.type = type;
        this.unqualifiedName = unqualifiedName;
        this.params = new ArrayList<>(params);
        this.body = body;
        this.superstructMemberOfName = superstructMemberOfName;

        if (!isStatic && params.size() == 1 && params.getFirst().equals("void")) {
            this.params.removeFirst();
        }

        declaration = createDeclaration();
    }

    public String getDeclaration() {
        return declaration + ";";
    }

    public String getDefinition() {
        return declaration + System.lineSeparator() + getBody();
    }

    private String createDeclaration() {
        final StringBuilder selfRef = new StringBuilder();
        if (!isStatic) {
            if (isPure) {
                selfRef.append("const ");
            }
            selfRef
                    .append("struct ")
                    .append(superstructMemberOfName)
                    .append(" *");

            selfRef.append("const this");

            if (!params.isEmpty()) {
                selfRef.append(", ");
            }
        }

        final ListBuilder<String> tokensBuilder = ListBuilder
                .from("static")
                .addAll(cDeclarationSpecifiers)
                .add(type)
                .add(superstructMemberOfName + "__" + unqualifiedName + "(" + selfRef + String.join(", ", params) + ")");

        return String.join(" ", tokensBuilder);
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
        return "FunctionDefinition{" + declaration + "}";
    }
}
