package cz.mipit.sscc.args;

import java.util.Arrays;
import java.util.Set;
import java.util.StringJoiner;

public class OptionString {
    private final Set<String> values;

    public OptionString(String... values) {
        if (values.length == 0) {
            throw new IllegalArgumentException("Must provide at least one value");
        }
        // use `copyOf` to throw if an element is null
        this.values = Set.copyOf(Arrays.asList(values));
    }

    public String formatted(final boolean hasArgument) {
        final String argString = (hasArgument ? " ‹arg›" : "");
        final StringJoiner joiner = new StringJoiner(", ");
        for (final String value : values) {
            joiner.add(value + argString);
        }

        return joiner.toString();
    }

    public boolean matches(String value) {
        return values.contains(value);
    }
}
