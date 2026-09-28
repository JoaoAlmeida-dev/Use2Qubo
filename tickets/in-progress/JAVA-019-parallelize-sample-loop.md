# JAVA-019 — Parallelize QuboEngine sampling across VarSet combinations

**Status:** In Progress
**Priority:** Medium
**Depends on:** none
**Context:** `PolySampler.sample`'s outer loop over VarSet combinations
(`src/main/java/org/tzi/use/plugin/use2qubo/qubo/engine/sampling/PolySampler.java:83-103`) is the
dominant cost of QUBO derivation: it evaluates the black-box cost/penalty function once per `C(n,m)`
combination, for both the cost and penalty passes, at every degree up to `maxDegree`. Degree
escalation (waste collection needs degree 7, max clique degree 9) makes this blow up combinatorially.
Only the innermost per-sample penalty computation is parallel today
(`PenaltyEvaluator.computePenalty`, `parallelStream()` over invariant tasks once
`PARALLEL_PENALTY_THRESHOLD`=16 is hit) — the outer sample loop itself, and
`ExactnessChecker.checkExactnessExhaustive`'s `2^n`-vector loop, are fully sequential because both
mutate the single shared `ctx.state` (insert decision links → eval → strip links), which is not
thread-safe for concurrent mutation.

The fix: give each worker thread its own throwaway `MSystem`/`MSystemState` clone (the codebase
already has this via `SandboxSystemFactory`, used by JAVA-015 for derive-time isolation), so N threads
can each independently insert/eval/strip links without touching a shared state. Samples within one
degree pass are embarrassingly parallel — the inclusion-exclusion subtraction for term `J` only reads
already-fixed lower-degree coefficients, never another same-degree term — so this is safe.

## Scope

- Decouple `DecisionLinkSampler` from `QuboContext`: its methods (`saveAndStripLinks`,
  `stripDecisionLinks`, `restoreLinks`, `withTemporaryLinks`) currently take the whole `ctx` and reach
  into `ctx.state`/`ctx.model`/`ctx.decisionVars`. Refactor to take those three pieces explicitly, so
  both the live-ctx path and the new sandbox workers can use the same primitive. Update call sites in
  `ObjectiveEvaluator.evalCost`, `PenaltyEvaluator.evalPenalty`, `QuboEngine`, `ExactnessChecker` to
  pass `ctx.state`/`ctx.model`/`ctx.decisionVars` instead of `ctx`.
