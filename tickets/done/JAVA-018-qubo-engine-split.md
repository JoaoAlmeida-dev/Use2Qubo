# JAVA-018 — Split QuboEngine.java into focused classes

**Status:** Done
**Priority:** Medium (readability/maintainability, no behaviour change)
**Depends on:** none
**Context:** `QuboEngine.java` (859 lines) is the only outlier file in `tools/use2qubo` (next largest is 348 lines). It mixes 7 unrelated responsibilities in one class: orchestration, decision-link state manipulation, cost/penalty OCL evaluation, exactness checking, quadratization verification, variable indexing, and progress reporting. This makes the file hard to scan and each concern hard to test in isolation. Goal: extract each concern into its own class, same package, so `QuboEngine` shrinks to pure orchestration (~250-300 lines) and each new file is single-purpose (~60-220 lines). Pure structural refactor — no behaviour change.

## Folder structure finding

Existing top-level package split (`config/`, `context/`, `engine/`, `result/`, `ui/`, `cli/`, `util/`, `action/`) is already domain-clean — no new top-level folders needed. The classes being extracted are tightly-coupled internal collaborators of the engine (they all operate on `QuboContext`/`MSystemState` during derivation), not a separate domain, so they stay in `qubo/engine/` alongside `PolySampler`, `Quadratizer`, `VarSet`, `ProgressEvent`.

## New files (all in `qubo/engine/`)

1. **`DVPair.java`** — the `(DecisionVar, MObject a, MObject b)` triple, currently a private nested class. Made package-visible so the other new classes can share it.
2. **`ExactnessOutcome.java`** — the `ExactnessOutcome` holder (points, method, matchCount, totalCount, exact), currently a private nested class of `QuboEngine`. Made package-visible. Pure data holder, no logic.
3. **`VarIndexBuilder.java`** — `buildFlatVars`, `buildVarLabels`. Pure indexing logic, no state mutation. Needs `DVPair`.
4. **`DecisionLinkSampler.java`** — `saveAndStripLinks`, `stripDecisionLinks`, `restoreLinks`, `withTemporaryLinks`, `ThrowingSupplier`. All the `MSystemState` link save/strip/insert/restore plumbing used before and during sampling. Needs `DVPair`.
5. **`ObjectiveEvaluator.java`** — `compileObjective`, `evalCost`, `computeObjective`. Depends on `DecisionLinkSampler.withTemporaryLinks`, `DVPair`.
6. **`PenaltyEvaluator.java`** — `PenaltyTask` record, `buildPenaltyTasks`, `computePenalty`, `evalOneInvariant`, `evalPenalty`, `PARALLEL_PENALTY_THRESHOLD`, `PENALTY_EVALUATOR` threadlocal. Depends on `DecisionLinkSampler.withTemporaryLinks`, `DVPair`.
7. **`PolyMath.java`** — `evalPoly`, `combine`, `computePenaltyWeight`. Pure pseudo-Boolean polynomial math, no OCL/state dependency.
8. **`ExactnessChecker.java`** — `checkExactness`, `checkExactnessExhaustive`, `checkExactnessSampled`, `logExactnessOutcome`. Depends on `ExactnessOutcome`, `ObjectiveEvaluator`, `PenaltyEvaluator`, `PolyMath`, `DecisionLinkSampler`, `DVPair`.
9. **`ResultAssembler.java`** — `buildResult`, `verifyQuadratization`, `ancillaProduct`, `evalQuadratic`. Depends on `ExactnessOutcome`, `Quadratizer`, `VarSet` — not the eval classes.
10. `report`/`reportPhase` move onto `ProgressEvent` as static helpers (`ProgressEvent.report(cb, msg)`, `ProgressEvent.reportPhase(cb, structuredCb, msg)`). Only task editing an existing shared file rather than creating a new one.

## What stays in `QuboEngine.java`

`TrueEval`, `EscalationConfirm`, `evaluateTrue`, the `derive(...)` overloads, and `deriveWithClearedState` — the orchestration sequence (sample → compute B → check exactness → escalate degree → assemble result), reading as a sequence of calls into the extracted classes.

## Execution waves

Dependency graph determines what runs in parallel. Each wave's tasks touch disjoint files with no cross-dependency within the wave. Waves run in order; `mvn -q compile` after every wave, not just at the end.

