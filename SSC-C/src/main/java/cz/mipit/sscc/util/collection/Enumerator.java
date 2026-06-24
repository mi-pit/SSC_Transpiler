package cz.mipit.sscc.util.collection;

import java.util.Collection;
import java.util.Iterator;

public final class Enumerator<T> implements Enumerable<T> {
    private final Iterator<T> iterator;
    private int index;

    private Enumerator(Iterator<T> iterator, int index) {
        this.iterator = iterator;
        this.index = index;
    }

    private Enumerator(Collection<T> coll, int index) {
        this(coll.iterator(), index);
    }

    private Enumerator(Iterator<T> iterator) {
        this(iterator, 0);
    }

    public static <T> Enumerator<T> from(Iterator<T> iterator) {
        return new Enumerator<>(iterator);
    }

    public static <T> Enumerator<T> of(Collection<T> collection) {
        return new Enumerator<>(collection.iterator());
    }

    public static <T> Enumerator<T> offset(Iterator<T> iterator, int offset) {
        return new Enumerator<>(iterator, offset);
    }

    public static <T> Enumerator<T> offset(Collection<T> collection, int offset) {
        return new Enumerator<>(collection, offset);
    }


    @Override
    public Iterator<Enumerated<T>> iterator() {
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Enumerated<T> next() {
                return new Enumerated<>(index++, iterator.next());
            }
        };
    }
}
