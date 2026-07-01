package org.tzi.use.plugin.use2qubo.qubo;

import org.tzi.use.uml.mm.MClassInvariant;
import org.tzi.use.uml.mm.MModel;
import org.tzi.use.uml.sys.MLink;
import org.tzi.use.uml.sys.MObject;
import org.tzi.use.uml.sys.MSystem;
import org.tzi.use.uml.sys.MSystemState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class QuboContext {

    public final MSystem system;
    public final MModel model;
    public final MSystemState state;

    /** All model class invariants. */
    public final List<MClassInvariant> invariants;

    /** Objects grouped by class name; each list sorted by object name. */
    public final Map<String, List<MObject>> objectsByClass;

    /** Fixed (non-decision-var) links grouped by association name; each list sorted. */
    public final Map<String, List<MLink>> fixedLinks;

    /** Ordered decision variables from export_config.json. */
    public final List<DecisionVar> decisionVars;

    /** Total number of binary variables = sum of |classA| * |classB| per DecisionVar. */
    public final int nVars;

    /** OCL expression string from objective.expression in export_config.json. */
    public final String objectiveExpr;

    public final boolean minimise;

    QuboContext(MSystem system,
                MModel model,
                MSystemState state,
                List<MClassInvariant> invariants,
                Map<String, List<MObject>> objectsByClass,
                Map<String, List<MLink>> fixedLinks,
                List<DecisionVar> decisionVars,
                int nVars,
                String objectiveExpr,
                boolean minimise) {
        this.system          = system;
        this.model           = model;
        this.state           = state;
        this.invariants      = invariants;
        this.objectsByClass  = objectsByClass;
        this.fixedLinks      = fixedLinks;
        this.decisionVars    = decisionVars;
        this.nVars           = nVars;
        this.objectiveExpr   = objectiveExpr;
        this.minimise        = minimise;
    }

    /**
     * Returns the index of binary variable x_{a,b} in the flat decision vector.
     *
     * Variable order: decision_vars list order from export_config.json;
     * within each entry all (a,b) pairs sorted lexicographically by (a.name(), b.name()).
     */
    public int varIndex(String assocName, MObject a, MObject b) {
        int offset = 0;
        for (DecisionVar dv : decisionVars) {
            List<MObject> bObjs = objectsByClass.getOrDefault(dv.classB, new ArrayList<>());
            if (dv.association.equals(assocName)) {
                List<MObject[]> pairs = buildSortedPairs(dv.domain, bObjs);
                for (int i = 0; i < pairs.size(); i++) {
                    MObject[] pair = pairs.get(i);
                    if (pair[0].name().equals(a.name()) && pair[1].name().equals(b.name())) {
                        return offset + i;
                    }
                }
                throw new IllegalArgumentException(
                        "Pair not found in " + assocName + ": (" + a.name() + ", " + b.name() + ")");
            }
            offset += dv.domain.size() * bObjs.size();
        }
        throw new IllegalArgumentException("Unknown decision-var association: " + assocName);
    }

    private static List<MObject[]> buildSortedPairs(List<MObject> aObjs, List<MObject> bObjs) {
        List<MObject[]> pairs = new ArrayList<>(aObjs.size() * bObjs.size());
        for (MObject ao : aObjs) {
            for (MObject bo : bObjs) {
                pairs.add(new MObject[]{ao, bo});
            }
        }
        pairs.sort(Comparator.<MObject[], String>comparing(p -> p[0].name())
                              .thenComparing(p -> p[1].name()));
        return pairs;
    }
}
