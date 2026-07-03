package org.tzi.use.plugin.use2qubo.qubo;

import org.junit.jupiter.api.Test;
import org.tzi.use.plugin.use2qubo.testutil.UseFixtures;
import org.tzi.use.uml.sys.MSystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuboEngineTest {

    @Test
    void garageTrucksDerivesExactQubo() throws Exception {
        MSystem system = UseFixtures.buildSystem(UseFixtures.garageTrucksUse(), UseFixtures.garageTrucksCmd());
        QuboContext ctx = QuboContextBuilder.build(system, UseFixtures.garageTrucksConfig().toPath());

        QuboResult result = QuboEngine.derive(ctx, null);

        assertTrue(result.exact, "GarageTrucks objective+penalty is degree-2; exactness check should pass");
        assertEquals(8, result.nVars);
        assertEquals(8, result.varLabels.size());
        for (String key : result.quadratic.keySet()) {
            String[] parts = key.split(",");
            int i = Integer.parseInt(parts[0]);
            int j = Integer.parseInt(parts[1]);
            assertTrue(i < j, "quadratic key " + key + " must be an ordered pair i<j");
        }
    }

    @Test
    void maxCliqueDerivesInexactQuboByDesign() throws Exception {
        // Documented limitation (QuboEngine class Javadoc): MaxClique's cliqueProperty
        // invariant is a boolean pass/fail over many decision variables at once, so the
        // combined objective+penalty is not degree-2 representable. Asserted here as
        // expected behavior, not something to "fix".
        MSystem system = UseFixtures.buildSystem(UseFixtures.maxCliqueUse(), UseFixtures.maxCliqueCmd());
        QuboContext ctx = QuboContextBuilder.build(system, UseFixtures.maxCliqueConfig().toPath());

        QuboResult result = QuboEngine.derive(ctx, null);

        assertFalse(result.exact);
        assertEquals(10, result.nVars);
    }
}
