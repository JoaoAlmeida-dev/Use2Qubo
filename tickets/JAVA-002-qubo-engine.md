# JAVA-002 — AutoQUBO Engine (Java port)

## Goal

Implement the AutoQUBO data-driven sampling algorithm (Moraglio et al. GECCO '22 §4) in Java. Takes a `QuboContext` and produces `QuboResult` with exact Q-matrix coefficients. This replaces TICKET-003 (OCL evaluator), TICKET-004 (penalty assembler), and TICKET-005 (AutoQUBO engine) — all in one Java implementation that leverages USE's built-in OCL evaluator.

## Algorithm

Identical to TICKET-005 spec. Reference that document for the full derivation.

```
N_samples = 1 + n*(n+1)/2
c        = f(0...0)
c_i      = f(e_i) - c
c_{i,j}  = f(e_i + e_j) - c_i - c_j - c
```

## Assembled Objective `f(x)`

```java
double f(int[] x, QuboContext ctx) {
    double penalty = 0.0;
    for (MClassInvariant inv : ctx.invariants) {
        for (MObject obj : ctx.objectsByClass.get(inv.cls().name())) {
            boolean holds = evalInvariant(inv, obj, x, ctx);
            if (!holds) penalty += 1.0;
        }
    }
    double obj = evalObjective(ctx.objectiveExpr, x, ctx);
    double B = ctx.nVars + 1.0;
    return obj + B * penalty;
}
```

## OCL Evaluation via USE

USE evaluates OCL against a real system state. For AutoQUBO we need to call f(x) for artificial binary vectors x that may not correspond to any valid system state. Strategy:

**Option A — State mutation (simpler):** for each sample point x, temporarily set decision-variable links in `MSystemState`, evaluate all invariants via `inv.bodyExpression().eval(...)`, then restore state. State mutation must be fully reversible.

**Option B — EvalContext injection (cleaner):** construct a synthetic `EvalContext` that answers link-navigation queries from the x vector without touching system state. Requires understanding USE's `EvalContext` / `Environment` internals.

Start with Option A; switch to B if state mutation proves too slow or fragile.

### Option A Implementation

```java
// Save original links
Set<MLink> toAdd    = decisionLinksForX(x, ctx);
Set<MLink> toRemove = allDecisionLinks(ctx);

// Mutate state
MSystemState state = ctx.state;
for (MLink l : toRemove) state.deleteLink(l);
for (MLink l : toAdd)    state.insertLink(l);

// Evaluate
double result = 0.0;
for (MClassInvariant inv : ctx.invariants) { ... eval ... }

// Restore
for (MLink l : toAdd)    state.deleteLink(l);
for (MLink l : toRemove) state.insertLink(l);
```

### Invariant Evaluation

```java
boolean evalInvariant(MClassInvariant inv, MObject self, MSystemState state) {
    Evaluator evaluator = new Evaluator();
    EvalContext ectx = new EvalContext(/* USE standard context */);
    ectx.setVar("self", new ObjectValue(self));
    Value result = evaluator.eval(inv.bodyExpression(), state, ectx);
    return result.equals(BooleanValue.TRUE);
}
```

Consult USE source (`org.tzi.use.uml.ocl.expr.Evaluator`) for exact API.

## `QuboResult` class

```java
public class QuboResult {
    public final double constant;
    public final Map<Integer, Double> linear;              // i → c_i
    public final Map<int[], Double>   quadratic;           // [i,j] → c_{ij}, i<j
    public final int    nVars;
    public final int    nSamples;
    public final boolean exact;
    public final List<String> varLabels;                   // for output

    public double eval(int[] x) { ... }
}
```

## Exactness Check

Same as TICKET-005: evaluate f and q on 20 random binary vectors not in training set; return `exact = max|f-q| < 1e-9`.

## Acceptance Criteria

```java
QuboContext ctx = QuboContextBuilder.build(system, configPath);
QuboResult  r   = QuboEngine.derive(ctx);

assert r.nVars    == 18;
assert r.nSamples == 1 + 18*19/2;  // 172
assert r.linear.size() > 0 || r.quadratic.size() > 0;
// Penalty B=19 should produce non-trivial coupling between route/truck vars
assert r.quadratic.size() > 0;
// Exactness: OCL penalties over binary links should be quadratic
assert r.exact == true;
```

## Dependencies

JAVA-001 (`QuboContext`). USE SDK (`Evaluator`, `EvalContext`, `MSystemState` mutation API).

## Performance Note

n=18 → 172 evaluations. Each evaluation mutates and restores up to 18 links + runs 14 invariants over 14 objects = ~200 OCL checks. Total ~34,000 OCL evaluations. Should complete in < 5 seconds; acceptable for a POC.
