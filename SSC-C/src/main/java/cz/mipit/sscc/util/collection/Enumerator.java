package cz.mipit.sscc.util.collection;

import java.util.Iterator;

public interface Enumerator<T>
        extends
        Iterator<T>,
        Iterable<Enumerable.Entry<T>> {
}
