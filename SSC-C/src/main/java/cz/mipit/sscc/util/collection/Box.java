package cz.mipit.sscc.util.collection;

import java.util.Objects;

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

    /// Initialized to null
    public Box() {
        this.item = null;
    }


    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        Box<?> box = (Box<?>) o;
        return Objects.equals(item, box.item);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(item);
    }

    @Override
    public String toString() {
        return item.toString();
    }
}
