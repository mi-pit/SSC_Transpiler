package cz.mipit.sscc.ssc.compiler.data.ss;

import cz.mipit.sscc.Main;
import cz.mipit.sscc.util.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SuperStruct {
    private final String name;

    private final List<Field> fields;
    private final List<Function> functions;

    public SuperStruct(@NotNull final String name) {
        this.name = Objects.requireNonNull(name);

        this.fields = new ArrayList<>();
        this.functions = new ArrayList<>();
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
                    .append("    ")
                    .append(field.getWhole())
                    .append(";")
                    .append(System.lineSeparator());
        }
        resultBuilder.append("};");

        return resultBuilder.toString();
    }

    public String emitMethodDeclarations() {
        return emitMethods(Function::getDeclaration);
    }

    public String emitMethodDefinitions() {
        return emitMethods(fd -> fd.getDefinition().orElse(""));
    }

    private String emitMethods(final java.util.function.Function<Function, String> functionFunction) {
        final StringBuilder resultBuilder = new StringBuilder();
        for (final Function fnDef : functions) {
            resultBuilder
                    .append(functionFunction.apply(fnDef))
                    .append(System.lineSeparator());
        }
        return resultBuilder.toString();
    }

    public List<Function> functions() {
        return functions;
    }

    public String name() {
        return name;
    }

    public List<Field> fields() {
        return fields;
    }

    public void addFunction(Function fn) {
        functions.add(fn);
    }

    public void addField(Field field) {
        fields.add(field);
    }

    public Optional<Function> findMethod(final String methodName) {
        for (final Function func : this.functions()) {
            if (func.getUnqualifiedName().equals(methodName)) {
                return Optional.of(func);
            }
        }

        return Optional.empty();
    }
}
