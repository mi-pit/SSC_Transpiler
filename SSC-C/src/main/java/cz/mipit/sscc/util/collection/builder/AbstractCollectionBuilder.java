package cz.mipit.sscc.util.collection.builder;

import cz.mipit.sscc.util.collection.CollectionAdapter;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Abstract base for other collection builders. Not instantiable or visible outside the package.
 *
 * @param <ITEM> element type
 * @param <COLL> collection type (e.g. {@code List<ITEM>})
 * @param <SELF> self
 */
abstract class AbstractCollectionBuilder<
        SELF extends AbstractCollectionBuilder<SELF, COLL, ITEM>,
        COLL extends Collection<ITEM>,
        ITEM
        >
        extends CollectionAdapter<ITEM, COLL> {
    protected final Function<COLL, COLL> builder;

    /// builder is called in {@link AbstractCollectionBuilder#build()}
    protected AbstractCollectionBuilder(final Supplier<COLL> supplier,
                                        final Function<COLL, COLL> builder) {
        super(Objects.requireNonNull(supplier, "Collection supplier not provided").get());
        this.builder = Objects.requireNonNull(builder, "Collection builder not provided");

        Objects.requireNonNull(_collection, "Supplier didn't supply a collection");
    }

    /**
     * Returns the underlying collection after it's passed through the
     * {@link AbstractCollectionBuilder#builder} {@link Function} passed in the constructor.
     *
     * @return The built collection
     */
    public COLL build() {
        return builder.apply(_collection);
    }

    /**
     * @param value value to add
     * @return this
     */
    public SELF plus(ITEM value) {
        add(value);
        return self();
    }

    /**
     * @param values collection of values to add
     * @return this
     */
    public SELF plusMany(Collection<ITEM> values) {
        addAll(values);
        return self();
    }

    /**
     * @param values array of values to add
     * @return this
     */
    @SafeVarargs
    public final SELF plusMany(ITEM... values) {
        addAll(Arrays.asList(values));
        return self();
    }

    /**
     * Adds each value in the collection after running it through the {@code mapper} function.
     *
     * @param values Collection of some type
     * @param mapper (type of collection elements) -> (type of builder elements)
     * @param <O>    Arbitrary type
     * @return this
     */
    public <O> SELF plusMapped(final Iterable<O> values,
                               final Function<O, ITEM> mapper) {
        Objects.requireNonNull(values);
        Objects.requireNonNull(mapper);

        for (final O value : values) {
            add(mapper.apply(value));
        }
        return self();
    }

    /**
     * @return {@code this}
     */
    @SuppressWarnings("unchecked")
    protected SELF self() {
        return (SELF) this;
    }

    @Override
    public String toString() {
        return "CollectionBuilder(%s){%s}".formatted(_collection.getClass().getSimpleName(), _collection);
    }
}
