package cz.mipit.sscc.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * Can be converted to list with `build`.
 *
 * @param <T> element type
 */
public final class ListBuilder<T> {
    public static final int DEFAULT_CAPACITY = 16;

    private final List<T> ls;

    private ListBuilder(int capacity) {
        ls = new ArrayList<>(capacity);
    }

    private ListBuilder() {
        this(DEFAULT_CAPACITY);
    }

    public static <T> ListBuilder<T> from(Collection<T> copyOf) {
        return new ListBuilder<T>(Math.max(copyOf.size(), DEFAULT_CAPACITY)).addAll(copyOf);
    }

    public static <T> ListBuilder<T> from(T item) {
        return new ListBuilder<T>().add(item);
    }

    public static <T> ListBuilder<T> empty() {
        return new ListBuilder<>();
    }

    public static <T> ListBuilder<T> withCapacity(int capacity) {
        return new ListBuilder<T>(capacity);
    }

    /**
     * @param value value to add
     * @return this
     */
    public ListBuilder<T> add(T value) {
        ls.add(value);
        return this;
    }

    /**
     * @param values collection of values to add
     * @return this
     */
    public ListBuilder<T> addAll(Collection<T> values) {
        ls.addAll(values);
        return this;
    }

    /**
     * @param values array of values to add
     * @return this
     */
    @SafeVarargs
    public final ListBuilder<T> add(T... values) {
        for (T value : values) {
            add(value);
        }
        return this;
    }

    /**
     * Adds each value in the collection after running it through the {@code mapper} function.
     *
     * @param values Collection of some type
     * @param mapper (type of collection elements) -> (type of builder elements)
     * @param <O>    Arbitrary type
     * @return this
     */
    public <O> ListBuilder<T> addMapped(Collection<O> values, Function<O, T> mapper) {
        for (O value : values) {
            add(mapper.apply(value));
        }

        return this;
    }

    /**
     * @return An immutable list with the elements added while building
     */
    public List<T> build() {
        return Collections.unmodifiableList(ls);
    }
}
