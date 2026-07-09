package org.tzi.use.plugin.use2qubo.qubo.context;

import org.tzi.use.uml.mm.MAssociation;
import org.tzi.use.uml.mm.MAssociationClass;
import org.tzi.use.uml.mm.MAttribute;
import org.tzi.use.uml.mm.MModel;
import org.tzi.use.uml.ocl.value.Value;
import org.tzi.use.uml.sys.MLink;
import org.tzi.use.uml.sys.MLinkObject;
import org.tzi.use.uml.sys.MObject;
import org.tzi.use.uml.sys.MSystem;
import org.tzi.use.uml.sys.MSystemState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds a throwaway {@link MSystem}/{@link MSystemState} clone of a live one, via public USE API
 * only: fresh objects (same class/name), optionally their attribute values, and a chosen subset
 * of links. Shared by JAVA-015 (full-fidelity clone to isolate {@code QuboEngine.derive}'s
 * mutations) and JAVA-016 (cheap attribute-free clone for a live preview diagram).
 */
final class SandboxSystemFactory {

    private SandboxSystemFactory() {}

    static final class Sandbox {
        final MSystem system;
        final MSystemState state;
        /** Real object name -> corresponding sandbox MObject. */
        final Map<String, MObject> byName;

        Sandbox(MSystem system, MSystemState state, Map<String, MObject> byName) {
            this.system = system;
            this.state = state;
            this.byName = byName;
        }
    }

    static Sandbox build(MModel model, MSystemState source,
                          Map<String, List<MObject>> objectsByClass,
                          boolean copyAttributes,
                          Map<String, List<MLink>> linksToCopy) throws Exception {
        MSystem sandboxSystem = new MSystem(model);
        MSystemState sandboxState = sandboxSystem.state();
        Map<String, MObject> byName = new LinkedHashMap<>();

        // Plain objects first: link-objects (association-class instances) need their endpoints
        // to already exist, since createLinkObject takes the linked objects as arguments.
        List<MLinkObject> linkObjects = new ArrayList<>();
        for (List<MObject> objs : objectsByClass.values()) {
            for (MObject obj : objs) {
                if (obj instanceof MLinkObject) {
                    linkObjects.add((MLinkObject) obj);
                    continue;
                }
                MObject sandboxObj = sandboxState.createObject(obj.cls(), obj.name());
                byName.put(obj.name(), sandboxObj);
            }
        }
        for (MLinkObject linkObj : linkObjects) {
            List<MObject> mapped = new ArrayList<>(linkObj.linkedObjects().size());
            for (MObject endpoint : linkObj.linkedObjects()) {
                mapped.add(byName.get(endpoint.name()));
            }
            MObject sandboxObj = sandboxState.createLinkObject(
                    (MAssociationClass) linkObj.cls(), linkObj.name(), mapped, Collections.emptyList());
            byName.put(linkObj.name(), sandboxObj);
        }

        if (copyAttributes) {
            for (List<MObject> objs : objectsByClass.values()) {
                for (MObject obj : objs) {
                    MObject sandboxObj = byName.get(obj.name());
                    Map<MAttribute, Value> attrs = obj.state(source).attributeValueMap();
                    for (Map.Entry<MAttribute, Value> e : attrs.entrySet()) {
                        sandboxObj.state(sandboxState).setAttributeValue(e.getKey(), e.getValue());
                    }
                }
            }
        }

        for (Map.Entry<String, List<MLink>> e : linksToCopy.entrySet()) {
            MAssociation assoc = model.getAssociation(e.getKey());
            if (assoc == null || assoc instanceof MAssociationClass) continue; // link-objects already created above
            for (MLink link : e.getValue()) {
                List<MObject> mapped = new ArrayList<>(link.linkedObjects().size());
                for (MObject endpoint : link.linkedObjects()) {
                    mapped.add(byName.get(endpoint.name()));
                }
                sandboxState.createLink(assoc, mapped, Collections.emptyList());
            }
        }

        return new Sandbox(sandboxSystem, sandboxState, byName);
    }
}
