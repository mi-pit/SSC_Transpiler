package cz.mipit.sscc.ssc.compiler.data.ss;

import cz.mipit.sscc.ssc.compiler.data.FunctionHeaderData;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;
import cz.mipit.sscc.util.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class SuperstructMethod {
    private final VisitorDispatcher dispatcher;
    private final FunctionHeaderData functionHeaderData;

    private final boolean isPrivate;

    private final List<String> params;
    private final @Nullable String body;

    private final SuperStruct superstructMemberOf;

    /**
     * Declaration is cached since it's queried multiple times and immutable.
     */
    private final String declaration;

    public SuperstructMethod(
            final VisitorDispatcher dispatcher,
            final SuperStruct superstructMemberOf,
            final FunctionHeaderData headerData,
            final boolean isPrivate,
            final List<String> params,
            final String body
    ) {
        this.dispatcher = dispatcher;

        this.functionHeaderData = headerData;
        this.isPrivate = isPrivate;
        this.params = new ArrayList<>(params);
        this.body = body;
        this.superstructMemberOf = superstructMemberOf;

        if (!headerData.isStatic() && params.size() == 1 && params.getFirst().equals("void")) {
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
        return Optional.of(declaration + body);
    }

    private String createDeclaration() {
        final String declSpecs = String.join(" ", functionHeaderData.cDeclarationSpecifiers());

        final String pointers = functionHeaderData.declarator()
                .pointer()
                .stream()
                .map(dispatcher::visitPointer)
                .collect(Collectors.joining(" "));
        final String qualifiedName = superstructMemberOf.qualifyName(getUnqualifiedName());
        final String selfRef = getSelfReferenceVariableDeclaration();
        final String parametersString = "( " + selfRef + String.join(", ", params) + " )";
        final String declarator = pointers + qualifiedName + parametersString;

        return declSpecs + " " + declarator;
    }

    private String getSelfReferenceVariableDeclaration() {
        final StringBuilder selfRef = new StringBuilder();
        if (!functionHeaderData.isStatic()) {
            if (functionHeaderData.isPure()) {
                selfRef.append("const ");
            }
            selfRef
                    .append("struct ")
                    .append(superstructMemberOf.name())
                    .append(" *");

            selfRef.append("const this");

            if (!params.isEmpty()) {
                selfRef.append(", ");
            }
        }
        return selfRef.toString();
    }

    public String getUnqualifiedName() {
        return functionHeaderData.unqualifiedName();
    }

    public boolean isPrivate() {
        return isPrivate;
    }


    @Override
    public String toString() {
        return "FunctionDefinition{" + declaration + "}";
    }
}
