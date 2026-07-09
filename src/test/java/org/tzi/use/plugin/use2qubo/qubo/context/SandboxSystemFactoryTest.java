package org.tzi.use.plugin.use2qubo.qubo.context;

import org.junit.jupiter.api.Test;
import org.tzi.use.plugin.use2qubo.qubo.context.SandboxSystemFactory.Sandbox;
import org.tzi.use.plugin.use2qubo.testutil.UseFixtures;
import org.tzi.use.uml.mm.MAssociation;
import org.tzi.use.uml.mm.MAttribute;
import org.tzi.use.uml.mm.MClass;
import org.tzi.use.uml.mm.MModel;
import org.tzi.use.uml.ocl.value.UndefinedValue;
import org.tzi.use.uml.ocl.value.Value;
import org.tzi.use.uml.sys.MLink;
import org.tzi.use.uml.sys.MObject;
import org.tzi.use.uml.sys.MSystem;
import org.tzi.use.uml.sys.MSystemState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SandboxSystemFactoryTest {

    private static Map<String, List<MObject>> objectsByClass(MModel model, MSystemState state, String... classNames) {
        Map<String, List<MObject>> byClass = new LinkedHashMap<>();
        for (String className : classNames) {
            MClass cls = model.getClass(className);
            List<MObject> objs = new ArrayList<>(state.objectsOfClass(cls));
            objs.sort((a, b) -> a.name().compareTo(b.name()));
            byClass.put(className, objs);
        }
        return byClass;
    }

    @Test
    void build_copiesObjectsByNameAndClass() throws Exception {
        MSystem system = UseFixtures.buildSystem(UseFixtures.maxCliqueUse(), UseFixtures.maxCliqueCmd());
        MModel model = system.model();
        MSystemState state = system.state();

        Map<String, List<MObject>> byClass = objectsByClass(model, state, "Vertex", "Solution");

        Sandbox sandbox = SandboxSystemFactory.build(model, state, byClass, false, Collections.emptyMap());

        for (List<MObject> objs : byClass.values()) {
            for (MObject obj : objs) {
                MObject sandboxObj = sandbox.byName.get(obj.name());
                assertTrue(sandboxObj != null, "missing sandbox object for " + obj.name());
                assertEquals(obj.cls().name(), sandboxObj.cls().name());
            }
        }
    }

    @Test
    void build_copiesAttributesWhenRequested() throws Exception {
        MSystem system = UseFixtures.buildSystem(UseFixtures.garageTrucksUse(), UseFixtures.garageTrucksCmd());
        MModel model = system.model();
        MSystemState state = system.state();

        Map<String, List<MObject>> byClass = objectsByClass(model, state, "Truck");
        MObject truck1 = state.objectByName("truck1");

        Sandbox sandbox = SandboxSystemFactory.build(model, state, byClass, true, Collections.emptyMap());

        MClass truckCls = model.getClass("Truck");
        MAttribute fuelRangeAttr = truckCls.attribute("fuelRange", true);
        MAttribute truckIdAttr = truckCls.attribute("truckId", true);

        MObject sandboxTruck1 = sandbox.byName.get("truck1");
        Value expectedFuelRange = truck1.state(state).attributeValue(fuelRangeAttr);
        Value expectedTruckId = truck1.state(state).attributeValue(truckIdAttr);

        assertEquals(expectedFuelRange, sandboxTruck1.state(sandbox.state).attributeValue(fuelRangeAttr));
        assertEquals(expectedTruckId, sandboxTruck1.state(sandbox.state).attributeValue(truckIdAttr));
    }

    @Test
    void build_skipsAttributesWhenNotRequested() throws Exception {
        MSystem system = UseFixtures.buildSystem(UseFixtures.garageTrucksUse(), UseFixtures.garageTrucksCmd());
        MModel model = system.model();
        MSystemState state = system.state();

        Map<String, List<MObject>> byClass = objectsByClass(model, state, "Truck");

        Sandbox sandbox = SandboxSystemFactory.build(model, state, byClass, false, Collections.emptyMap());

        MClass truckCls = model.getClass("Truck");
        MAttribute fuelRangeAttr = truckCls.attribute("fuelRange", true);

        MObject sandboxTruck1 = sandbox.byName.get("truck1");
        Value fuelRange = sandboxTruck1.state(sandbox.state).attributeValue(fuelRangeAttr);
        assertTrue(fuelRange instanceof UndefinedValue, "expected undefined attribute, got " + fuelRange);
    }

    @Test
    void build_copiesOnlyGivenLinks() throws Exception {
        MSystem system = UseFixtures.buildSystem(UseFixtures.garageTrucksUse(), UseFixtures.garageTrucksCmd());
        MModel model = system.model();
        MSystemState state = system.state();

        Map<String, List<MObject>> byClass = objectsByClass(model, state, "Route", "Truck");

        MAssociation assignedTo = model.getAssociation("AssignedTo");
        List<MLink> assignedToLinks = new ArrayList<>(state.linksOfAssociation(assignedTo).links());

        Map<String, List<MLink>> linksToCopy = new LinkedHashMap<>();
        linksToCopy.put("AssignedTo", assignedToLinks);

        Sandbox sandbox = SandboxSystemFactory.build(model, state, byClass, false, linksToCopy);

        assertEquals(assignedToLinks.size(),
                sandbox.state.linksOfAssociation(assignedTo).links().size());

        MAssociation routeRoad = model.getAssociation("RouteRoad");
        assertTrue(sandbox.state.linksOfAssociation(routeRoad).links().isEmpty());
    }
}
