package cz.muni.fi.sscc.data;

import java.util.Objects;

public class Field {
    private final boolean isPrivate;
    private final String data;

    public Field(boolean isPrivate, String data) {
        this.isPrivate = isPrivate;
        this.data = Objects.requireNonNull(data);
    }

    public String getData() {
        return data;
    }
}
