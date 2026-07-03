package org.tzi.use.plugin.use2qubo.qubo;

import com.google.gson.JsonSyntaxException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuboConfigTest {

    private static final String VALID_JSON = "{\n"
            + "  \"decision_var_associations\": [\"RouteStop\", \"AssignedTo\"],\n"
            + "  \"decision_vars\": [\n"
            + "    {\"type\": \"link\", \"association\": \"RouteStop\", \"domain\": [\"Route\", \"Node\"]},\n"
            + "    {\"type\": \"link\", \"association\": \"AssignedTo\", \"domain\": [\"Route\", \"Truck\"]}\n"
            + "  ],\n"
            + "  \"objective\": {\n"
            + "    \"expression\": \"Route.allInstances->collect(r | r.cost)->sum()\",\n"
            + "    \"minimise\": true\n"
            + "  }\n"
            + "}";

    @Test
    void parsesValidConfig() {
        QuboConfig config = QuboConfig.parse(VALID_JSON);

        assertEquals(2, config.decisionVarAssocs.size());
        assertTrue(config.decisionVarAssocs.contains("RouteStop"));
        assertTrue(config.decisionVarAssocs.contains("AssignedTo"));

        assertEquals(2, config.dvEntries.size());
        assertEquals("link", config.dvEntries.get(0)[0]);
        assertEquals("RouteStop", config.dvEntries.get(0)[1]);
        assertEquals("Route", config.dvEntries.get(0)[2]);
        assertEquals("Node", config.dvEntries.get(0)[3]);

        assertEquals("Route.allInstances->collect(r | r.cost)->sum()", config.objectiveExpr);
        assertTrue(config.minimise);
        assertTrue(config.isDecisionVar("RouteStop"));
        assertFalse(config.isDecisionVar("SomethingElse"));
    }

    @Test
    void missingDecisionVarAssociationsDefaultsToEmptySet() {
        QuboConfig config = QuboConfig.parse(
                "{ \"objective\": { \"expression\": \"1\", \"minimise\": true } }");

        assertTrue(config.decisionVarAssocs.isEmpty());
        assertTrue(config.dvEntries.isEmpty());
        assertFalse(config.isDecisionVar("Anything"));
    }

    @Test
    void missingObjectiveDefaultsToNullExpressionAndMinimiseTrue() {
        QuboConfig config = QuboConfig.parse(
                "{ \"decision_var_associations\": [\"A\"] }");

        assertEquals(null, config.objectiveExpr);
        assertTrue(config.minimise);
    }

    @Test
    void maximiseObjectiveIsRespected() {
        QuboConfig config = QuboConfig.parse(
                "{ \"objective\": { \"expression\": \"1\", \"minimise\": false } }");

        assertFalse(config.minimise);
    }

    @Test
    void decisionVarEntryWithMissingDomainDefaultsToEmptyStrings() {
        QuboConfig config = QuboConfig.parse(
                "{ \"decision_vars\": [ {\"type\": \"link\", \"association\": \"A\"} ] }");

        assertEquals(1, config.dvEntries.size());
        assertEquals("", config.dvEntries.get(0)[2]);
        assertEquals("", config.dvEntries.get(0)[3]);
    }

    @Test
    void malformedJsonSyntaxThrows() {
        assertThrows(JsonSyntaxException.class, () -> QuboConfig.parse("{ not valid json "));
    }
}
