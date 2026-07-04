package org.tzi.use.plugin.use2qubo.qubo;

import org.junit.jupiter.api.Test;
import org.tzi.use.plugin.use2qubo.testutil.UseFixtures;
import org.tzi.use.uml.sys.MSystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuboEngineTest {

    @Test
    void garageTrucksEscalatesToDegreeThreeAndQuadratizesExactly() throws Exception {
        // RouteRoad(Route,Road) edge-based decision variables make edgeCost()
        // linear and Route::fuelPenalty()'s slack-variable encoding (Lucas 2014
        // Sec 2.3) genuinely degree-2 exact in isolation. GarbageBin::coveragePenalty()/
        // Route::shapePenalty(), however, aggregate an exists/OR over the (small, degree-3)
        // set of incident-Road decision variables per node in this scenario, so the
        // full objective needs degree-3 sampling (AutoQUBO Sec 4.3) before it is exact;
        // the resulting cubic terms are then reduced via Rosenberg quadratization
        // (3 ancilla variables), and the reduction is itself verified exact.
        MSystem system = UseFixtures.buildSystem(UseFixtures.garageTrucksUse(), UseFixtures.garageTrucksCmd());
        QuboContext ctx = QuboContextBuilder.build(system, UseFixtures.garageTrucksConfig().toPath());

        QuboResult result = QuboEngine.derive(ctx, null);

        assertTrue(result.exact);
        assertEquals(3, result.polyDegree);
        assertEquals(3, result.nAncillaVars);
        assertEquals(19, result.nVars);
        assertEquals(19, result.varLabels.size());
        for (String key : result.quadratic.keySet()) {
            String[] parts = key.split(",");
            int i = Integer.parseInt(parts[0]);
            int j = Integer.parseInt(parts[1]);
            assertTrue(i < j, "quadratic key " + key + " must be an ordered pair i<j");
        }
    }

    @Test
    void maxCliqueRemainsInexactAtTheDegreeCapButStillExportsBestEffortQuadratization() throws Exception {
        // MaxClique's cliqueProperty invariant is a boolean pass/fail over many decision
        // variables at once (up to all 10), so it needs a polynomial degree far beyond the
        // default max_degree=3 cap to be exact. When the cap is reached without exactness,
        // QuboEngine still quadratizes the best degree-3 polynomial found (rather than
        // silently discarding it back to a degree-2 slice) so the closest attempt is visible
        // and the user can raise max_degree if a tighter fit is needed.
        MSystem system = UseFixtures.buildSystem(UseFixtures.maxCliqueUse(), UseFixtures.maxCliqueCmd());
        QuboContext ctx = QuboContextBuilder.build(system, UseFixtures.maxCliqueConfig().toPath());

        QuboResult result = QuboEngine.derive(ctx, null);

        assertFalse(result.exact);
        assertEquals(3, result.polyDegree);
        assertTrue(result.nAncillaVars > 0);
        assertEquals(10 + result.nAncillaVars, result.nVars);
    }
}
