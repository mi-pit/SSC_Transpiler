package cz.mipit.sscc.util.collection.builder;

import cz.mipit.sscc.util.collection.CollectionAdapter;

import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Abstract base for other collection builders
 *
 * @param <ITEM> element type
 * @param <COLL> collection type
 * @param <SELF> self
 */
class AbstractCollectionBuilder<
        SELF extends AbstractCollectionBuilder<SELF, COLL, ITEM>,
        COLL extends Collection<ITEM>,
        ITEM
        >
        extends CollectionAdapter<ITEM> {
    protected final Function<COLL, COLL> builder;

    /// builder is called in {@link AbstractCollectionBuilder#build()}
    protected AbstractCollectionBuilder(final Supplier<COLL> supplier,
                                        final Function<COLL, COLL> builder) {
        super(supplier.get());
        this.builder = builder;
    }

    public COLL build() {
        return collect(builder);
    }

    /**
     * Collects items from this builder to one of another type.
     *
     * @return a collection of the same type of item
     */
    @SuppressWarnings("unchecked")
    public <OTHER_COLL extends Collection<ITEM>>
    OTHER_COLL collect(
            final Function<COLL, OTHER_COLL> getter
    ) {
        /* ad `@SuppressWarnings("unchecked")`:
         * COLL extends Collection<ITEM>
         * type of coll is Collection<ITEM>
         */
        return getter.apply((COLL) coll);
    }

    /**
     * @param value value to add
     * @return this
     */
    @SuppressWarnings("unchecked")
    public SELF plus(ITEM value) {
        coll.add(value);
        return (SELF) this;
    }

    /**
     * @param values collection of values to add
     * @return this
     */
    @SuppressWarnings("unchecked")
    public SELF plusMany(Collection<ITEM> values) {
        coll.addAll(values);
        return (SELF) this;
    }

    /**
     * @param values array of values to add
     * @return this
     */
    @SuppressWarnings("unchecked")
    public final SELF plusMany(ITEM... values) {
        for (final ITEM value : values) {
            plus(value);
        }
        return (SELF) this;
    }

    /**
     * Adds each value in the collection after running it through the {@code mapper} function.
     *
     * @param values Collection of some type
     * @param mapper (type of collection elements) -> (type of builder elements)
     * @param <O>    Arbitrary type
     * @return this
     */
    @SuppressWarnings("unchecked")
    public <O> SELF plusMapped(Iterable<O> values, Function<O, ITEM> mapper) {
        for (final O value : values) {
            plus(mapper.apply(value));
        }

        return (SELF) this;
    }

    @Override
    public String toString() {
        return String.format("CollectionBuilder(%s){%s}", coll.getClass(), coll);
    }
}
