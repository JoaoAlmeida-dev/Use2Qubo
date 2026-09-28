package org.tzi.use.plugin.use2qubo.qubo.engine.index;

import org.tzi.use.plugin.use2qubo.qubo.context.DecisionVar;
import org.tzi.use.uml.sys.MObject;

/** One (DecisionVar, objA, objB) triple in the flat decision-variable index. */
public final class DVPair {
    public final DecisionVar dv;
    public final MObject a;
    public final MObject b;

    public DVPair(DecisionVar dv, MObject a, MObject b) {
        this.dv = dv;
        this.a  = a;
        this.b  = b;
    }
}
