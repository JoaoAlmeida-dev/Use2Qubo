# JAVA-022 — Collapse QuboEngine.derive's 5 overloads into one DeriveOptions entry point

**Status:** Done
**Priority:** Medium
**Depends on:** none
**Context:** Architecture review (codebase-design skill) flagged `QuboEngine.derive` (`qubo/engine/QuboEngine.java:128-214`) as shallow: 5 telescoping overloads (2 through 6 params), each pure forwarding to the next with a hardcoded default, carrying ~90 lines of near-duplicate Javadoc between them. Interface surface grows with every new option; a future option would need a 6th overload. Confirmed via grilling session: 2 production callers (`DeriveQuboAction` uses the 3-arg shape, `QuboCli` uses the 5-arg shape with `workerOverride`), 6 test call sites in `QuboEngineTest.java` using varied arities. No external consumers of `QuboEngine` exist — it's plugin-internal, not a published library.

## Scope

- Add `QuboEngine.DeriveOptions` — nested static record-with-wither class (immutable, `withX` methods return a new instance), fields: `progress` (`Consumer<String>`), `structuredProgress` (`Consumer<ProgressEvent>`), `confirm` (`EscalationConfirm`), `collectSamples` (`boolean`), `workerOverride` (`Integer`).
- `DeriveOptions.defaults()` static factory: `progress=null, structuredProgress=null, confirm=ALWAYS_PROCEED, collectSamples=true, workerOverride=null` — byte-for-byte match to today's overload-chain defaults.
- Replace all 5 `derive(...)` overloads (lines 128-214) with one: `derive(QuboContext ctx, DeriveOptions options)`. `ctx` stays a separate required param (not folded into options) — it's the mandatory subject, options are the optional tuning knobs.
- Delete the 5 old overloads outright (hard-cut, no deprecated forwarders — nothing outside this repo calls them).
- Update `deriveWithClearedState`'s private signature to take `DeriveOptions` instead of the 5 loose params it currently threads through (`progress, structuredProgress, confirm, collectSamples, workerOverride` at lines 246-248).
- Update callers:
  - `DeriveQuboAction.java:102-103` — `QuboEngine.derive(ctx, this::publish, (from,to,exp) -> confirmEscalation(...))` → `QuboEngine.derive(ctx, DeriveOptions.defaults().withProgress(this::publish).withConfirm((from,to,exp) -> confirmEscalation(parent, from, to, exp)))`
  - `QuboCli.java:81-85` (5-arg call with `workerOverride`) → `DeriveOptions.defaults().withProgress(...).withStructuredProgress(bar).withConfirm((from,to,exp)->true).withWorkerOverride(...)` (re-check exact current args before editing, line numbers drift)
  - `QuboEngineTest.java` — 6 call sites at lines 37, 66, 79, 90, 94, 109, 126 — each becomes `DeriveOptions.defaults()` plus whichever `withX` the test needs (e.g. line 79's `(from,to,expected) -> false` becomes `.withConfirm((from,to,expected) -> false)`).

## Files Changed

| File | Change |
|---|---|
| `src/main/java/org/tzi/use/plugin/use2qubo/qubo/engine/QuboEngine.java` | add `DeriveOptions`, collapse 5 overloads → 1, update `deriveWithClearedState` |
| `src/main/java/org/tzi/use/plugin/use2qubo/action/DeriveQuboAction.java` | update call site |
| `src/main/java/org/tzi/use/plugin/use2qubo/cli/QuboCli.java` | update call site |
| `src/test/java/org/tzi/use/plugin/use2qubo/qubo/QuboEngineTest.java` | update 6 call sites |

## Acceptance criteria / Verification

- Build: `mvn -q -pl tools/use2qubo compile` (or repo's configured build command) succeeds with no leftover references to the deleted overloads.
- Test: `QuboEngineTest` passes unchanged in assertions — this is a pure interface refactor, no behavior change expected.
- Spot-check: `derive`'s public surface is exactly one method (plus `evaluateTrue`), `DeriveOptions.defaults()` reproduces every current default.

## Commit

Once done, `git commit` the changes with a commit message of 15 words max.
