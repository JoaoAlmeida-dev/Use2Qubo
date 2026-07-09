# JAVA-014 — Shared sandbox-`MSystem` factory + combinatorics helper

**Status:** Open
**Priority:** High (foundation for JAVA-015 and JAVA-016)
**Depends on:** none
**Context:** Two upcoming tickets both need a way to build a throwaway `MSystem`/`MSystemState` clone from a live one: JAVA-015 needs a full-fidelity clone (objects + attributes + fixed links) to isolate `QuboEngine.derive`'s mutations from the user's real session; JAVA-016 needs a cheaper, attribute-free clone to render a live preview diagram in the new "Try It" tab. Building this once, shared, avoids duplicating object/link-copy logic in two places.

An earlier investigation assumed attribute values couldn't be copied into a sandbox because `MSystemState.getObjectState` is package-private. Re-checked via `javap` against `lib/use-core-7.5.0.jar`: this is a false blocker. `MObject.state(MSystemState)` is a **public** method (on the public `MObject` interface) returning `MObjectState`, which has public `attributeValueMap()` and `setAttributeValue(MAttribute, Value)`. A full-fidelity clone (objects + attribute values + links) is achievable through public API alone.

---

## Scope

### 1. `SandboxSystemFactory`

New class `qubo/context/SandboxSystemFactory.java`:

```java
final class SandboxSystemFactory {
    static final class Sandbox {
        final MSystem system;
        final MSystemState state;
        final Map<String, MObject> byName; // real object name -> sandbox MObject
    }

    static Sandbox build(MModel model, MSystemState source,
                          Map<String, List<MObject>> objectsByClass,
                          boolean copyAttributes,
                          Map<String, List<MLink>> linksToCopy) { ... }
}
```

All via confirmed-public API:
- `new MSystem(model)` for a fresh system; `system.state()` for its `MSystemState`.
- For every real object in `objectsByClass`: `sandboxState.createObject(obj.cls(), obj.name())`, recorded in `byName`.
- If `copyAttributes`: for every real object, `obj.state(source).attributeValueMap()` (public getter), then for each entry `sandboxObj.state(sandboxState).setAttributeValue(attr, value)` (public setter).
- For every link in `linksToCopy`: resolve each endpoint via `byName`, `sandboxState.createLink(assoc, mappedEndpoints, emptyQualifiers)` — same signature already used in `QuboEngine.withTemporaryLinks`.

### 2. Extract `Combinatorics.binomial`

Move the `binomial(n, k)` helper out of `SamplingTabPanel.java:102-109` into `util/Combinatorics.java`. `SamplingTabPanel` keeps using it unchanged (no behaviour change). JAVA-015 will use it for escalation sample-count estimates.

## Files Changed

| File | Change |
|---|---|
| `qubo/context/SandboxSystemFactory.java` | New |
| `util/Combinatorics.java` | New — `binomial(n, k)` extracted from `SamplingTabPanel` |
| `ui/tabs/SamplingTabPanel.java` | Remove private `binomial`, call `Combinatorics.binomial` instead |

## Implementation notes for whoever picks this up

- `MSystemState.createObject`/`createLink` both declare `throws MSystemException`. Give `SandboxSystemFactory.build` a `throws Exception` signature (matches the existing style of `QuboEngine`'s private helpers, e.g. `evalCost`/`evalPenalty`), rather than adding new checked-exception wrapping machinery.
- Don't hand-roll a test model. `src/test/java/.../testutil/UseFixtures.java` already loads the two checked-in example scenarios headlessly (mirrors `QuboCli`'s compile+run sequence) via `UseFixtures.buildSystem(useFile, cmdFile)`:
  - `UseFixtures.garageTrucksUse()/garageTrucksCmd()` — waste-collection model, richer attribute set.
  - `UseFixtures.maxCliqueUse()/maxCliqueCmd()` — smaller (10 vertices), faster to build.
  Use one of these directly in `SandboxSystemFactoryTest` instead of constructing a synthetic model by hand.
- Concrete attribute fixtures to assert against in `build_copiesAttributesWhenRequested` (from `examples/GarageTrucks/GarbageTruckRouting.use`): `Truck.fuelRange` (`Real`) and `Truck.truckId` (`Integer`) — both already declared on the real model, no need to invent attribute types.
- Tests that use `UseFixtures` resolve paths like `"examples/GarageTrucks/..."` relative to cwd — run with working directory `tools/use2qubo` (same as existing `QuboEngineTest`/`QuboContextBuilderTest`, which already use this fixture without special setup).
- Project uses JUnit 5 (`org.junit.jupiter.api.Test`, see any existing test under `src/test/java`) — match that, not JUnit 4.

## Acceptance criteria / Verification

New `SandboxSystemFactoryTest.java`:
- `build_copiesObjectsByNameAndClass` — using `UseFixtures.maxCliqueUse()/maxCliqueCmd()` (small, fast); sandbox has same object names/classes as source.
- `build_copiesAttributesWhenRequested` — `copyAttributes=true`, using `UseFixtures.garageTrucksUse()/garageTrucksCmd()`; assert `sandboxObj.state(sandboxState).attributeValue(fuelRangeAttr)` equals the source `Truck` object's `fuelRange`, and likewise for the `Integer` `truckId` attribute.
- `build_skipsAttributesWhenNotRequested` — `copyAttributes=false`; attribute map on sandbox objects is empty.
- `build_copiesOnlyGivenLinks` — pass a `linksToCopy` map with one association (e.g. `AssignedTo` from GarageTrucks); sandbox has exactly those links and none of the others present in source, checked via `byName` + `sandboxState.linksOfAssociation(...)`.

New `CombinatoricsTest.java`: `binomial(5,2)==10`, `binomial(n,0)==1`, `binomial(n,n)==1`, out-of-range `k` (negative or `>n`) returns `0`.

`mvn clean package` — no compile errors; `SamplingTabPanel`'s existing behaviour (probe-count summaries) unchanged.

## Commit

Once done, `git commit` the changes with a commit message of 15 words max.
