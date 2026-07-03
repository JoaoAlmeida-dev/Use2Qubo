package org.tzi.use.plugin.use2qubo.qubo;

import org.junit.jupiter.api.Test;
import org.tzi.use.plugin.use2qubo.testutil.UseFixtures;
import org.tzi.use.uml.sys.MSystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuboContextBuilderTest {

    @Test
    void buildsContextFromMaxCliqueFixture() throws Exception {
        MSystem system = UseFixtures.buildSystem(UseFixtures.maxCliqueUse(), UseFixtures.maxCliqueCmd());

        QuboContext ctx = QuboContextBuilder.build(system, UseFixtures.maxCliqueConfig().toPath());

        assertEquals(1, ctx.decisionVars.size());
        assertEquals("Contains", ctx.decisionVars.get(0).association);
        assertEquals(10, ctx.nVars); // 1 Solution x 10 Vertex

        assertEquals(2, ctx.invariants.size());

        assertTrue(ctx.objectsByClass.containsKey("Vertex"));
        assertEquals(10, ctx.objectsByClass.get("Vertex").size());
        assertTrue(ctx.objectsByClass.containsKey("Solution"));
        assertEquals(1, ctx.objectsByClass.get("Solution").size());

        assertEquals("Solution.allInstances->collect(s | s.members->size())->sum()", ctx.objectiveExpr);
        assertEquals(false, ctx.minimise);
    }

    @Test
    void buildsContextFromGarageTrucksFixture() throws Exception {
        MSystem system = UseFixtures.buildSystem(UseFixtures.garageTrucksUse(), UseFixtures.garageTrucksCmd());

        QuboContext ctx = QuboContextBuilder.build(system, UseFixtures.garageTrucksConfig().toPath());

        assertEquals(2, ctx.decisionVars.size());
        assertEquals(8, ctx.nVars);
    }
}
