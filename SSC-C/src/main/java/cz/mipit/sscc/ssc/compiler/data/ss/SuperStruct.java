package cz.mipit.sscc.ssc.compiler.data.ss;

import cz.mipit.sscc.ssc.exceptions.SSCTranspilerException;
import cz.mipit.sscc.util.SSCCUtil;
import cz.mipit.sscc.util.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SuperStruct {
    private final String name;

    private final ArrayList<Field> fields;
    private final ArrayList<SuperstructMethod> methods;

    private boolean isDefined;

    public SuperStruct(@NotNull final String name) {
        this.name = Objects.requireNonNull(name);

        this.fields = new ArrayList<>();
        this.methods = new ArrayList<>();

        this.isDefined = false;
    }

    public boolean isDefined() {
        return isDefined;
    }

    public void setDefined() {
        isDefined = true;
    }

    public String qualifyName(String unqualifiedName) {
        return "__ssc_ss_" + this.name + "__" + unqualifiedName;
    }


    public String emitStructDeclaration() {
        return String.format("struct %s;%n", name);
    }

    public String emitStructDefinition() {
        final StringBuilder resultBuilder = new StringBuilder();

        resultBuilder.append(String.format("struct %s {%n", name));
        for (Field field : fields) {
            resultBuilder
                    /* do a little bit of formatting for mid-compilation error messages */
                    .append(SSCCUtil.Text.INDENT)
                    .append(field.getWhole())
                    .append(";")
                    .append(System.lineSeparator());
        }
        resultBuilder.append("};");

        return resultBuilder.toString();
    }

    public String emitMethodDeclarations() {
        final StringJoiner resultBuilder = new StringJoiner(System.lineSeparator());
        for (final SuperstructMethod fnDef : methods) {
            resultBuilder
                    .add(fnDef.header() + ";");
        }
        return resultBuilder.toString();
    }

    public String emitMethodDefinitions(Function<SuperstructMethod, SSCTranspilerException> exceptionGetter) {
        final StringJoiner resultBuilder = new StringJoiner(System.lineSeparator());
        for (final SuperstructMethod fnDef : methods) {
            final Optional<String> def = fnDef.definition();

            if (def.isEmpty()) {
                throw exceptionGetter.apply(fnDef);
            }

            resultBuilder.add(def.get());
        }
        return resultBuilder.toString();
    }

    public String name() {
        return name;
    }

    public List<SuperstructMethod> methods() {
        return methods;
    }

    public List<Field> fields() {
        return fields;
    }

    public void declareMethod(SuperstructMethod fn) {
        if (fn.definition().isPresent()) {
            throw new IllegalStateException("Declaring a defined method");
        }
        methods.add(fn);
    }

    public void defineMethod(SuperstructMethod fn) {
        final String name = fn.name();
        assert name != null;

        for (int i = 0; i < methods.size(); i++) {
            final SuperstructMethod m = methods.get(i);
            if (name.equals(m.name())) {
                methods.set(i, fn);
                return;
            }
        }

        methods.add(fn);
    }

    public void addField(Field field) {
        fields.add(field);
    }

    public Optional<SuperstructMethod> findMethod(final String methodName) {
        for (final SuperstructMethod func : this.methods()) {
            if (func.name().equals(methodName)) {
                return Optional.of(func);
            }
        }

        return Optional.empty();
    }


    @Override
    public String toString() {
        final String fields = this.fields.stream().map(Field::toString).collect(Collectors.joining("; "));
        final String methods = this.methods.stream().map(SuperstructMethod::toString).collect(Collectors.joining("; "));
        return String.format("""
                        SuperStruct: '%s'
                            fields:  %s
                            methods: %s
                        """,
                name, fields, methods
        );
    }
}
