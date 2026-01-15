package cz.mipit.sscc.ssc.compiler.data.macro;

import cz.mipit.sscc.util.annotations.NotNull;
import cz.mipit.sscc.util.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/* TODO: add # and ## functionality */
public class Macro {
    private final @NotNull String identifier;
    private final @Nullable List<String> fields; /* null => no `()` */
    private final @Nullable List<MacroBodyMember> replacement; /* null => no body */

    public Macro(@NotNull String identifier,
                 @Nullable List<String> fields,
                 @Nullable List<MacroBodyMember> replacement) {
        this.identifier = Objects.requireNonNull(identifier);
        this.replacement = replacement;
        this.fields = fields;
    }

    public String replace(List<String> arguments) {
        final List<String> result = new ArrayList<>();
        for (MacroBodyMember member : replacement) {
            if (member.isArgument()) {
                final int fieldIdx = fields.indexOf(member.token());
                result.add(arguments.get(fieldIdx));
            } else {
                result.add(member.token());
            }
        }
        return String.join(" ", result);
    }


    public boolean hasArguments() {
        return !fields.isEmpty();
    }

    public String identifier() {
        return identifier;
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        Macro macro = (Macro) o;
        return Objects.equals(identifier, macro.identifier);
    }

    @Override
    public String toString() {
        return "Macro{" +
                "identifier='" + identifier + '\'' +
                ", fields=" + fields +
                ", replacement=" + replacement +
                '}';
    }
}
