package cz.mipit.sscc.ssc.compiler.data.ss;

import java.util.List;
import java.util.Objects;

public class Field {
    private final boolean isPrivate;

    private final List<String> type;
    private final boolean ptr;
    private final String name;

    public Field(boolean isPrivate, List<String> type, boolean ptr, String name) {
        this.isPrivate = isPrivate;
        this.type = Objects.requireNonNull(type);
        this.ptr = ptr;
        this.name = Objects.requireNonNull(name);
    }

    public String getWhole() {
        return String.join(" ", type) + " " + (ptr ? "*" : "") + name;
    }

    public String getName() {
        return name;
    }

    public List<String> getType() {
        return type;
    }

    public boolean isPrivate() {
        return isPrivate;
    }
}
