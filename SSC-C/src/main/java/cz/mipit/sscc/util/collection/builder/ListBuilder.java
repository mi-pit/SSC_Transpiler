package cz.mipit.sscc.util.collection.builder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ListBuilder<T> extends AbstractCollectionBuilder<ListBuilder<T>, List<T>, T> {
    public static final int DEFAULT_CAPACITY = 16;

    private ListBuilder(Supplier<List<T>> supplier) {
        super(supplier, List::copyOf);
    }

    private ListBuilder() {
        this(ArrayList::new);
    }

    private ListBuilder(int capacity) {
        this(() -> new ArrayList<>(capacity));
    }


    public static <T> ListBuilder<T> from(Collection<T> copyOf) {
        return new ListBuilder<T>(Math.max(copyOf.size(), DEFAULT_CAPACITY)).plusMany(copyOf);
    }

    public static <T> ListBuilder<T> from(T item) {
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
        return ListBuilder.<O>withCapacity(coll.size()).plusMapped(coll, mapper);
    }
}
