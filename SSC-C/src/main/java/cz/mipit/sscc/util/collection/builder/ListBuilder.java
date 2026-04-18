package cz.mipit.sscc.util.collection.builder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ListBuilder<T>
        extends AbstractCollectionBuilder<ListBuilder<T>, List<T>, T> {
    public static final int DEFAULT_CAPACITY = 16;

    private ListBuilder(Supplier<List<T>> supplier) {
        super(supplier, ListBuilder::listCopy);
    }

    private ListBuilder() {
        this(DEFAULT_CAPACITY);
    }

    private ListBuilder(int capacity) {
        this(() -> new ArrayList<>(capacity));
    }


    public static <T> ListBuilder<T> from(Collection<T> copyOf) {
        return new ListBuilder<T>(Math.max(copyOf.size(), DEFAULT_CAPACITY))
                .plusMany(copyOf);
    }

    public static <T> ListBuilder<T> with(T item) {
        return new ListBuilder<T>().plus(item);
    }

    public static <T> ListBuilder<T> empty() {
        return new ListBuilder<>();
    }

    public static <T> ListBuilder<T> withCapacity(int capacity) {
        return new ListBuilder<>(capacity);
    }


    public <O> ListBuilder<O> map(
            final Function<T, O> mapper
    ) {
        final ListBuilder<O> lb = ListBuilder.withCapacity(_collection.size());
        return lb.plusMapped(_collection, mapper);
    }

    /**
     * Returns an immutable list, built from the original by using {@link ListBuilder#listCopy(List)}.
     * This operation is idempotent, and has no effect on the original collection.
     *
     * @return a new immutable list
     */
    @Override
    public List<T> build() {
        // overridden for docs
        return super.build();
    }


    /**
     * This static function returns a copy of the original list.
     * <p>
     * Items may be null
     * </p>
     *
     * @param list list to be copied
     * @param <T>  item type
     * @return unmodifiable list with the items from the parameter
     */
    public static <T> List<T> listCopy(List<T> list) {
        return new ArrayList<>(Objects.requireNonNull(list, "no list to copy"));
    }
}
