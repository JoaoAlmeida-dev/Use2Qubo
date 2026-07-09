# JAVA-016 — "Try It" tab: arbitrary-input QUBO vs OCL objective comparison

**Status:** Open
**Priority:** Medium
**Depends on:** JAVA-014 (`SandboxSystemFactory`), JAVA-015 (derive isolation — `ctx.state` is a sandbox by the time this tab runs)
**Context:** `use2qubo` derives QUBO coefficients from an OCL objective/invariants by sampling (`QuboEngine`), then shows the result in 5 read-only tabs (`QuboMatrixView` → Matrix/Terms/Graph/Sampling/Exactness). There's no way for a user to punch in an arbitrary binary assignment and see: (a) what `q(x)` evaluates to, (b) what the real OCL objective/penalty evaluate to on the live USE model, (c) whether they agree. `ExactnessTabPanel` shows this only for the fixed random held-out points chosen during derivation, not for user-chosen vectors. This ticket adds a 6th tab, "Try It", for interactive testing.

## Key existing pieces reused

- `QuboResult.eval(int[] x)` (`qubo/result/QuboResult.java:76`) — q(x) evaluator, already handles linear+quadratic incl. ancillas.
- `QuboEngine`'s private `evalCost`/`evalPenalty`/`withTemporaryLinks`/`buildFlatVars`/`compileObjective`/`saveAndStripLinks`/`restoreLinks` — do exactly the "set x, evaluate true OCL objective/penalty, restore state" cycle needed; just not currently public.
- `QuboContext` (`qubo/context/QuboContext.java`) — holds `model`/`state`/`decisionVars`/`objectiveExpr`/`minimise`, needed to re-evaluate OCL. **Not currently passed to `QuboMatrixView`** — only `QuboResult` is.
- `Quadratizer.Result.ancillaPairs` — pair of var-indices each ancilla encodes (`y = x_a AND x_b`). Computed during derive but **discarded**, not stored on `QuboResult`. Needed to auto-derive ancilla bits from a user's original-variable assignment.
- `ui/tabs/ExactnessTabPanel.java` and `ui/ViewFormatUtil.java` — styling/colour conventions to match (green/red match indicators, stat labels).
- `SandboxSystemFactory` (JAVA-014) — reused here for the live preview diagram instead of bespoke inline sandbox-building code.

---

## 1. Thread `QuboContext` through to the UI

- `action/DeriveQuboAction.java:120` — change `new QuboMatrixView(result)` to `new QuboMatrixView(result, ctx)` (ctx already in scope there).
- `QuboMatrixView` constructor takes `(QuboResult result, QuboContext ctx)`, passes both into the new tab.
- No other tab needs `ctx`; only the new one does.

## 2. Persist ancilla pair info on `QuboResult`

- Add `public final List<int[]> ancillaPairs;` to `QuboResult` (empty list when `nAncillaVars == 0`), threaded through constructor + `withDerivationMs`.
- In `QuboEngine.buildResult`, pass `qz.ancillaPairs` (already computed on `Quadratizer.Result`, currently dropped) into the new `QuboResult` field for the `degree > 2` branch; empty list for the `degree <= 2` branch.

## 3. Public "evaluate true objective at x" entry point on `QuboEngine`

```java
public static final class TrueEval {
    public final double cost;      // objective, sign already flipped per ctx.minimise
    public final double penalty;   // raw violated-invariant count (unweighted)
    public double weighted(double B) { return cost + B * penalty; }
}

public static TrueEval evaluateTrue(QuboContext ctx, int[] x) throws Exception {
    // x.length must equal ctx.nVars (original decision vars only, no ancillas)
}
```
Internally: `buildFlatVars(ctx)`, `compileObjective(ctx, null)`, `new Evaluator()`, `saveAndStripLinks(ctx)` → `evalCost`/`evalPenalty` → `restoreLinks` in `finally`. Straight extraction of the body already used inside `deriveWithClearedState`/`checkExactness`, made callable on demand for one vector.

Because `ctx.state` is the JAVA-015 sandbox by the time this runs, this evaluates against the sandbox too — consistent with `derive()`, and fully isolated from whatever the user has open elsewhere.

## 4. New tab: `ui/tabs/TryItTabPanel.java`

Constructor: `TryItTabPanel(QuboResult result, QuboContext ctx)`.

