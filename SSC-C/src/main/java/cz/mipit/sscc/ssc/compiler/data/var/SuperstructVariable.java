package cz.mipit.sscc.ssc.compiler.data.var;

import java.util.Objects;

public class SuperstructVariable extends Variable {
    private final String ssName;

    public SuperstructVariable(String ssName, int pointer, String name) {
        super(name, pointer);
        this.ssName = Objects.requireNonNull(ssName);
    }

    public String ssName() {
        return ssName;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || obj.getClass() != this.getClass())
            return false;
        SuperstructVariable that = (SuperstructVariable) obj;
        return Objects.equals(this.ssName, that.ssName) &&
                this.pointer == that.pointer &&
                Objects.equals(this.identifier, that.identifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ssName, pointer, identifier);
    }

    @Override
    public String toString() {
        return "SuperstructVariable[" +
                "ssName=" + ssName + ", " +
                "pointer=" + pointer + ", " +
                "name=" + identifier + ']';
    }
}
