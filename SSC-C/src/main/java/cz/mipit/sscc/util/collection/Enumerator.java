package cz.mipit.sscc.util.collection;

import java.util.Collection;
import java.util.Iterator;

public final class Enumerator<T> implements Enumerable<T> {
    private final Iterator<T> iterator;
    private int index;

    public Enumerator(Iterator<T> iterator) {
        this.iterator = iterator;
        index = 0;
    }

    public Enumerator(Collection<T> collection) {
        this.iterator = collection.iterator();
        index = 0;
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
