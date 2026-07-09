# JAVA-015 — Isolate derive() on a sandbox; log restore failures; interactive escalation confirm

**Status:** Open
**Priority:** High
**Depends on:** JAVA-014 (`SandboxSystemFactory`, `Combinatorics.binomial`)
**Context:** A wider architectural review of `use2qubo` (prompted while planning JAVA-016's sandbox diagram) surfaced three issues in the derive pipeline, independent of any new UI feature:

1. `QuboEngine.derive` mutates `ctx.state` directly on a background `SwingWorker` thread. `ctx.state` is today the real, possibly-still-visible `MSystem`/`MSystemState` from the user's session — any other open view (object diagram, model browser) is registered on that same system's `EventBus` and can observe link flicker mid-derivation.
2. `QuboEngine.stripDecisionLinks`/`restoreLinks` (`QuboEngine.java:440-463`) silently swallow exceptions from `deleteLink`/`insertLink` (`catch (Exception e) { /* best effort */ }`), including in the `finally` block run after every derive. A failed restore leaves the live model in a state nobody is told about.
3. Degree escalation (`QuboEngine.java:127-155`) samples `C(n, degree)` live-model points per pass with no cost check or user visibility. For the documented V2 scenario (nVars=285), degree 3 alone is ~3.8M evals, with only a Cancel button on the modal progress dialog as an escape hatch.

Per stakeholder feedback: no hardcoded sample budget for issue 3 — instead an interactive Yes/No confirmation per degree step, showing the degree and sample cost, so the user decides whether to continue.

---

## 1. Isolate derive() on a sandbox (fixes issue 1)

`QuboContextBuilder.build(MSystem system, Path configPath)`:
- After parsing config and computing `objectsByClass`/`fixedLinks` against the **real** `system.state()` (as today, needed to know what to copy),
- call `SandboxSystemFactory.build(model, system.state(), objectsByClass, /*copyAttributes*/ true, fixedLinks)`,
- rebuild `objectsByClass`/`fixedLinks` a second time against the **sandbox's** own state (reusing the existing private `buildObjectsByClass`/`buildFixedLinks` helpers unchanged, just pointed at `sandbox.state`), so every downstream index (`varIndex`, `DVPair` construction in `QuboEngine.buildFlatVars`) resolves to sandbox `MObject`s consistently,
- construct `QuboContext` with `system = sandbox.system`, `state = sandbox.state` instead of the live session's.

Net effect: `QuboEngine` needs **no changes** for isolation — it only ever touches `ctx.system`/`ctx.state`, which now point at a throwaway clone. The live model the user is looking at is never mutated during derive. `QuboContext`'s own Javadoc ("Immutable snapshot of everything QuboEngine needs") becomes literally true instead of holding a live reference. `QuboCli` needs no change (already calls `QuboContextBuilder.build`) and gets isolation for free.

## 2. Log link-restore failures instead of swallowing them (fixes issue 2)

Change both catch blocks in `stripDecisionLinks`/`restoreLinks` from silent `{ /* best effort */ }` to `PluginLog.warn("Failed to delete/insert link during state restore: " + link, e)`, matching the logging style already used in `evalPenalty`'s invariant-eval catch. Behaviour unchanged (still best-effort); consequences are now confined to the sandbox per fix 1, so this is a pure visibility fix.

## 3. Interactive degree-escalation confirmation (fixes issue 3)

- Add to `QuboEngine`:
  ```java
  @FunctionalInterface
  public interface EscalationConfirm {
      /** Called before sampling toDegree; expectedSamples counts both cost+penalty passes. */
      boolean proceed(int fromDegree, int toDegree, long expectedSamples);
  }
  ```
- Keep `derive(QuboContext ctx, Consumer<String> progress)` working unchanged (delegates to a new overload with an always-true confirmer) — zero changes needed at `QuboCli.java:76` or `QuboEngineTest.java:24,50`.
- Add `derive(QuboContext ctx, Consumer<String> progress, EscalationConfirm confirm)`, used by `DeriveQuboAction`.
- In `deriveWithClearedState`'s escalation loop (`QuboEngine.java:127-155`), where it currently logs "Exactness failed at degree X; escalating to degree Y…": compute `expected = 2 * Combinatorics.binomial(n, nextDegree)` (cost pass + penalty pass, matching the two `PolySampler.sample` calls just below), call `confirm.proceed(degree, nextDegree, expected)`; if `false`, log (`PluginLog.info`, same style as `logExactnessOutcome`) that the user declined and `break` — falls through to the existing best-effort finalisation path (same as reaching `maxDegree` without exactness). No new `QuboResult` field needed.
- `DeriveQuboAction`'s confirmer runs the dialog on the EDT while `derive()` runs on the background `SwingWorker`: `SwingUtilities.invokeAndWait` wrapping `JOptionPane.showConfirmDialog(parent, "Escalating to degree " + toDegree + " will run " + expectedSamples + " additional live-model evaluations. Continue?", "Degree Escalation", JOptionPane.YES_NO_OPTION)`, returning `result == JOptionPane.YES_OPTION`. On `InvocationTargetException`/`InterruptedException`, return `false` (safe default: stop rather than hang).

## Files Changed

| File | Change |
|---|---|
| `qubo/context/QuboContextBuilder.java` | Build/route through `SandboxSystemFactory`; rebuild `objectsByClass`/`fixedLinks` against sandbox state |
| `qubo/engine/QuboEngine.java` | `EscalationConfirm` interface; new 3-arg `derive` overload; escalation-loop confirm call; log instead of swallow in `stripDecisionLinks`/`restoreLinks` |
| `action/DeriveQuboAction.java` | Pass a real `EscalationConfirm` (EDT dialog) to `derive` |

No changes needed to `QuboCli.java` or existing `QuboEngineTest` call sites (2-arg overload preserved).

## Implementation notes for whoever picks this up

- Existing `QuboEngineTest.java` (`src/test/java/.../qubo/QuboEngineTest.java`) already has two full-pipeline regression tests, both using `UseFixtures` (see JAVA-014) — read these first, they're the baseline every new test here must not break:
  - `garageTrucksEscalatesToDegreeSevenAndQuadratizesExactly` — `UseFixtures.garageTrucksUse()/garageTrucksCmd()` + `garageTrucksConfig()` (max_degree=7 in that config); asserts `result.exact==true`, `result.polyDegree==7`, `result.nAncillaVars==238`, `result.nVars==247`.
  - `maxCliqueRemainsInexactAtTheDegreeCapButStillExportsBestEffortQuadratization` — `UseFixtures.maxCliqueUse()/maxCliqueCmd()` + `maxCliqueConfig()` (default `max_degree=3`, no `max_degree` key in that config); asserts `result.exact==false`, `result.polyDegree==3`, `result.nVars==10+nAncillaVars`.
- **Prefer the MaxClique fixture for the new escalation-confirm tests.** GarageTrucks escalates all the way to degree 7 with 238 ancillas — realistic for the "does derive still work end-to-end" regression tests above, but slow and wasteful for a test that only needs to observe *one* escalation step get declined. MaxClique's default `max_degree=3` means it only escalates once (degree 2 → 3); wrap it with a confirmer to test both the accept and decline paths cheaply.
- Both existing test methods call `QuboEngine.derive(ctx, null)` — the 2-arg overload. Confirm after this ticket that both still compile and pass unchanged (proves the delegating overload is truly a no-behaviour-change wrapper).
- For `derive_attributeCopyIsFaithful`: reuse the real `Truck.fuelRange` (`Real`) attribute already on the GarageTrucks model (see JAVA-014's notes) rather than authoring a new fixture — evaluate `self.truck->collect(t|t.fuelRange)->sum()` (or similar, matching the style already used in `fuelPenalty()`) against both the sandboxed `ctx.state` and the original live state for the same fixed `x`, and assert equality.
- `EscalationConfirm` is a new public nested type on `QuboEngine` — put it as a top-level `public interface` inside the class (same placement style as `PolySampler.Evaluable`/`Result` in `qubo/engine/PolySampler.java`), not a separate file.

## Acceptance criteria / Verification

**`QuboEngineTest.java`** (existing file, add cases):
- `derive_declinedEscalation_stopsAtLowerDegree` — MaxClique fixture, confirmer `(from,to,n) -> false`. Assert no exception, `result.exact == false`, `result.polyDegree == 2` (declines the only escalation step this fixture takes), `result.nAncillaVars == 0`.
- `derive_acceptedEscalation_matchesLegacyBehaviour` — MaxClique fixture, confirmer always `true`; assert result matches the existing `maxCliqueRemainsInexactAtTheDegreeCapButStillExportsBestEffortQuadratization` test's expectations (same `exact`, `polyDegree`, `nVars`) via the 2-arg overload vs. the 3-arg overload with an always-true confirmer — both must produce identical `QuboResult` fields.
- `derive_confirmReceivesCorrectSampleCount` — MaxClique fixture (n=10), confirmer captures `(fromDegree, toDegree, expectedSamples)`; assert `fromDegree==2`, `toDegree==3`, `expectedSamples == 2 * binomial(10, 3)` (=240).
- `derive_isolatesSandboxState` — MaxClique fixture; after `derive()` returns, assert `ctx.system != originalSystem` and `ctx.state != originalState` (capture the originals before calling `QuboContextBuilder.build`, i.e. compare against the `MSystem` returned by `UseFixtures.buildSystem`).
- `derive_attributeCopyIsFaithful` — GarageTrucks fixture; see implementation note above.

**`QuboContextBuilderTest.java`** (existing file — check current contents before adding, to match its existing fixture/assertion style):
- `build_returnsContextOverSandboxNotLiveSystem` — assert `ctx.system != system` / `ctx.state != system.state()`, and that `ctx.objectsByClass`/`ctx.fixedLinks` reference the sandbox's own `MObject`/`MLink` instances.

## Commit

Once done, `git commit` the changes with a commit message of 15 words max.

**`QuboContextBuilderTest.java`** (existing file, add case):
- `build_returnsContextOverSandboxNotLiveSystem` — assert `ctx.system != system` / `ctx.state != system.state()`, and that `ctx.objectsByClass`/`ctx.fixedLinks` reference the sandbox's own `MObject`/`MLink` instances.

Manual:
- Run "Derive QUBO Matrix" on `examples/GarageTrucks` with another object-diagram view of the same model open; confirm that view shows no flicker/mutation during derivation.
- Trigger degree escalation (waste-collection example already escalates to degree 7); confirm the Yes/No dialog appears once per degree step with a plausible sample count, and answering "No" at some step stops escalation cleanly and still opens `QuboMatrixView` with a `false` exact badge.
- `QuboCli` regression: run existing example configs headlessly, confirm output unchanged (same `nVars`/`exact`/derivation behaviour as before), since its confirmer always proceeds.
