package cz.mipit.sscc.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;

public final class Range implements Collection<Integer> {
    public final int start, end;
    private final int size;

    public static int[] array(int start, int end) {
        final int size = end - start;
        final int[] array = new int[size];
        for (int i = 0; i < size; i++) {
            array[i] = start + i;
        }

        return array;
    }

    public Range(int start, int end) {
        if (start > end) {
            throw new IllegalArgumentException("start > end");
        }
        this.start = start;
        this.end = end;

        size = end - start;
    }

    public Range(int size) {
        this(0, size);
    }


    public int size() {
        return size;
    }

    public boolean contains(Object o) {
        if (o instanceof Number n) {
            return n.doubleValue() >= start && n.doubleValue() <= end;
        }
        return false;
    }

    public Iterator<Integer> iterator() {
        return new Iter();
    }

    public Integer[] toArray() {
        final Integer[] array = new Integer[size];

        int i = 0;
        for (final Integer n : this) {
            assert n != null;
            array[i++] = n;
        }
        return array;
    }

    public int[] array() {
        return Range.array(start, end);
    }


    @Override
    public boolean equals(Object o) {
        if (o instanceof Range range) {
            return start == range.start && end == range.end;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end);
    }


    private class Iter implements Iterator<Integer> {
        int curr;

        @Override
        public boolean hasNext() {
            return curr < end;
        }

        @Override
        public Integer next() {
            return curr++;
        }
    }


    @Override
    public boolean isEmpty() {
        return start == end;
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return null;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object o : c) {
            if (!contains(o)) {
                return false;
            }
        }
        return true;
    }


    @Override
    public boolean add(Integer integer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends Integer> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }
}
