package cz.mipit.sscc.collection;

import cz.mipit.sscc.util.collection.Range;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestRange {
    @Test
    void minmax() {
        final Range zeroToTen = new Range(0, 10);
        assertEquals(0, zeroToTen.start);
        assertEquals(10, zeroToTen.end);
        assertEquals(10, zeroToTen.size());

        {
            int i = zeroToTen.start;
            for (final Integer v : zeroToTen) {
                assertEquals(i++, v);
            }
            assertEquals(i, zeroToTen.end);
            assertNotEquals(i, zeroToTen.end + 1);
        }

        final List<Integer> intsList = new ArrayList<>(zeroToTen.size());
        for (final Integer v : zeroToTen) {
            //noinspection UseBulkOperation
            intsList.add(v);
        }
        final List<Integer> wanted = List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
        assertEquals(
                List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9),
                intsList
        );
        assertArrayEquals(
                wanted.toArray(),
                intsList.toArray()
        );

        for (int i = zeroToTen.start; i < zeroToTen.end; i++) {
            assertTrue(zeroToTen.contains(i));
        }
        assertFalse(zeroToTen.contains(zeroToTen.size()));

        final Integer[] toArray = zeroToTen.toArray();
        final int[] array = zeroToTen.array();
        assertEquals(
                toArray.length,
                array.length
        );
        for (int i = 0; i < array.length; i++) {
            assertEquals(array[i], toArray[i]);
        }

        assertEquals(zeroToTen.size(), toArray.length);
        for (int j : toArray) {
            assertTrue(zeroToTen.contains(j));
        }

        final List<Integer> intLs = new ArrayList<>(zeroToTen);
        final Integer[] intLsAr = intLs.toArray(new Integer[0]);

        assertEquals(toArray.length, intLsAr.length);
        for (int i = 0; i < toArray.length; i++) {
            assertEquals(toArray[i], intLsAr[i]);
        }

        final Range ofSize10 = new Range(10);
        assertEquals(0, ofSize10.start);
        assertEquals(10, ofSize10.end);
        assertEquals(10, ofSize10.size());

        assertEquals(zeroToTen, ofSize10);
    }
}
