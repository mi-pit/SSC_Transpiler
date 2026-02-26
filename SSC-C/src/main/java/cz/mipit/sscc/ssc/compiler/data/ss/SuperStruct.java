package cz.mipit.sscc.ssc.compiler.data.ss;

import cz.mipit.sscc.util.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class SuperStruct {
    private final String name;
    private final List<SSMember> members;

    public SuperStruct(@NotNull final String name) {
        this.name = Objects.requireNonNull(name);
        this.members = new ArrayList<>();
    }

    public String convert() {
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
        resultBuilder
                .append("};")
                .append(System.lineSeparator());

        appendFunctions(resultBuilder, FunctionDefinition::getDeclaration);
        appendFunctions(resultBuilder, FunctionDefinition::getDefinition);

        return resultBuilder.toString();
    }

    private void appendFunctions(final StringBuilder resultBuilder,
                                 final Function<FunctionDefinition, String> function) {
        for (final SSMember member : members) {
            member.data().getRight().ifPresent(fnDef ->
                    resultBuilder
                            .append(function.apply(fnDef))
                            .append(System.lineSeparator())
            );
        }
    }

    public List<FunctionDefinition> getFunctions() {
        final List<FunctionDefinition> result = new ArrayList<>();
        for (SSMember member : members) {
            if (member.data().getRight().isPresent()) {
                result.add(member.data().getRight().get());
            }
        }
        return result;
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
}
