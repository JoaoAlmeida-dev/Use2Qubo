package org.tzi.use.plugin.use2qubo.qubo;

import org.junit.jupiter.api.Test;
import org.tzi.use.plugin.use2qubo.qubo.context.QuboContext;
import org.tzi.use.plugin.use2qubo.qubo.context.QuboContextBuilder;
import org.tzi.use.plugin.use2qubo.testutil.UseFixtures;
import org.tzi.use.uml.sys.MLink;
import org.tzi.use.uml.sys.MObject;
import org.tzi.use.uml.sys.MSystem;

import java.util.List;

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

        // RouteRoad(Route,Road)=7, AssignedTo(Route,Truck)=2
        assertEquals(2, ctx.decisionVars.size());
        assertEquals(9, ctx.nVars);
    }

    @Test
    void build_returnsContextOverSandboxNotLiveSystem() throws Exception {
        MSystem system = UseFixtures.buildSystem(UseFixtures.maxCliqueUse(), UseFixtures.maxCliqueCmd());

        QuboContext ctx = QuboContextBuilder.build(system, UseFixtures.maxCliqueConfig().toPath());

        assertTrue(ctx.system != system);
        assertTrue(ctx.state != system.state());
        // MObject.equals() compares by name only, so containment checks can't tell sandbox
        // objects apart from live ones with the same name; compare identity against the live
        // object looked up by name instead.
        for (List<MObject> objs : ctx.objectsByClass.values()) {
            for (MObject obj : objs) {
                MObject liveObj = system.state().objectByName(obj.name());
                assertTrue(obj != liveObj,
                        "objectsByClass must reference sandbox MObjects, not live ones: " + obj.name());
            }
        }
        // MLink.equals() likewise compares by association + endpoint names, so check the link's
        // own endpoints are sandbox (not live) MObjects instead of using containment.
        for (List<MLink> links : ctx.fixedLinks.values()) {
            for (MLink link : links) {
                for (MObject endpoint : link.linkedObjects()) {
                    MObject liveObj = system.state().objectByName(endpoint.name());
                    assertTrue(endpoint != liveObj,
                            "fixedLinks must reference sandbox MObjects, not live ones: " + endpoint.name());
                }
            }
        }
    }
}
