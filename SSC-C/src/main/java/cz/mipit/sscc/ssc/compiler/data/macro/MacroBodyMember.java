package cz.mipit.sscc.ssc.compiler.data.macro;

import java.util.Objects;

public final class MacroBodyMember {
    private final String token;
    private final boolean isArg;

    public MacroBodyMember(boolean isArg, String token) {
        this.isArg = isArg;
        this.token = Objects.requireNonNull(token);
    }

    public String token() {
        return token;
    }

    public boolean isArgument() {
        return isArg;
    }

    @Override
    public String toString() {
        return token;
    }
}
