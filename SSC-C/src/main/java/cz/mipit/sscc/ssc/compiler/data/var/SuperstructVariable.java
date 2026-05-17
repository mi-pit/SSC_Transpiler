package cz.mipit.sscc.ssc.compiler.data.var;

import java.util.List;
import java.util.Objects;

public class SuperstructVariable extends Variable {
    private final String ssName;

    public SuperstructVariable(String ssName, List<Pointer> pointer, String name) {
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
                this.pointers == that.pointers &&
                Objects.equals(this.identifier, that.identifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ssName, pointers, identifier);
    }

    @Override
    public String toString() {
        return "SuperstructVariable[" +
                "ssName=" + ssName + ", " +
                "pointer=" + pointers + ", " +
                "name=" + identifier + ']';
    }
}
