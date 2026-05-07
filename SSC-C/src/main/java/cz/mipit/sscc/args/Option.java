package cz.mipit.sscc.args;

import cz.mipit.sscc.util.annotations.Nullable;

import java.util.List;
import java.util.Objects;

import static cz.mipit.sscc.util.SSCCUtil.Text.INDENT;

final class Option<T> {
    private final OptionString strings;
    private final String name, description;
    private final @Nullable String argumentDescription;

    private final Class<T> type;
    private final T defaultValue;
    private T value;

    final NextOperation nextOperation;

    public Option(
            final OptionString optstr,
            final String name,
            final String description,
            final List<String> arguments,
            final Class<T> type,
            final @Nullable T defaultValue,
            final NextOperation nextOperation
    ) {
        this.strings = Objects.requireNonNull(optstr, "option string");
        this.name = Objects.requireNonNull(name, "name");
        this.description = Objects.requireNonNull(description, "description");
        this.argumentDescription = String.join(" ", arguments);

        this.type = Objects.requireNonNull(type, "type class");
        this.defaultValue = defaultValue;
        value = defaultValue;

        this.nextOperation = nextOperation;
    }

    public String formatted() {
        boolean hasArgument = !argumentDescription.isEmpty();

        final StringBuilder sBuilder = new StringBuilder()
                .append(INDENT)
                .append(name)
                .append(System.lineSeparator())

                .append(INDENT)
                .append(strings.formatted(hasArgument))
                .append(System.lineSeparator())

                .append(INDENT)
                .append(INDENT)
                .append("> ")
                .append(description)
                .append(System.lineSeparator());

        if (hasArgument) {
            sBuilder
                    .append(INDENT)
                    .append(INDENT)
                    .append("> arg: ")
                    .append(argumentDescription)
                    .append(System.lineSeparator());
        }

        return sBuilder.toString();
    }

    public void print() {
        System.out.println(formatted());
    }

    public T value() {
        return value;
    }

    public void setValue(Object value) {
        if (!type.isInstance(value)) {
            throw new IllegalArgumentException(
                    "Cannot assign value of type '%s' to a field of type '%s'".formatted(value.getClass(), type)
            );
        }
        this.value = type.cast(value);
    }

    public T defaultValue() {
        return defaultValue;
    }

    public boolean matches(String value) {
        return strings.matches(value);
    }
}
