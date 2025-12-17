package cz.mipit.sscc.ssc.compiler.data;

import java.util.Objects;

public class SuperstructVariable extends Variable {
    private final String ssName;

    public SuperstructVariable(String ssName, boolean pointer, String name) {
        super(name, pointer);
        this.ssName = Objects.requireNonNull(ssName, "ssName");
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
        var that = (SuperstructVariable) obj;
        return Objects.equals(this.ssName, that.ssName) &&
                this.pointer == that.pointer &&
                Objects.equals(this.name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ssName, pointer, name);
    }

    @Override
    public String toString() {
        return "SuperstructVariable[" +
                "ssName=" + ssName + ", " +
                "pointer=" + pointer + ", " +
                "name=" + name + ']';
    }

}
