package cz.mipit.sscc.util.collection;

import java.util.Collection;
import java.util.Iterator;

/**
 * Public class for adapting a collection object.
 * <p>
 * This class is meant to be extended when the super class wants to implement all the functionality
 * of a collection by composition instead of inheritance,
 * basically extending it.
 * </p>
 *
 * @param <ITEM> item type
 */
public abstract class CollectionAdapter<ITEM> implements Collection<ITEM> {
    protected final Collection<ITEM> coll;

    protected CollectionAdapter(Collection<ITEM> coll) {
        this.coll = coll;
    }


    /* The rest is just Collection methods */

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

    @SuppressWarnings("SuspiciousMethodCalls")
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
