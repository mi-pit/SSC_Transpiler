package cz.mipit.sscc.ssc;

import cz.mipit.sscc.ssc.compiler.data.var.Pointer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestPointer {
    @Test
    void testToCString() {
        final Pointer p1 = Pointer.qualified(List.of("const", "volatile"));
        assertEquals("*const volatile", p1.toCString().trim());

        final Pointer p1_copy = Pointer.qualified(List.of("const", "volatile"));
        assertEquals(p1.toCString(), p1_copy.toCString());

        final Pointer p2 = Pointer.normal();
        assertEquals("*", p2.toCString());

        final List<Pointer> ls1 = Pointer.oneConst();
        final List<Pointer> ls2 = Pointer.oneQualified(List.of("const", "volatile"));
        assertNotEquals(ls1.getFirst(), ls2.getFirst());

        final List<Pointer> lsC = Pointer.combine(ls1, ls2);
        assertEquals(2, lsC.size());
        for (final Pointer p : lsC) {
            assertTrue(p.toCString().contains("const"));
        }

        for (final Pointer p : Pointer.combine(Pointer.combine(lsC, lsC), Pointer.combine(lsC, lsC))) {
            assertTrue(p.toCString().contains("const"));
            assertFalse(p.toCString().contains("_Nonnull"));
        }
    }
}
