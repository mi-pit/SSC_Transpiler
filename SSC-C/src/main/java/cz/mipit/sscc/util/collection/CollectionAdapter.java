package cz.mipit.sscc.util.collection;

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
public abstract class CollectionAdapter<ITEM, COLL extends java.util.Collection<ITEM>>
        implements java.util.Collection<ITEM>, Enumerable<ITEM> {
    protected final COLL _collection;

    protected CollectionAdapter(COLL _collection) {
        this._collection = _collection;
    }


    /* The rest is just Collection methods */

    @Override
    public int size() {
        return _collection.size();
    }

    @Override
    public boolean isEmpty() {
        return _collection.isEmpty();
    }

    @Override
    public void clear() {
        _collection.clear();
    }

    @Override
    public boolean contains(Object o) {
        return _collection.contains(o);
    }

    @Override
    public Object[] toArray() {
        return _collection.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return _collection.toArray(a);
    }

    @Override
    public boolean add(ITEM item) {
        return _collection.add(item);
    }

    @Override
    public boolean remove(Object o) {
        return _collection.remove(o);
    }

    @Override
    public boolean containsAll(java.util.Collection<?> c) {
        return _collection.containsAll(c);
    }

    @Override
    public boolean addAll(java.util.Collection<? extends ITEM> c) {
        return _collection.addAll(c);
    }

    @Override
    public boolean removeAll(java.util.Collection<?> c) {
        return _collection.removeAll(c);
    }

    @Override
    public boolean retainAll(java.util.Collection<?> c) {
        return _collection.retainAll(c);
    }


    @Override
    public Iterator<ITEM> iterator() {
        return _collection.iterator();
    }

    @SuppressWarnings("EqualsDoesntCheckParameterClass")
    @Override
    public boolean equals(Object o) {
        return _collection.equals(o);
    }

    @Override
    public int hashCode() {
        return _collection.hashCode();
    }

    @Override
    public Enumerator<ITEM> enumerator() {
        return new EnumeratorImpl<>(iterator());
    }
}
