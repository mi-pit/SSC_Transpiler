package cz.mipit.sscc.util.collection;

/**
 * Container which can hold any value
 *
 * @param <T> Any type
 */
public class Box<T> {
    public T item;

    public Box(T item) {
        this.item = item;
    }

    public Box() {
        this.item = null;
    }
}
