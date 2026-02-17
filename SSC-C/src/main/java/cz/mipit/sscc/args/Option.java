package cz.mipit.sscc.args;

import java.util.Objects;
import java.util.Optional;

public final class Option {
    private final String optString, name, description;

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final Optional<String> argument;

    private Option(String optString, String name, String description, String argumentsNullable) {
        this.optString = Objects.requireNonNull(optString);
        this.name = Objects.requireNonNull(name);
        this.description = Objects.requireNonNull(description);
        this.argument = Optional.ofNullable(argumentsNullable);
    }

    public static Option of(String optString, String name,
                            String description, String args) {
        return new Option(optString, name, description, args);
    }

    public String formatted() {
        final StringBuilder sBuilder = new StringBuilder("    ")
                .append(optString);

        argument.ifPresent(ignored -> sBuilder.append("‹arg›"));

        sBuilder
                .append("\t\"")
                .append(name)
                .append('\"')
                .append(System.lineSeparator())
                .append("        > '")
                .append(description)
                .append("'")
                .append(System.lineSeparator());

        argument.ifPresent(arg -> sBuilder
                .append("        > arg='")
                .append(arg)
                .append("'")
                .append(System.lineSeparator()));

        return sBuilder.toString();
    }

    public void print() {
        System.out.println(formatted());
    }
}
