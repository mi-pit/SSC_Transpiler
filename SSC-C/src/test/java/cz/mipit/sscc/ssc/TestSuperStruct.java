package cz.mipit.sscc.ssc;

import cz.mipit.sscc.ssc.compiler.data.ss.Field;
import cz.mipit.sscc.ssc.compiler.data.ss.SuperStruct;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestSuperStruct {
    public static final String SS_NAME = "List";

    @Test
    void name() {
        final SuperStruct struct = new SuperStruct(SS_NAME);

        assertEquals(SS_NAME, struct.name());

        assertTrue(struct.fields().isEmpty() && struct.methods().isEmpty());

        struct.addField(
                new Field(
                        true,
                        "int num",
                        "num"
                )
        );

        assertFalse(struct.fields().isEmpty());
        assertTrue(struct.methods().isEmpty());
    }
}
