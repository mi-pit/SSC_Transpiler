package cz.muni.fi.sscc.data;

import java.util.List;
import java.util.Objects;

public record SuperStructRepre(String name, List<SSMember> members) {
    public SuperStructRepre(String name, List<SSMember> members) {
        this.name = Objects.requireNonNull(name);
        this.members = Objects.requireNonNull(members);
    }

    public String convert() {
        final StringBuilder result = new StringBuilder();

        result.append(String.format("superstruct %s {\n", name));
        for (SSMember member : members) {
            if (member.data().getLeft().isEmpty()) {
                continue;
            }

            result
                    /* do a little bit of formatting for mid-compilation error messages */
                    .append("    ")
                    .append(member.data().getLeft().get().getData())
                    .append("\n");
        }
        result.append("}\n");

        for (SSMember member : members) {
            if (member.data().getRight().isEmpty()) {
                continue;
            }

            result
                    /* :( ts is really horrible
                     * basically -- superstruct can not have a semicolon after itself
                     * because it would mess up `typedef`s
                     * therefore append one BEFORE each method
                     * warnings against extra semicolons are turned off anyway ;)
                     */
                    .append(";")
                    .append(member.data().getRight().get().getText());
        }

        return result.toString();
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
