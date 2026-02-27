package cz.mipit.sscc.util.collection;

import java.util.Collection;
import java.util.Iterator;

public class CollectionAdapter<ITEM> implements Collection<ITEM> {
    protected final Collection<ITEM> coll;

    protected CollectionAdapter(Collection<ITEM> coll) {
        this.coll = coll;
    }

    @Override
    public int size() {
        return coll.size();
    }

    @Override
    public boolean isEmpty() {
        return coll.isEmpty();
    }

    @Override
    public void clear() {
        coll.clear();
    }

    @Override
    public boolean contains(Object o) {
        return coll.contains(o);
    }

    @Override
    public Object[] toArray() {
        return coll.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return coll.toArray(a);
    }

    @Override
    public boolean add(ITEM item) {
        return coll.add(item);
    }

    @Override
    public boolean remove(Object o) {
        return coll.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return coll.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends ITEM> c) {
        return coll.addAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return coll.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return coll.removeAll(c);
    }


    @Override
    public Iterator<ITEM> iterator() {
        return coll.iterator();
    }

    @SuppressWarnings("EqualsDoesntCheckParameterClass")
    @Override
    public boolean equals(Object o) {
        return coll.equals(o);
    }

    @Override
    public int hashCode() {
        return coll.hashCode();
    }
}
