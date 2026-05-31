package cz.mipit.sscc.ssc.compiler.data.ss;

import cz.mipit.sscc.util.SSCCUtil;
import cz.mipit.sscc.util.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class SuperStruct {
    private final String name;

    private final List<Field> fields;
    private final List<SuperstructMethod> methods;

    public SuperStruct(@NotNull final String name) {
        this.name = Objects.requireNonNull(name);

        this.fields = new ArrayList<>();
        this.methods = new ArrayList<>();
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
                    .add(fnDef.getDeclaration());
        }
        return resultBuilder.toString();
    }

    public String emitMethodDefinitions() {
        final StringJoiner resultBuilder = new StringJoiner(System.lineSeparator());
        for (final SuperstructMethod fnDef : methods) {
            fnDef.getDefinition()
                    .ifPresent(resultBuilder::add);
        }
        return resultBuilder.toString();
    }

    public List<SuperstructMethod> methods() {
        return methods;
    }

    public String name() {
        return name;
    }

    public List<Field> fields() {
        return fields;
    }

    public void addFunction(SuperstructMethod fn) {
        methods.add(fn);
    }

    public void addField(Field field) {
        fields.add(field);
    }

    public Optional<SuperstructMethod> findMethod(final String methodName) {
        for (final SuperstructMethod func : this.methods()) {
            if (func.getUnqualifiedName().equals(methodName)) {
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
                            methods: %s""",
                name, fields, methods
        );
    }
}
