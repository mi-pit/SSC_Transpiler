package cz.mipit.sscc.ssc.exceptions;

import cz.mipit.sscc.util.Range;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestRange {
    @Test
    void minmax() {
        final Range range = new Range(0, 10);
        assertEquals(0, range.start);
        assertEquals(10, range.end);
        assertEquals(10, range.size());

        {
            int i = range.start;
            for (Integer r : range) {
                assertEquals(i++, r);
            }
            assertEquals(i, range.end);
            assertNotEquals(i, range.end + 1);
        }

        for (int i = range.start; i < range.end; i++) {
            assertTrue(range.contains(i));
        }
        assertFalse(range.contains(range.size()));

        final Integer[] ar = range.toArray();
        assertEquals(range.size(), ar.length);
        for (int j : ar) {
            assertTrue(range.contains(j));
        }

        final List<Integer> intLs = new ArrayList<>(range);
        final Integer[] intLsAr = intLs.toArray(new Integer[0]);

        assertEquals(ar.length, intLsAr.length);
        for (int i = 0; i < ar.length; i++) {
            assertEquals(ar[i], intLsAr[i]);
        }
    }
}
