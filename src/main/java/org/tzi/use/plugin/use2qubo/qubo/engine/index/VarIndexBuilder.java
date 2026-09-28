package org.tzi.use.plugin.use2qubo.qubo.engine.index;

import org.tzi.use.plugin.use2qubo.qubo.context.DecisionVar;
import org.tzi.use.plugin.use2qubo.qubo.context.QuboContext;
import org.tzi.use.uml.sys.MObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Utility for building the flat ordered list of (DecisionVar, objA, objB) triples
 * that mirrors the variable ordering defined in QuboContext.
 */
public final class VarIndexBuilder {

    private VarIndexBuilder() {}

    /**
     * Builds the flat ordered list of (DecisionVar, objA, objB) triples that
     * mirrors the variable ordering defined in QuboContext.varIndex().
     */
    public static List<DVPair> buildFlatVars(QuboContext ctx) {
        List<DVPair> flat = new ArrayList<>(ctx.nVars);
        for (DecisionVar dv : ctx.decisionVars) {
            List<MObject> bObjs = ctx.objectsByClass.getOrDefault(
                    dv.classB, Collections.emptyList());
            List<MObject[]> pairs = new ArrayList<>(dv.domain.size() * bObjs.size());
            for (MObject a : dv.domain) {
                for (MObject b : bObjs) {
                    pairs.add(new MObject[]{a, b});
                }
            }
            pairs.sort(Comparator.<MObject[], String>comparing(p -> p[0].name())
                                  .thenComparing(p -> p[1].name()));
            for (MObject[] pair : pairs) {
                flat.add(new DVPair(dv, pair[0], pair[1]));
            }
        }
        return flat;
    }

    public static List<String> buildVarLabels(List<DVPair> flatVars) {
        List<String> varLabels = new ArrayList<>(flatVars.size());
        for (DVPair p : flatVars) {
            varLabels.add(p.dv.association + "(" + p.a.name() + "," + p.b.name() + ")");
        }
        return varLabels;
    }
}
