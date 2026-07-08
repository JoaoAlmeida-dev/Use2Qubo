package org.tzi.use.plugin.use2qubo.qubo.context;

import org.tzi.use.uml.sys.MObject;

import java.util.List;

/**
 * One {@code decision_vars} entry from {@code qubo_config.json}: a binary decision variable
 * family x_{a,b} ranging over every pair (a in classA, b in classB), materialised as an
 * {@code association(a,b)} link when x_{a,b}=1. {@link #domain} holds the concrete classA
 * objects from the live system state; classB objects are resolved separately per use (see
 * {@link QuboContextBuilder#build}) since the same DecisionVar may be paired against different
 * classB collections.
 */
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
