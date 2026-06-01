package cz.mipit.sscc.ssc.compiler.data.var;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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


    public String toCString() {
        return "*" + String.join(" ", qualifiers);
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
