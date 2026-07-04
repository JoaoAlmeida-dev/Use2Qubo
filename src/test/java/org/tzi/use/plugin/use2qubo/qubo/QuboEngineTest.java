package org.tzi.use.plugin.use2qubo.qubo;

import org.junit.jupiter.api.Test;
import org.tzi.use.plugin.use2qubo.testutil.UseFixtures;
import org.tzi.use.uml.sys.MSystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuboEngineTest {

    @Test
    void garageTrucksEscalatesToDegreeSevenAndQuadratizesExactly() throws Exception {
        // Feasibility (routeConnected, fuelWithinRange, capacityWithinRange, binCovered,
        // routeTouchesDepot/Disposal) is expressed entirely as plain OCL invariants; the
        // objective is pure edgeCost(). QuboEngine's own sampling/quadratization pass derives
        // an exact QUBO for these once qubo_config.json's max_degree covers the true arity
        // (the 7 RouteRoad candidates for this scenario's one route) -- no manual polynomial
        // rewrite needed, per QuboEngine.logExactnessOutcome's own "raise max_degree" guidance.
        MSystem system = UseFixtures.buildSystem(UseFixtures.garageTrucksUse(), UseFixtures.garageTrucksCmd());
        QuboContext ctx = QuboContextBuilder.build(system, UseFixtures.garageTrucksConfig().toPath());

        QuboResult result = QuboEngine.derive(ctx, null);

        assertTrue(result.exact);
        assertEquals(7, result.polyDegree);
        assertEquals(238, result.nAncillaVars);
        assertEquals(247, result.nVars);
        assertEquals(247, result.varLabels.size());
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