Layout (mirrors `SamplingTabPanel`'s split-pane / table conventions):
- **Left**: one row per *original* decision variable (`ctx.nVars` of them, labels from `result.varLabels.subList(0, ctx.nVars)`), each a toggle button/checkbox for 0/1. Buttons: "Randomize", "Reset to 0", "Reset to 1".
- **Ancilla row (only if `result.nAncillaVars > 0`)**: read-only display of each ancilla's derived bit (`x_a AND x_b` from `result.ancillaPairs`), auto-recomputed whenever an original bit changes. Label them from `result.varLabels.subList(ctx.nVars, result.nVars)`.
- **Evaluate button** (explicit button, not live-update on toggle — OCL eval briefly mutates the sandbox, run on a `SwingWorker` like `QuboMatrixView`'s Save action, disabling inputs while running).
- **Results panel**: stat labels for `q(x)` (via `result.eval(fullX)`), true cost `f(x)`, penalty count, weighted true value `f(x) + B*penalty`, and a match/mismatch badge (`|q(x) - weighted| < EPS`) styled like the exact/inexact badge in `QuboMatrixView.buildStatsPanel`. On mismatch, a short note pointing at `ExactnessTabPanel`/known non-exactness causes (reuse wording style from `QuboEngine.logExactnessOutcome`).
- **Bottom/right: embedded sandbox object diagram** (below), rendering the current toggle state visually.

Errors (e.g. OCL eval exception) shown via the same `JOptionPane` pattern already used for save failures.

## 5. Sandbox object diagram (visual preview, reuses `SandboxSystemFactory`)

- `NewObjectDiagramView(MainWindow, MSystem)` binds to one `MSystem` and registers on **that system's own Guava `EventBus`** (`MSystem.getEventBus()`, confirmed via `javap`); each `MSystem` owns a private `EventBus`, so this preview system is fully event-isolated from the real session's.
- On each "Evaluate" click: `SandboxSystemFactory.build(ctx.model, ctx.state, ctx.objectsByClass, /*copyAttributes*/ false, ctx.fixedLinks)`, then add the currently-toggled decision-var links using the returned `byName` map — same association/object-pair logic already in `QuboEngine.buildFlatVars`/`withTemporaryLinks`.
- Attribute values are **not** copied (`copyAttributes=false`) — the diagram is structure-only (which links are active for the current `x`), not a data inspector; build the sandbox's `ObjDiagramOptions` with `setShowAttributes(false)` (confirmed field on `DiagramOptions`, `ObjDiagramOptions extends DiagramOptions`).
- Rebuild from scratch on every "Evaluate" click (small scenarios: ≤~300 vars, tens of objects) rather than incrementally diffing — avoids maintaining a persistent sandbox-object name-map across toggles. Swap the panel's embedded `NewObjectDiagramView`/`getDiagram()` component for the freshly built one each time.
- This diagram is purely illustrative of link structure; the actual `q(x)` vs. true-objective comparison still comes from `QuboEngine.evaluateTrue` (step 3) against `ctx`'s own sandbox (JAVA-015), unaffected by this separate preview sandbox.

## 6. Register tab

`QuboMatrixView` constructor: `tabs.addTab("Try It", new TryItTabPanel(result, ctx));` after Exactness.

## Files Changed

| File | Change |
|---|---|
| `action/DeriveQuboAction.java` | `new QuboMatrixView(result, ctx)` |
| `ui/QuboMatrixView.java` | Constructor takes `ctx`; register "Try It" tab |
| `qubo/result/QuboResult.java` | Add `ancillaPairs` field, threaded through constructor + `withDerivationMs` |
| `qubo/engine/QuboEngine.java` | Pass `qz.ancillaPairs` into `buildResult`'s `QuboResult`; new public `evaluateTrue`/`TrueEval` |
| `ui/tabs/TryItTabPanel.java` | New |

## Implementation notes for whoever picks this up

- No `ui/tabs/*` panel in this codebase has a unit test today (`src/test/java/.../qubo/` only covers `qubo/*` — `SamplingTabPanel`, `ExactnessTabPanel`, `MatrixTabPanel`, `QuboGraphPanel` are all untested by the existing convention). Don't introduce Swing/AWT test infrastructure for `TryItTabPanel` — verify it manually per the checklist below, consistent with how the other 5 tabs were verified.
- `evaluateTrue`'s signature should declare `throws Exception`, matching every other OCL-evaluating method already in `QuboEngine` (`evalCost`, `evalPenalty`, `derive` itself) — don't introduce a narrower checked exception type just for this one method.
- **Recommended manual-test fixture: `examples/MaxClique`.** Only 10 decision variables (`Solution`-`Vertex` `Contains` links), no ancillas at `max_degree=3`'s best-effort quadratization only if inexact — actually check `result.nAncillaVars` after deriving; either way it's small enough to toggle every bit by hand and hand-verify `q(x)` against `result.eval(x)`. `examples/GarageTrucks` (247 vars post-ancilla) is too large to toggle exhaustively by hand; use it only for a quick "does the tab open and not crash on a big scenario" smoke check, not for hand-verified numbers.
- The known feasible assignment for MaxClique is the clique `{v3,v4,v6,v7,v9}` (per `articles/qmod_2026/CLAUDE.md`'s "Key Claims" section) — toggle exactly those five `Contains(Solution,Vertex)` decision variables on, rest off, as the "known scenario assignment" case in the verification checklist below.

## Acceptance criteria / Verification

- Build the plugin (`tools/build-plugin.ps1`) and load into USE with `examples/MaxClique` (primary) and `examples/GarageTrucks` (smoke check only, see note above).
- Run the existing `.cmd` scenario, trigger "Derive QUBO Matrix", open the new "Try It" tab.
- Toggle bits to reproduce the all-zero vector and the known clique assignment `{v3,v4,v6,v7,v9}`; confirm `q(x)` matches `result.eval(x)` computed by hand for both cases, and confirm the match badge agrees with `result.exact`/`ExactnessTabPanel` on those vectors (MaxClique is exported `exact=false`, so expect the mismatch note to appear, not a false "PASS").
- Toggle an arbitrary/infeasible vector (e.g. all-ones) and confirm penalty count increases and cost/OCL evaluation doesn't throw (or fails gracefully with a dialog).
- Confirm the sandbox object-diagram preview updates on "Evaluate" and shows exactly the links implied by the current toggle state, with no attribute values displayed (`setShowAttributes(false)`).
- Confirm opening/using this tab doesn't mutate the real model: run "Derive QUBO Matrix" again afterwards on the same session and check the result is identical to the first derivation (proves the Try-It sandbox and the derive-time sandbox from JAVA-015 stayed isolated from each other and from the live session).

## Commit

Once done, `git commit` the changes with a commit message of 15 words max.
