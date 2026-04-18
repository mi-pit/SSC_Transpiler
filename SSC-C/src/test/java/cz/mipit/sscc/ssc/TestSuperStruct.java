package cz.mipit.sscc.ssc;

import cz.mipit.sscc.ssc.compiler.data.ss.Field;
import cz.mipit.sscc.ssc.compiler.data.ss.FunctionDefinition;
import cz.mipit.sscc.ssc.compiler.data.ss.SSMember;
import cz.mipit.sscc.ssc.compiler.data.ss.SuperStruct;
import cz.mipit.sscc.ssc.compiler.data.var.TypedVariable;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestSuperStruct {
    public static final String SS_NAME = "List";

    @Test
    void name() {
        final SuperStruct struct = new SuperStruct(SS_NAME);

        assertEquals(SS_NAME, struct.name());

        final List<FunctionDefinition> functions = struct.getFunctions();
        assertTrue(functions == null || functions.isEmpty());

        final List<SSMember> members = struct.members();
        assertTrue(members == null || members.isEmpty());

        struct.addMember(
                SSMember.field(
                        new Field(
                                true,
                                new TypedVariable(List.of("int"), 0, "num")
                        )
                )
        );

        assertFalse(struct.members().isEmpty());
    }
}
