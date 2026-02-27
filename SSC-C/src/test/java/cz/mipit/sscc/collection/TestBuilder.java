package cz.mipit.sscc.collection;

import cz.mipit.sscc.util.collection.builder.CollectionBuilder;
import cz.mipit.sscc.util.collection.builder.HashSetBuilder;
import cz.mipit.sscc.util.collection.builder.ListBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class TestBuilder {
    @Test
    void listBuilder() {
        final ListBuilder<Void> empty = ListBuilder.empty();
        Assertions.assertTrue(empty.build().isEmpty());

        final ListBuilder<Integer> ints = ListBuilder.withCapacity(16);
        Assertions.assertTrue(ints.build().isEmpty());

        Assertions.assertSame(ints, ints.plus(0));

        ints
                .plus(1)
                .plus(2)
                .plus(3);

        final List<Integer> firstBuilt = ints.build();
        for (int i = 0; i < firstBuilt.size(); i++) {
            Assertions.assertEquals(i, firstBuilt.get(i));
        }

        {
            final List<Integer> ls = new ArrayList<>();
            for (int i = 10; i < 20; i++) {
                ls.add(i);
            }

            ints.plusMany(ls);
        }

        {
            final List<Double> doublesList = new ArrayList<>();
            for (int i = 10; i < 20; i++) {
                double v = i * 1.2345;
                doublesList.add(v);
            }
            ints.plusMapped(doublesList, Double::intValue);
        }

        Assertions.assertIterableEquals(
                List.of(0, 1, 2, 3, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 12, 13, 14, 16, 17, 18, 19, 20, 22, 23),
                ints
        );

        Assertions.assertEquals(
                List.of(0, 1, 2, 3),
                firstBuilt,
                "list built earlier should be independent from the builder");

        final HashSet<Integer> secondBuilt = ints.collect(HashSet::new);
        for (final Integer i : ints) {
            Assertions.assertNotNull(i);
            Assertions.assertTrue(secondBuilt.contains(i));
        }

        ints.clear();
        Assertions.assertTrue(ints.isEmpty());
        Assertions.assertEquals(0, ints.size());

        Assertions.assertEquals(4, firstBuilt.size(),
                "list built earlier should be independent from the builder");
    }

    @Test
    void setBuilder() {
        final Set<String> inputSet = Set.of("String1", "Second", "3rd", "Last");

        final HashSetBuilder<String> stringsSetBuilder = HashSetBuilder
                .from(inputSet)
                .plus("Added in builder");
        Assertions.assertFalse(stringsSetBuilder.isEmpty());

        final Set<String> built = stringsSetBuilder.build();
        Assertions.assertEquals(5, built.size());
        for (final String s : inputSet) {
            Assertions.assertTrue(built.contains(s));
        }
    }

    @Test
    void builder() {
        final List<String> inputList = List.of("B", "a", "b", "c");

        final CollectionBuilder<List<String>, String> builder = new CollectionBuilder<>(ArrayList::new, ArrayList::new);
        Assertions.assertTrue(builder.isEmpty());
        Assertions.assertNotSame(builder.build(), builder.build());

        final List<String> built = builder
                .plus("B")
                .plusMapped(List.of("A", "B", "C"), String::toLowerCase)
                .build();

        Assertions.assertTrue(builder.contains("c"));
        Assertions.assertFalse(builder.contains("STRING"));
        Assertions.assertFalse(builder.contains(Integer.valueOf("123")));

        Assertions.assertEquals(inputList, built);
    }

    @Test
    void map() {
        final ListBuilder<Integer> ints = ListBuilder.from(1).plus(2).plus(3);
        final ListBuilder<Long> longs = ints.map(i -> (long) i);
        final ListBuilder<Double> doubles = ints.map(i -> (double) i);
        final ListBuilder<String> strings = ints.map(String::valueOf);

        Assertions.assertTrue(
                ints.size() == longs.size()
                        && ints.size() == doubles.size()
                        && ints.size() == strings.size()
        );

        final List<Integer> intsList = ints.build();
        final List<Long> longsList = longs.build();
        final List<Double> doublesList = doubles.build();
        final List<String> stringsList = strings.build();

        for (final Integer i : ints) {
            Assertions.assertNotNull(i);

            Assertions.assertTrue(intsList.contains(i));
            Assertions.assertTrue(longsList.contains((long) i));
            Assertions.assertTrue(doublesList.contains((double) i));
            Assertions.assertTrue(stringsList.contains(String.valueOf(i)));
        }
    }
}
