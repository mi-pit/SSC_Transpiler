package cz.mipit.sscc.args;

import cz.mipit.sscc.util.annotations.Nullable;

import java.util.Objects;

import static cz.mipit.sscc.util.SSCCUtil.Text.INDENT;

public final class Option<T> {
    final OptionString strings;
    private final String name, description;
    private final @Nullable String argumentDescription;

    public final Class<T> type;
    public final T defaultValue;
    private T value;

    final NextOperation nextOperation;

    Option(OptionString optstr, String name,
           String description, String argument,
           Class<T> type, T defaultValue,
           NextOperation nextOperation) {
        this.strings = Objects.requireNonNull(optstr);
        this.name = Objects.requireNonNull(name);
        this.description = Objects.requireNonNull(description);
        this.argumentDescription = argument;

        this.type = type;
        this.defaultValue = defaultValue;
        value = defaultValue;

        this.nextOperation = nextOperation;
    }

    public String formatted() {
        final StringBuilder sBuilder = new StringBuilder()
                .append(INDENT)
                .append(name)
                .append(System.lineSeparator())

                .append(INDENT)
                .append(strings.formatted(argumentDescription != null))
                .append(System.lineSeparator())

                .append(INDENT)
                .append(INDENT)
                .append("> ")
                .append(description)
                .append(System.lineSeparator());

        if (argumentDescription != null) {
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

    @SuppressWarnings("unchecked")
    public void setValue(Object value) {
        if (value != null && value.getClass() != type) {
            throw new IllegalArgumentException(
                    "Cannot assign value of type '%s' to a field of type '%s'".formatted(value.getClass(), type)
            );
        }
        this.value = (T) value;
    }
}
