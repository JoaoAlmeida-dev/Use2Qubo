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
        MSystem system = UseFixtures.buildSystem(UseFixtures.selectionUse(), UseFixtures.selectionCmd());
        MModel model = system.model();
        MSystemState state = system.state();

        Map<String, List<MObject>> byClass = objectsByClass(model, state, "Option", "Picker");

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
        MSystem system = UseFixtures.buildSystem(UseFixtures.selectionUse(), UseFixtures.selectionCmd());
        MModel model = system.model();
        MSystemState state = system.state();

        Map<String, List<MObject>> byClass = objectsByClass(model, state, "Option");
        MObject o1 = state.objectByName("o1");

        Sandbox sandbox = SandboxSystemFactory.build(model, state, byClass, true, Collections.emptyMap());

        MClass optionCls = model.getClass("Option");
        MAttribute weightAttr = optionCls.attribute("weight", true);

        MObject sandboxO1 = sandbox.byName.get("o1");
        Value expectedWeight = o1.state(state).attributeValue(weightAttr);

        assertEquals(expectedWeight, sandboxO1.state(sandbox.state).attributeValue(weightAttr));
    }

    @Test
    void build_skipsAttributesWhenNotRequested() throws Exception {
        MSystem system = UseFixtures.buildSystem(UseFixtures.selectionUse(), UseFixtures.selectionCmd());
        MModel model = system.model();
        MSystemState state = system.state();

        Map<String, List<MObject>> byClass = objectsByClass(model, state, "Option");

        Sandbox sandbox = SandboxSystemFactory.build(model, state, byClass, false, Collections.emptyMap());

        MClass optionCls = model.getClass("Option");
        MAttribute weightAttr = optionCls.attribute("weight", true);

        MObject sandboxO1 = sandbox.byName.get("o1");
        Value weight = sandboxO1.state(sandbox.state).attributeValue(weightAttr);
        assertTrue(weight instanceof UndefinedValue, "expected undefined attribute, got " + weight);
    }

    @Test
    void build_copiesOnlyGivenLinks() throws Exception {
        MSystem system = UseFixtures.buildSystem(UseFixtures.selectionUse(), UseFixtures.selectionCmd());
        MModel model = system.model();
        MSystemState state = system.state();

        Map<String, List<MObject>> byClass = objectsByClass(model, state, "Picker", "Tag");

        MAssociation marked = model.getAssociation("Marked");
        List<MLink> markedLinks = new ArrayList<>(state.linksOfAssociation(marked).links());

        Map<String, List<MLink>> linksToCopy = new LinkedHashMap<>();
        linksToCopy.put("Marked", markedLinks);

        Sandbox sandbox = SandboxSystemFactory.build(model, state, byClass, false, linksToCopy);

        assertEquals(markedLinks.size(),
                sandbox.state.linksOfAssociation(marked).links().size());

        MAssociation chosen = model.getAssociation("Chosen");
        assertTrue(sandbox.state.linksOfAssociation(chosen).links().isEmpty());
    }
}