- New class `SandboxWorkerPool` (`qubo/engine/sampling/SandboxWorkerPool.java`), a pool of
  `SandboxWorker`s, built once per `derive()` call:
  - Sizes itself `min(Runtime.availableProcessors(), MAX_SAMPLE_WORKERS)`; skips pooling (falls back
    to today's single-threaded path) when the workload is small — new `PARALLEL_SAMPLE_THRESHOLD` in
    `QuboConstants`, gating on `C(n,m)`, mirroring `PenaltyEvaluator.PARALLEL_PENALTY_THRESHOLD`.
  - Each `SandboxWorker` clones a sandbox via `SandboxSystemFactory.build(ctx.model, ctx.state,
    ctx.objectsByClass, /*copyAttributes*/ true, ctx.fixedLinks)`, remaps `flatVars` (the `DVPair`
    list) and a `PenaltyEvaluator.buildPenaltyTasks`-equivalent list through the sandbox's `byName`
    map, and owns one private `Evaluator` instance (the objective `Expression` AST is stateless/shared
    — only `Evaluator` has the mutable eval-context field, per its existing javadoc).
  - Pool exposes `evaluate(List<VarSet> combos, EvalKind cost|penalty)`: partitions `combos` into
    `nWorkers` contiguous chunks, submits one `Callable` per chunk to a fixed `ExecutorService`, each
    chunk processed sequentially against that worker's own sandbox using the refactored
    `DecisionLinkSampler` primitives, returns raw `f(x)` values aligned to input order.
  - `PenaltyEvaluator` needs a sandbox-object variant of `buildPenaltyTasks` (remap `MObject` through
    `byName`).
- `PolySampler.sample`: add an overload taking a `SandboxWorkerPool` instead of a single `Evaluable`.
  Per-degree loop body becomes "batch-evaluate all `C(n,m)` combos via the pool, then do
  inclusion-exclusion subtraction sequentially" (subtraction is cheap map lookups, not parallelized).
- `QuboEngine.deriveWithClearedState`: build one `SandboxWorkerPool` and reuse it for both the cost
  pass and the penalty pass, across the initial degree-2 pass and every escalation iteration.
- `ExactnessChecker.checkExactnessExhaustive`: use the same pool, partitioning the `2^n` index range
  across workers instead of combos. Aggregate `matchCount`/`evalFailedCount` with thread-safe counters
  (`LongAdder`); cap `mismatches`/`matchesSample` collection with a synchronized list + size check.
  "First K mismatches" ordering becomes best-effort under concurrency — acceptable, since the proof
  itself is `matchCount == totalCount`, not the sample list.
- Progress reporting under the pool: `PolySampler.sample` today reports one `progress`/
  `structuredProgress` callback per sampled combo, documented as "called from the calling thread"
  (Swing progress bar / GUI Sampling tab are not thread-safe to call from workers). Fix: workers never
  call these callbacks themselves — each worker increments a shared `AtomicInteger`/`LongAdder`
  "completed" counter after finishing each combo in its chunk. `SandboxWorkerPool.evaluate(...)` takes
  the callbacks itself and, on the calling thread, polls that counter in a short loop (e.g. every
  ~50-100ms via `Future.isDone()` checks) while chunk tasks run, firing a coalesced progress tick each
  time the count advances, plus one final tick at `count == total`. Preserves the calling-thread
  contract; coalescing multiple combos into one tick under load is the only visible behaviour change.
- Logging: `SandboxWorkerPool`'s constructor logs once via `PluginLog.info` — `nWorkers` chosen,
  `Runtime.availableProcessors()`, `MAX_SAMPLE_WORKERS` cap, sandbox-cloning time (summed across
  workers). `evaluate(combos, kind)` logs once per batch, before the progress-poll loop starts:
  pooled vs sequential fallback (and why — `combos.size()` vs `PARALLEL_SAMPLE_THRESHOLD`), the
  chunk-size split across workers, and the degree/phase (`"cost"`/`"pen"`, degree m) about to run.
  Mirrors the existing `PluginLog.info` style already in `QuboEngine` (`"derive: nVars=..."`,
  `"escalating sampling to degree ..."`), so pool behaviour is visible in the log stream without
  needing the GUI's progress bar.
- Not touched: `Quadratizer.reduce` (cheap, not a bottleneck), `ExactnessChecker`'s sampled
  (non-exhaustive) branch (already bounded by `EXACTNESS_SAMPLE_COUNT`), and
  `PenaltyEvaluator.computePenalty`'s existing intra-sample parallelism (a sandbox worker still uses it
  for its own chunk if a single sample's invariant-task count is large).
- Update `qubo/engine/README.md` to document `SandboxWorkerPool`/`SandboxWorker` per repo convention.

## Execution waves

**Wave 0 — sequential, foundational:**
- Refactor `DecisionLinkSampler` to take explicit `(MModel, MSystemState, List<DecisionVar>)` instead
  of `QuboContext`; update `ObjectiveEvaluator`, `PenaltyEvaluator`, `QuboEngine`, `ExactnessChecker`
  call sites. Build + run existing tests — pure refactor, must not change behaviour.

**Wave 1 — sequential, depends on Wave 0:**
- Implement `SandboxWorkerPool`/`SandboxWorker` (worker construction via `SandboxSystemFactory`,
  threshold-gated fallback, `evaluate(combos, kind)` batch API, and the constructor/per-batch
  `PluginLog` calls described above) and the sandbox-remapped `buildPenaltyTasks` variant.

**Wave 2 — depends on Wave 1 (2 parallel subagents):**
- Task A: `PolySampler` pool-aware overload + `QuboEngine` wiring (cost and penalty passes, initial
  pass and escalation loop); includes counter-based progress polling — workers only increment a shared
  counter, `SandboxWorkerPool.evaluate` polls it from the calling thread and fires
  `progress`/`structuredProgress` itself.
- Task B: `ExactnessChecker.checkExactnessExhaustive` parallelization (thread-safe aggregation), reusing
  the same counter-polling progress mechanism for its `2^n`-vector loop's reports.

**Wave 3 — sequential verification:**
- `mvn test` — full suite green, including `QuadratizerTest`, `PolySamplerTest`.
- Re-run derivation on waste collection (`RouteRoad`, degree 7, 247 vars) and max clique (degree 9,
  293 vars); diff `qubo.json` against pre-change baseline — coefficients, `exact`,
  `exactnessMatchCount`/`exactnessTotalCount` must be identical.
- Record before/after derivation wall-clock time on both examples as speedup evidence.

## Files Changed

| File | Change |
|---|---|
| `qubo/engine/eval/DecisionLinkSampler.java` | Signature refactor: explicit state/model/decisionVars instead of `QuboContext`. |
| `qubo/engine/eval/ObjectiveEvaluator.java` | Update call sites for refactored `DecisionLinkSampler`. |
| `qubo/engine/eval/PenaltyEvaluator.java` | Update call sites; add sandbox-remapped `buildPenaltyTasks` variant. |
| `qubo/engine/sampling/SandboxWorkerPool.java` | New: pool of `SandboxWorker`s wrapping per-thread sandbox clones. |
| `qubo/engine/sampling/PolySampler.java` | New pool-aware `sample` overload. |
| `qubo/engine/QuboEngine.java` | Build/reuse one `SandboxWorkerPool` per `derive()` call. |
| `qubo/engine/exactness/ExactnessChecker.java` | Parallelize `checkExactnessExhaustive`'s `2^n` loop via the pool. |
| `util/QuboConstants.java` | New `PARALLEL_SAMPLE_THRESHOLD`, `MAX_SAMPLE_WORKERS`. |
| `qubo/engine/README.md` | Document `SandboxWorkerPool`/`SandboxWorker`. |

## Acceptance criteria / Verification

- `mvn -q test` in `tools/use2qubo` — green.
- Run both example `.cmd` scripts (or CLI), capture `qubo.json`, diff against pre-change baseline —
  byte-identical apart from timing/metadata fields.
- Wall-clock derivation time on the 247-var waste-collection example measurably improves.

## Commit

Once done, `git commit` the changes with a commit message of 15 words max.
