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
    void derive_escalatesAndQuadratizesExactly() throws Exception {
        // Selection's exactlyOneChosen invariant is a boolean indicator over all 3 decision
        // variables at once, so degree-2 sampling cannot represent it exactly. Since any
        // pseudo-Boolean function of n bits has an exact multilinear polynomial of degree <= n,
        // escalating to degree 3 (n=3, matching qubo_config.json's max_degree=3) is guaranteed
        // to find an exact polynomial, whose cubic term forces >=1 quadratization ancilla.
        MSystem system = UseFixtures.buildSystem(UseFixtures.selectionUse(), UseFixtures.selectionCmd());
        QuboContext ctx = QuboContextBuilder.build(system, UseFixtures.selectionConfig().toPath());

        QuboResult result = QuboEngine.derive(ctx, null);

        assertTrue(result.exact);
        assertEquals(3, result.polyDegree);
        assertTrue(result.nAncillaVars > 0);
        assertEquals(3 + result.nAncillaVars, result.nVars);
        assertEquals(3 + result.nAncillaVars, result.varLabels.size());
        for (String key : result.quadratic.keySet()) {
            String[] parts = key.split(",");
            int i = Integer.parseInt(parts[0]);
            int j = Integer.parseInt(parts[1]);
            assertTrue(i < j, "quadratic key " + key + " must be an ordered pair i<j");
        }
    }

    @Test
    void derive_remainsInexactAtDegreeCap() throws Exception {
        // AllOrNothing's allChosen invariant is true only when all 4 decision variables are
        // selected: its exact multilinear representation is the single top-degree monomial
        // x1*x2*x3*x4, with every lower-degree coefficient exactly zero. qubo_config.json caps
        // max_degree=3 (< n=4), so sampling correctly finds zero contribution at every degree it
        // explores (there is no partial/approximate degree-3 signal to pick up -- the true
        // coefficient is entirely concentrated on the unreachable degree-4 term). The derived
        // polynomial is therefore purely linear/constant, exactness fails on held-out points that
        // depend on the missing term, and quadratization correctly finds nothing above degree 2
        // to reduce (nAncillaVars stays 0) rather than fabricating a spurious ancilla.
        MSystem system = UseFixtures.buildSystem(UseFixtures.allOrNothingUse(), UseFixtures.allOrNothingCmd());
        QuboContext ctx = QuboContextBuilder.build(system, UseFixtures.allOrNothingConfig().toPath());

        QuboResult result = QuboEngine.derive(ctx, null);

        assertFalse(result.exact);
        assertEquals(3, result.polyDegree);
        assertEquals(0, result.nAncillaVars);
        assertEquals(4, result.nVars);
    }

    @Test
    void derive_declinedEscalation_stopsAtLowerDegree() throws Exception {
        MSystem system = UseFixtures.buildSystem(UseFixtures.allOrNothingUse(), UseFixtures.allOrNothingCmd());
        QuboContext ctx = QuboContextBuilder.build(system, UseFixtures.allOrNothingConfig().toPath());

        QuboResult result = QuboEngine.derive(ctx, null, (from, to, expected) -> false);

        assertFalse(result.exact);
        assertEquals(2, result.polyDegree);
        assertEquals(0, result.nAncillaVars);
    }

    @Test
    void derive_acceptedEscalation_matchesLegacyBehaviour() throws Exception {
        MSystem systemA = UseFixtures.buildSystem(UseFixtures.allOrNothingUse(), UseFixtures.allOrNothingCmd());
        QuboContext ctxA = QuboContextBuilder.build(systemA, UseFixtures.allOrNothingConfig().toPath());
        QuboResult legacy = QuboEngine.derive(ctxA, null);

        MSystem systemB = UseFixtures.buildSystem(UseFixtures.allOrNothingUse(), UseFixtures.allOrNothingCmd());
        QuboContext ctxB = QuboContextBuilder.build(systemB, UseFixtures.allOrNothingConfig().toPath());
        QuboResult confirmed = QuboEngine.derive(ctxB, null, (from, to, expected) -> true);

        assertEquals(legacy.exact, confirmed.exact);
        assertEquals(legacy.polyDegree, confirmed.polyDegree);
        assertEquals(legacy.nVars, confirmed.nVars);
    }

    @Test
    void derive_confirmReceivesCorrectSampleCount() throws Exception {
        MSystem system = UseFixtures.buildSystem(UseFixtures.allOrNothingUse(), UseFixtures.allOrNothingCmd());
        QuboContext ctx = QuboContextBuilder.build(system, UseFixtures.allOrNothingConfig().toPath());

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
        assertEquals(2L * Combinatorics.binomial(4, 3), expectedSamples[0]);
    }

    @Test
    void derive_isolatesSandboxState() throws Exception {
        MSystem system = UseFixtures.buildSystem(UseFixtures.allOrNothingUse(), UseFixtures.allOrNothingCmd());
        QuboContext ctx = QuboContextBuilder.build(system, UseFixtures.allOrNothingConfig().toPath());

        QuboEngine.derive(ctx, null);

        assertNotSame(system, ctx.system);
        assertNotSame(system.state(), ctx.state);
    }

    @Test
    void derive_attributeCopyIsFaithful() throws Exception {
        MSystem system = UseFixtures.buildSystem(UseFixtures.selectionUse(), UseFixtures.selectionCmd());
        QuboContext ctx = QuboContextBuilder.build(system, UseFixtures.selectionConfig().toPath());

        String expr = "Option.allInstances->collect(o | o.weight)->sum()";
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
