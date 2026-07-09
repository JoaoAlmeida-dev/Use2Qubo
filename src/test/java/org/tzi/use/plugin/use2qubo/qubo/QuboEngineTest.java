package org.tzi.use.plugin.use2qubo.qubo;

import org.junit.jupiter.api.Test;
import org.tzi.use.parser.ocl.OCLCompiler;
import org.tzi.use.plugin.use2qubo.qubo.context.QuboContext;
import org.tzi.use.plugin.use2qubo.qubo.context.QuboContextBuilder;
import org.tzi.use.plugin.use2qubo.qubo.engine.QuboEngine;
import org.tzi.use.plugin.use2qubo.qubo.result.QuboResult;
import org.tzi.use.plugin.use2qubo.testutil.UseFixtures;
import org.tzi.use.plugin.use2qubo.util.Combinatorics;
import org.tzi.use.uml.ocl.expr.Evaluator;
import org.tzi.use.uml.ocl.expr.Expression;
import org.tzi.use.uml.ocl.value.RealValue;
import org.tzi.use.uml.ocl.value.VarBindings;
import org.tzi.use.uml.sys.MSystem;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
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

    @Test
    void derive_declinedEscalation_stopsAtLowerDegree() throws Exception {
        MSystem system = UseFixtures.buildSystem(UseFixtures.maxCliqueUse(), UseFixtures.maxCliqueCmd());
        QuboContext ctx = QuboContextBuilder.build(system, UseFixtures.maxCliqueConfig().toPath());

        QuboResult result = QuboEngine.derive(ctx, null, (from, to, expected) -> false);

        assertFalse(result.exact);
        assertEquals(2, result.polyDegree);
        assertEquals(0, result.nAncillaVars);
    }

    @Test
    void derive_acceptedEscalation_matchesLegacyBehaviour() throws Exception {
        MSystem systemA = UseFixtures.buildSystem(UseFixtures.maxCliqueUse(), UseFixtures.maxCliqueCmd());
        QuboContext ctxA = QuboContextBuilder.build(systemA, UseFixtures.maxCliqueConfig().toPath());
        QuboResult legacy = QuboEngine.derive(ctxA, null);

        MSystem systemB = UseFixtures.buildSystem(UseFixtures.maxCliqueUse(), UseFixtures.maxCliqueCmd());
        QuboContext ctxB = QuboContextBuilder.build(systemB, UseFixtures.maxCliqueConfig().toPath());
        QuboResult confirmed = QuboEngine.derive(ctxB, null, (from, to, expected) -> true);

        assertEquals(legacy.exact, confirmed.exact);
        assertEquals(legacy.polyDegree, confirmed.polyDegree);
        assertEquals(legacy.nVars, confirmed.nVars);
    }

    @Test
    void derive_confirmReceivesCorrectSampleCount() throws Exception {
        MSystem system = UseFixtures.buildSystem(UseFixtures.maxCliqueUse(), UseFixtures.maxCliqueCmd());
        QuboContext ctx = QuboContextBuilder.build(system, UseFixtures.maxCliqueConfig().toPath());

        int[] fromDegree = {-1};
        int[] toDegree = {-1};
        long[] expectedSamples = {-1};
        QuboEngine.derive(ctx, null, (from, to, expected) -> {
            fromDegree[0] = from;
            toDegree[0] = to;
            expectedSamples[0] = expected;
            return true;
        });

        assertEquals(2, fromDegree[0]);
        assertEquals(3, toDegree[0]);
        assertEquals(2L * Combinatorics.binomial(10, 3), expectedSamples[0]);
    }

    @Test
    void derive_isolatesSandboxState() throws Exception {
        MSystem system = UseFixtures.buildSystem(UseFixtures.maxCliqueUse(), UseFixtures.maxCliqueCmd());
        QuboContext ctx = QuboContextBuilder.build(system, UseFixtures.maxCliqueConfig().toPath());

        QuboEngine.derive(ctx, null);

        assertNotSame(system, ctx.system);
        assertNotSame(system.state(), ctx.state);
    }

    @Test
    void derive_attributeCopyIsFaithful() throws Exception {
        MSystem system = UseFixtures.buildSystem(UseFixtures.garageTrucksUse(), UseFixtures.garageTrucksCmd());
        QuboContext ctx = QuboContextBuilder.build(system, UseFixtures.garageTrucksConfig().toPath());

        String expr = "Truck.allInstances->collect(t | t.fuelRange)->sum()";
        StringWriter errBuf = new StringWriter();
        Expression compiled = OCLCompiler.compileExpression(
                system.model(), expr, "test", new PrintWriter(errBuf), new VarBindings());
        assertTrue(compiled != null, "Failed to compile OCL: " + errBuf);

        Evaluator evaluator = new Evaluator();
        double liveSum = ((RealValue) evaluator.eval(compiled, system.state())).value();
        double sandboxSum = ((RealValue) evaluator.eval(compiled, ctx.state)).value();

        assertEquals(liveSum, sandboxSum, 1e-9);
    }
}
