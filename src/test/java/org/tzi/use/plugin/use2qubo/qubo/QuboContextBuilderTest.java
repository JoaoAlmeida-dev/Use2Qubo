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
    void buildsContextFromFixture() throws Exception {
        MSystem system = UseFixtures.buildSystem(UseFixtures.selectionUse(), UseFixtures.selectionCmd());

        QuboContext ctx = QuboContextBuilder.build(system, UseFixtures.selectionConfig().toPath());

        assertEquals(1, ctx.decisionVars.size());
        assertEquals("Chosen", ctx.decisionVars.get(0).association);
        assertEquals(3, ctx.nVars); // 1 Picker x 3 Options

        assertEquals(1, ctx.invariants.size());

        assertTrue(ctx.objectsByClass.containsKey("Option"));
        assertEquals(3, ctx.objectsByClass.get("Option").size());
        assertTrue(ctx.objectsByClass.containsKey("Picker"));
        assertEquals(1, ctx.objectsByClass.get("Picker").size());

        assertEquals("Picker.allInstances->collect(p | p.options->size())->sum()", ctx.objectiveExpr);
        assertEquals(true, ctx.minimise);
    }

    @Test
    void buildsContextFromFixtureWithTwoDecisionVars() throws Exception {
        MSystem system = UseFixtures.buildSystem(UseFixtures.selectionUse(), UseFixtures.selectionCmd());

        QuboContext ctx = QuboContextBuilder.build(system, UseFixtures.selectionUiConfig().toPath());

        // Chosen(Picker,Option)=3, Marked(Picker,Tag)=1
        assertEquals(2, ctx.decisionVars.size());
        assertEquals(4, ctx.nVars);
    }

    @Test
    void build_returnsContextOverSandboxNotLiveSystem() throws Exception {
        MSystem system = UseFixtures.buildSystem(UseFixtures.selectionUse(), UseFixtures.selectionCmd());

        QuboContext ctx = QuboContextBuilder.build(system, UseFixtures.selectionConfig().toPath());

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
