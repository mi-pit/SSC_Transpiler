package cz.mipit.sscc.ssc.compiler.data.var;

import antlr.ssc.SSCParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class Pointer {
    private static final Pointer CONST = new Pointer(List.of("const"));
    private static final List<Pointer> ONE_CONST = List.of(CONST);

    private static final Pointer BARE = new Pointer();
    private static final List<Pointer> ONE_BARE = List.of(BARE);


    private final List<String> qualifiers;

    private Pointer(final List<String> qualifiers) {
        this.qualifiers = qualifiers;
    }

    private Pointer() {
        this(List.of());
    }


    public static Pointer normal() {
        return BARE;
    }

    public static Pointer qualified(List<String> qualifiers) {
        return new Pointer(qualifiers);
    }


    public static List<Pointer> oneNormal() {
        return ONE_BARE;
    }

    public static List<Pointer> oneQualified(List<String> qualifiers) {
        return List.of(qualified(qualifiers));
    }

    public static List<Pointer> oneConst() {
        return ONE_CONST;
    }

    public static List<Pointer> none() {
        return List.of();
    }


    public static List<Pointer> combine(
            final List<Pointer> pointers,
            final List<Pointer> others
    ) {
        final List<Pointer> result = new ArrayList<>(pointers);
        result.addAll(others);

        return result;
    }

    public static List<Pointer> fromDeclarator(
            Function<SSCParser.TypeQualifierContext, String> qualifierStringizer,
            SSCParser.DeclaratorContext declarator
    ) {
        return declarator
                .pointer()
                .stream()
                .map(pointerCtx -> pointerCtx
                        .typeQualifierList()
                        .stream()
                        .flatMap(tqLs -> tqLs.typeQualifier().stream())
                        .map(qualifierStringizer)
                        .toList()
                )
                .map(Pointer::qualified)
                .toList();
    }


    public String toCString() {
        return "*" + String.join(" ", qualifiers);
    }

    public List<String> qualifiers() {
        return Collections.unmodifiableList(qualifiers);
    }

    @Override
    public String toString() {
        return "ptr={"
               + toCString()
               + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        Pointer pointer = (Pointer) o;
        return Objects.equals(qualifiers, pointer.qualifiers);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(qualifiers);
    }
}