**Wave 0 — pure leaves (2 parallel subagents, cheap model):**
- `DVPair.java`
- `ExactnessOutcome.java`

Compile check.

**Wave 1 — depends on Wave 0 (4 parallel subagents, cheap model):**
- `PolyMath.java` (no deps)
- `ProgressEvent` static helpers (no deps; own compile check right after since later waves' progress-callback code calls into it)
- `DecisionLinkSampler.java` (needs `DVPair`)
- `ResultAssembler.java` (needs `ExactnessOutcome`, `Quadratizer`, `VarSet`)

Compile check.

**Wave 2 — depends on Wave 0/1 (3 parallel subagents, cheap model):**
- `VarIndexBuilder.java` (needs `DVPair`)
- `ObjectiveEvaluator.java` (needs `DecisionLinkSampler.withTemporaryLinks`, `DVPair`)
- `PenaltyEvaluator.java` (needs `DecisionLinkSampler.withTemporaryLinks`, `DVPair`)

Compile check.

**Wave 3+4 — sequential, single full-context agent (not delegable to a cheap model):**
- Extract `ExactnessChecker.java` (needs `ExactnessOutcome`, `ObjectiveEvaluator`, `PenaltyEvaluator`, `PolyMath`, `DecisionLinkSampler`, `DVPair`) — folded into the same agent as the rewire below since it must already understand every call site.
- Trim `QuboEngine.java` down to `TrueEval`, `EscalationConfirm`, `evaluateTrue`, `derive*` overloads, `deriveWithClearedState`, rewiring every call site to the Wave 0-3 classes.

**Wave 5 — sequential verification:**
- `mvn test`, `wc -l` check, CLI spot-check.

## Files Changed

| File | Change |
|---|---|
| `qubo/engine/QuboEngine.java` | Trimmed to orchestration only |
| `qubo/engine/DVPair.java` | New |
| `qubo/engine/ExactnessOutcome.java` | New |
| `qubo/engine/VarIndexBuilder.java` | New |
| `qubo/engine/DecisionLinkSampler.java` | New |
| `qubo/engine/ObjectiveEvaluator.java` | New |
| `qubo/engine/PenaltyEvaluator.java` | New |
| `qubo/engine/PolyMath.java` | New |
| `qubo/engine/ExactnessChecker.java` | New |
| `qubo/engine/ResultAssembler.java` | New |
| `qubo/engine/ProgressEvent.java` | Add `report`/`reportPhase` static helpers |

## Verification

- `mvn test` — existing tests (`QuboEngineTest.java`, `QuadratizerTest.java`) must pass unchanged (no behaviour change).
- `wc -l` on all files afterward: no new file exceeds ~250 lines, `QuboEngine.java` drops to ~250-300.
- Spot-check one CLI derive run (`QuboCli`) against a known example (e.g. garage trucks fixture) to confirm output `qubo.json` is byte-identical to before the refactor.

## Post-split addition: subpackages

After the flat split, `qubo/engine/` held 14 files, dense enough to warrant grouping. Follow-up
(same session, no separate ticket) moved the extracted classes into subpackages by what they
operate on, keeping `QuboEngine`, `ProgressEvent`, `Quadratizer`, `ResultAssembler` at the
package root:

- `qubo/engine/index/` — `DVPair`, `VarIndexBuilder`
- `qubo/engine/eval/` — `DecisionLinkSampler`, `ObjectiveEvaluator`, `PenaltyEvaluator`
- `qubo/engine/sampling/` — `VarSet`, `PolySampler`, `PolyMath`
- `qubo/engine/exactness/` — `ExactnessChecker`, `ExactnessOutcome`

Every moved class became `public` (previously package-private), since cross-package calls
require it — a deliberate trade of encapsulation for navigability, accepted per user direction.
Test files referencing moved classes (`QuadratizerTest`, `PolySamplerTest`) were updated/moved
to match. `qubo/engine/README.md` updated with the new package table and dependency diagram.
Re-verified: `mvn test` still 55/55 passing after the move.

## Commit

Single commit once Wave 5 verification passes: `refactor(use2qubo): split QuboEngine into focused collaborator classes`.
