package cz.mipit.sscc.args;

import cz.mipit.sscc.util.ListBuilder;
import cz.mipit.sscc.util.annotations.NotNull;
import cz.mipit.sscc.util.annotations.Nullable;

import java.util.Objects;
import java.util.stream.Collectors;

import static cz.mipit.sscc.util.SSCCUtil.Text.INDENT;

public final class Option {
    private final String optStringShort;
    private final String optStringLong;
    private final String name, description;

    private final String argument;

    private Option(String optStringShort, String optStringLong,
                   String name, String description, String arguments) {
        if (optStringShort == null && optStringLong == null) {
            throw new IllegalArgumentException("No option string provided");
        }

        this.optStringShort = optStringShort;
        this.optStringLong = optStringLong;
        this.name = Objects.requireNonNull(name);
        this.description = Objects.requireNonNull(description);

        this.argument = arguments;
    }

    public static Option of(@Nullable String optStringShort, @Nullable String optStringLong,
                            @NotNull String name, @NotNull String description,
                            @Nullable String argument) {
        return new Option(optStringShort, optStringLong, name, description, argument);
    }

    public static Option of(@Nullable String optString, @NotNull String name,
                            @NotNull String description, @Nullable String argument) {
        return new Option(optString, null, name, description, argument);
    }

    public String formatted() {
        final StringBuilder sBuilder = new StringBuilder();

        sBuilder.append(INDENT)
                .append(name)
                .append(System.lineSeparator())
                .append(INDENT);

        sBuilder.append(
                ListBuilder.<String>withCapacity(2)
                        .add(optStringShort)
                        .add(optStringLong)
                        .build()
                        .stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining(", "))
        );

        if (argument != null) {
            sBuilder.append("‹arg›");
        }

        sBuilder
                .append(System.lineSeparator())
                .append(INDENT)
                .append(INDENT)
                .append("> ")
                .append(description)
                .append(System.lineSeparator());

        if (argument != null) {
            sBuilder
                    .append(INDENT)
                    .append(INDENT)
                    .append("> arg='")
                    .append(argument)
                    .append("'")
                    .append(System.lineSeparator());
        }

        return sBuilder.toString();
    }

    public void print() {
        System.out.println(formatted());
    }
}
