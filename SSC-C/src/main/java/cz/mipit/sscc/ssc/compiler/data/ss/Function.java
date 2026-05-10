package cz.mipit.sscc.ssc.compiler.data.ss;

import cz.mipit.sscc.ssc.compiler.data.FunctionHeaderData;
import cz.mipit.sscc.util.annotations.Nullable;
import cz.mipit.sscc.util.collection.builder.ListBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Function {
    private final String unqualifiedName;

    private final boolean isStatic;
    private final boolean isPure;
    private final boolean isPrivate;
    private final List<String> cDeclarationSpecifiers;
    private final String type;

    private final List<String> params;
    private final @Nullable String body;

    private final String superstructMemberOfName;

    /**
     * Declaration is cached since it's queried multiple times and immutable.
     */
    private final String declaration;

    public Function(
            final FunctionHeaderData headerData,
            final boolean isPrivate,
            final String type,
            final List<String> params,
            final String body,
            final String superstructMemberOfName
    ) {
        this.cDeclarationSpecifiers = headerData.cDeclarationSpecifiers();
        this.isStatic = headerData.isStatic();
        this.isPure = headerData.isPure();
        this.isPrivate = isPrivate;
        this.unqualifiedName = headerData.unqualifiedName();
        this.type = type;
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

    public Optional<String> getDefinition() {
        if (body == null) {
            return Optional.empty();
        }
        return Optional.of(declaration + System.lineSeparator() + body);
    }

    private String createDeclaration() {
        final String qualifiedName = superstructMemberOfName + "__" + unqualifiedName;

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
                .from(cDeclarationSpecifiers)
                .plus(type)
                .plus(qualifiedName + "(" + selfRef + String.join(", ", params) + ")");

        return String.join(" ", tokensBuilder);
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
