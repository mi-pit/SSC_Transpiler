package cz.muni.fi.sscc.data;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record SuperStructRepre(String name, List<SSMember> members) {
    public SuperStructRepre(String name, List<SSMember> members) {
        this.name = Objects.requireNonNull(name);
        this.members = Objects.requireNonNull(members);
    }

    public String convert() {
        final StringBuilder result = new StringBuilder();

        result.append(String.format("superstruct %s {\n", name));
        for (SSMember member : members) {
            member.data().getLeft().ifPresent(field -> result
                    /* do a little bit of formatting for mid-compilation error messages */
                    .append("    ")
                    .append(field.getData())
                    .append("\n"));

        }
        result.append("};\n");

        getForwardDeclarations(result);

        for (SSMember member : members) {
            member.data().getRight().ifPresent(functionDefinition ->
                    result.append(functionDefinition.getDefinition()));
        }

        return result.toString();
    }

    private void getForwardDeclarations(final StringBuilder result) {
        for (SSMember member : members) {
            member.data().getRight().ifPresent(fnDef ->
                    result.append(fnDef.getDeclaration()));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;

        SuperStructRepre that = (SuperStructRepre) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }
}
