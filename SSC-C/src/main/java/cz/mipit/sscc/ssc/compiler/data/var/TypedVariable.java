package cz.mipit.sscc.ssc.compiler.data.var;

import java.util.List;

public class TypedVariable extends Variable {
    private final List<String> type;

    public TypedVariable(List<String> type, int pointer, String name) {
        super(name, pointer);
        this.type = type;
    }

    @Override
    public String getDeclarator() {
        return String.join(" ", type) + " " + abstractDeclarator();
    }
}
