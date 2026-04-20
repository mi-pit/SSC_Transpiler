package cz.mipit.sscc.args;

import java.util.Arrays;
import java.util.Objects;
import java.util.SequencedSet;
import java.util.StringJoiner;
import java.util.TreeSet;

class OptionString {
    private final SequencedSet<String> values;

    public OptionString(final String... values) {
        if (Objects.requireNonNull(values).length == 0) {
            throw new IllegalArgumentException("Must provide at least one value");
        }

        // throw if an element is null
        this.values = new TreeSet<>(Arrays.asList(values));
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
