# JAVA-023 — Collapse SandboxWorkerPool's two dispatch loops into one shared helper

**Status:** Done
**Priority:** Medium
**Depends on:** none
**Context:** Architecture review (codebase-design skill) flagged `SandboxWorkerPool` (`qubo/engine/sampling/SandboxWorkerPool.java`, 387 lines, largest file in the plugin) as internally duplicated: `evaluate` (lines 199-264, returns `double[]`) and `evaluateIndexed` (lines 301-356, returns `List<R>`) both implement the identical ~55-line skeleton — threshold check (`total >= PARALLEL_SAMPLE_THRESHOLD && nWorkers > 1`), sequential-fallback branch, chunk sizing, `executor.submit` loop, poll-until-done loop (`Thread.sleep` + `AtomicInteger` polling), `future.get()` exception drain — differing only in the per-sample work body and result storage type. Confirmed via grilling session.

## Scope

- Add private `dispatchChunked(int total, ChunkWork work, Consumer<int[]> onPollTick, String logPrefix)` (exact signature TBD at implementation time; carries the threshold check, sequential-vs-pooled branch, chunk-range math, `executor.submit` loop, poll-until-done loop, `future.get()` drain — the full duplicated skeleton from both `evaluate` and `evaluateIndexed`).
- `ChunkWork` functional interface: `void run(SandboxWorker worker, int start, int end) throws Exception` — no generic result storage, no boxing. Each caller's lambda closes over and writes directly into its own result array (`double[] raw` in `evaluate`, `Object[] results` in `evaluateIndexed`), sidestepping the double-boxing cost flagged during grilling (results can be `C(n,m)`-sized).
- Progress formatting stays outside `dispatchChunked`: it invokes `onPollTick.accept(currentChunkCounts)` each poll tick (same `int[]` shape `chunkProgress`/`completed` already produce); `evaluate` wires this to the existing `reportBatch` (per-worker breakdown), `evaluateIndexed` wires it to the existing `reportIndexed` (aggregate only) — both keep their current output, unchanged.
- `evaluate` (199-264) shrinks to: build its `ChunkWork` lambda (`worker.eval(kind, J.toVector(n))` per index into `raw[i]`), call `dispatchChunked`, return `raw`.
- `evaluateIndexed` (301-356) shrinks to: build its `ChunkWork` lambda (`task.run(worker, index, x)` per index into `results[i]`), call `dispatchChunked`, return `Arrays.asList(results)`.
- `currentCounts`/`sum`/`allDone` helpers (266-276, 368-371) move inside/stay shared by `dispatchChunked` as needed — re-check at implementation time which are still referenced outside it.

## Files Changed

| File | Change |
|---|---|
| `src/main/java/org/tzi/use/plugin/use2qubo/qubo/engine/sampling/SandboxWorkerPool.java` | extract `dispatchChunked`, collapse `evaluate`/`evaluateIndexed` onto it |

## Acceptance criteria / Verification

- Build: `mvn -q -pl tools/use2qubo compile` succeeds.
- Test: existing tests covering sampling/derivation (`QuboEngineTest`, any `SandboxWorkerPool`-adjacent tests) pass unchanged — pure internal refactor, no behavior change, no public-method signature change (`build`, `evaluate`, `evaluateIndexed` unchanged from callers' view).
- Spot-check: no `double`→`Double` boxing introduced in the `evaluate` path (grep for `Double` in the new code); `evaluate`'s per-worker progress breakdown and `evaluateIndexed`'s aggregate-only progress are both preserved exactly as before.

## Commit

Once done, `git commit` the changes with a commit message of 15 words max.
