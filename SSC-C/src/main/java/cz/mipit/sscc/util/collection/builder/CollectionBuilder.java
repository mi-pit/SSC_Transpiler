package cz.mipit.sscc.util.collection.builder;

import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;

public final class CollectionBuilder<C extends Collection<I>, I> extends AbstractCollectionBuilder<CollectionBuilder<C, I>, C, I> {
    public CollectionBuilder(Supplier<C> supplier, Function<C, C> getter) {
        super(supplier, getter);
    }
}
