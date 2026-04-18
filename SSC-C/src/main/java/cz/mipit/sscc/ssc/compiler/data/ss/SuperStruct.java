package cz.mipit.sscc.ssc.compiler.data.ss;

import cz.mipit.sscc.util.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public class SuperStruct {
    private final String name;
    private final List<SSMember> members;

    public SuperStruct(@NotNull final String name) {
        this.name = Objects.requireNonNull(name);
        this.members = new ArrayList<>();
    }

    public String emitStructDefinition() {
        final StringBuilder resultBuilder = new StringBuilder();

        resultBuilder.append(String.format("struct %s {%n", name));
        for (SSMember member : members) {
            member.data().getLeft().ifPresent(field -> resultBuilder
                    /* do a little bit of formatting for mid-compilation error messages */
                    .append("    ")
                    .append(field.getWhole())
                    .append(";")
                    .append(System.lineSeparator())
            );
        }
        resultBuilder.append("}");

        return resultBuilder.toString();
    }

    public String emitMethodDeclarations() {
        return emitMethods(FunctionDefinition::getDeclaration);
    }

    public String emitMethodDefinitions() {
        return emitMethods(FunctionDefinition::getDefinition);
    }

    private String emitMethods(final Function<FunctionDefinition, String> functionFunction) {
        final StringBuilder resultBuilder = new StringBuilder();
        for (final SSMember member : members) {
            member.data().getRight().ifPresent(fnDef ->
                    resultBuilder
                            .append(functionFunction.apply(fnDef))
                            .append(System.lineSeparator())
            );
        }
        return resultBuilder.toString();
    }

    public List<FunctionDefinition> getFunctions() {
        return members.stream()
                .map(member -> member.data().getRight())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    public String name() {
        return name;
    }

    public List<SSMember> members() {
        return members;
    }

    public void addMember(SSMember member) {
        members.add(member);
    }

    public Optional<FunctionDefinition> findMethod(final String methodName) {
        for (final FunctionDefinition func : this.getFunctions()) {
            if (func.getUnqualifiedName().equals(methodName)) {
                return Optional.of(func);
            }
        }

        return Optional.empty();
    }
}
