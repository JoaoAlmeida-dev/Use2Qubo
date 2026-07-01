package org.tzi.use.plugin.use2qubo.qubo;

import org.tzi.use.uml.sys.MObject;

import java.util.List;

public final class DecisionVar {

    public final String type;
    public final String association;
    public final String classA;
    public final String classB;
    /** Sorted (by name) objects of classA from the live system state. */
    public final List<MObject> domain;

    public DecisionVar(String type, String association,
                       String classA, String classB,
                       List<MObject> domain) {
        this.type        = type;
        this.association = association;
        this.classA      = classA;
        this.classB      = classB;
        this.domain      = domain;
    }
}
