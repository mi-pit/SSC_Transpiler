package cz.mipit.sscc.util.collection;

public interface Enumerable<T> {
    Enumerator<T> enumerator();

    record Entry<E>(int index, E item) {
    }
}
