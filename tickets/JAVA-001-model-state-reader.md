# JAVA-001 — Model & State Reader

## Goal

Build an internal `QuboContext` from the live USE runtime: read class invariants from `MModel` and objects/links from `MSystemState`. This replaces TICKET-001 + TICKET-002 (Python parser + instantiator).

## Scope

**In scope:**
- Collect all `MClassInvariant` from `MModel.classInvariants()`
- Build object list per class from `MSystemState.allObjects()`
- Build fixed link map from `MSystemState.allLinks()` filtered by `qubo_config.json` decision-var associations
- Build decision-variable index from `qubo_config.json` `decision_vars` array (same sorted-order logic as TICKET-002)
- Load `objective` expression string from `qubo_config.json`
- Produce `QuboContext` for downstream consumption by JAVA-002

**Out of scope:**
- OCL evaluation (JAVA-002)
- QUBO derivation (JAVA-002)
- Output writing (JAVA-003)

## Key USE APIs

```java
// Model structure
MModel model = system.model();
Collection<MClassInvariant> invs = model.classInvariants();
MClassInvariant inv = ...;
String className = inv.cls().name();      // context class
Expression body  = inv.bodyExpression();  // parsed OCL AST, ready for eval

// System state
MSystemState state = system.state();
Collection<MObject> objects = state.allObjects();
MObject obj = ...;
String cls  = obj.cls().name();
Value   val = obj.state(state).attributeValue(attr);  // fixed attribute

Collection<MLink> links = state.allLinks();
MLink link = ...;
String assocName = link.association().name();
List<MObject> ends = link.linkedObjects();
```

## `QuboContext` class

```java
public class QuboContext {
    public final MSystem system;
    public final MModel  model;
    public final MSystemState state;
    public final List<MClassInvariant> invariants;       // all model invariants
    public final Map<String, List<MObject>> objectsByClass;
    public final Map<String, List<MLink>>   fixedLinks;  // assocName → links (non-decision)
    public final List<DecisionVar> decisionVars;         // ordered, from export_config
    public final int nVars;
    public final String objectiveExpr;  // OCL string from qubo_config.json
    public final boolean minimise;

    // Index into binary x vector
    public int varIndex(String assocName, MObject a, MObject b) { ... }
}

public record DecisionVar(String type, String association,
                          String classA, String classB,
                          List<MObject> domain) {}
```

## Variable Indexing

Same deterministic sort as TICKET-002:
- Process `decision_vars` in list order from `qubo_config.json`
- Within each entry: all (a, b) pairs sorted by `(a.name(), b.name())` lexicographically
- First entry occupies indices 0..k-1, second k..m-1, etc.

## Config Loading

`qubo_config.json` is loaded from the same directory as the `.use` file (existing convention from `SystemStateExporter`). Required fields for QUBO:

```json
{
  "decision_var_associations": ["AssocName"],
  "decision_vars": [
    {"type": "link", "association": "AssocName", "domain": ["ClassA", "ClassB"]}
  ],
  "objective": {"expression": "OCL_STRING", "minimise": true}
}
```

## Acceptance Criteria

Given the GarbageTruckRouting model with `small_instance` loaded:

```java
QuboContext ctx = QuboContextBuilder.build(system, configPath);
assert ctx.invariants.size() == 14;
assert ctx.objectsByClass.get("Node").size() == 7;
assert ctx.objectsByClass.get("Truck").size() == 2;
assert ctx.nVars == 18;  // 14 RouteVisits + 4 AssignedTo
assert ctx.objectiveExpr != null;
```

## Dependencies

None beyond USE SDK. First Java ticket.
