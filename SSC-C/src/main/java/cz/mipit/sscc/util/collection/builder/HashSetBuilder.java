package cz.mipit.sscc.util.collection.builder;

import java.util.Collection;
import java.util.HashSet;

public final class HashSetBuilder<T> extends AbstractCollectionBuilder<HashSetBuilder<T>, HashSet<T>, T> {
    private HashSetBuilder() {
        super(HashSet::new, HashSet::new);
    }

    private HashSetBuilder(Collection<T> coll) {
        super(() -> new HashSet<>(coll), HashSet::new);
    }

    public static <I> HashSetBuilder<I> from(I item) {
        return new HashSetBuilder<I>().plus(item);
    }

    public static <I> HashSetBuilder<I> from(Collection<I> items) {
        return new HashSetBuilder<>(items);
    }

    public static <I> HashSetBuilder<I> empty() {
        return new HashSetBuilder<>();
    }
}
