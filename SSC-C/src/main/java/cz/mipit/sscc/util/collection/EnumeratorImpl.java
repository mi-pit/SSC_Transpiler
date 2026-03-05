package cz.mipit.sscc.util.collection;

import java.util.Iterator;

public final class EnumeratorImpl<T> implements Enumerator<T> {
    private final Iterator<T> iterator;
    private int index;

    public EnumeratorImpl(Iterator<T> iterator) {
        this.iterator = iterator;
        index = 0;
    }

    @Override
    public Iterator<Enumerable.Entry<T>> iterator() {
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Enumerable.Entry<T> next() {
                return new Enumerable.Entry<>(index++, iterator.next());
            }
        };
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public T next() {
        return iterator.next();
    }
}
