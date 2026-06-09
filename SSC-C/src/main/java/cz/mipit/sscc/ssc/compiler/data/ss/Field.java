package cz.mipit.sscc.ssc.compiler.data.ss;

public class Field {
    private final boolean isPrivate;
    private final String declaration;
    private final String identifier;

    public Field(
            final boolean isPrivate,
            final String declarationSpecifiers,
            final String declarator,
            final String identifier
    ) {
        this.isPrivate = isPrivate;

        this.declaration = declarationSpecifiers + " " + declarator;

        this.identifier = identifier;
    }

    public String getWhole() {
        return declaration;
    }

    public String getName() {
        return identifier;
    }

    public boolean isPrivate() {
        return isPrivate;
    }


    @Override
    public String toString() {
        return "Field{" + getWhole() + "}";
    }
}
