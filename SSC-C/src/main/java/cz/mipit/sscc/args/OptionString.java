package cz.mipit.sscc.args;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

public class OptionString {
    private final List<String> values;

    public OptionString(String... values) {
        if (values.length == 0) {
            throw new IllegalArgumentException("Must provide at least one value");
        }
        this.values = new ArrayList<>(Arrays.asList(values));
    }

    public String formatted(final boolean hasArgument) {
        final StringJoiner joiner = new StringJoiner(", ");
        for (final String value : values) {
            joiner.add(value + (hasArgument ? " ‹arg›" : ""));
        }

        return joiner.toString();
    }

    public boolean matches(String value) {
        Objects.requireNonNull(value);

        return values.contains(value);
    }
}
