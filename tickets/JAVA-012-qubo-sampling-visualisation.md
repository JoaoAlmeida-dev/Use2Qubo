# JAVA-012 — QUBO Sampling Visualisation

**Status:** Open  
**Priority:** Medium  
**Depends on:** JAVA-007 (QuboEngine v2), JAVA-011 (AutoQUBO alignment)  
**Context:** `QuboEngine` evaluates `2 + n*(n+1)` binary vectors during two-pass AutoQUBO sampling but discards all intermediate data. Only the extracted Q coefficients reach the UI. A domain expert inspecting the derived matrix has no way to trace a coefficient back to the raw OCL evaluations that produced it, verify that the correct objects are exercised per sample, or diagnose a failed exactness check beyond the green/red badge in the stats panel.

---

## Problems

### 1. No audit trail for sampled f(x) evaluations

`QuboEngine.derive()` calls `evalCost` and `evalPenalty` for each binary vector and immediately discards the raw values:

```java
// QuboEngine.java line 122
costLin[i] = evalCost(ei, flatVars, ctx, evaluator, objExpr) - costC;
```

The intermediate result — the actual OCL-evaluated objective value for the unit vector `eᵢ` — is never stored. If `costLin[i]` looks wrong, there is no way to recover `evalCost(eᵢ)` or inspect which links were inserted during that evaluation.

AutoQUBO's reference implementation (`qubo_matrix.py`) exposes raw sample evaluations via `self.samples_` and `self.evals_` attributes on the solver object, enabling post-hoc inspection.

**Fix:** Introduce a `SampleRecord` data class and collect one record per evaluation in `QuboEngine.derive()`. The record captures the binary vector, phase label, raw cost value, raw penalty value, and the Q-matrix index the sample contributes to:

```java
// qubo/SampleRecord.java (new file)
public final class SampleRecord {
    public final int[]   vector;       // binary assignment x ∈ {0,1}^n
    public final String  phase;        // "cost_const", "cost_lin_i=3", "pen_quad_i=1,j=2", etc.
    public final double  costValue;    // raw objective evaluation (pre-sign flip)
    public final double  penValue;     // raw penalty evaluation
    public final int     derivedI;     // Q row index this sample derives (-1 for constant)
    public final int     derivedJ;     // Q col index (-1 for linear or constant)
}
```

Collect in `derive()` immediately after each `evalCost`/`evalPenalty` call:

```java
// after each evalCost call — example for linear term:
double rawCost = evalCost(ei, flatVars, ctx, evaluator, objExpr);
double rawPen  = evalPenalty(ei, flatVars, ctx, evaluator);
samples.add(new SampleRecord(ei.clone(), "cost_lin_i=" + i, rawCost, rawPen, i, -1));
costLin[i] = rawCost - costC;
```

Note: collecting both cost and penalty in a single evaluation avoids extra state manipulations only if the two passes are merged. If kept separate (current architecture), collect cost and penalty values independently per pass and merge by vector index before storing in `QuboResult`.

**Files:** `qubo/SampleRecord.java` (new), `qubo/QuboEngine.java`, `qubo/QuboResult.java`

---

### 2. Exactness check results buried in logs

`checkExactness` writes pass/fail to `PluginLog` only:

```java
// QuboEngine.java line 205-212
if (!exact) {
    PluginLog.warn("QuboEngine: exactness check FAILED — q(x) ≠ f(x) on held-out points. …");
} else {
    PluginLog.info("Exactness check: PASS");
}
```

`QuboMatrixView` surfaces this only as a coloured badge (`exact` field on `QuboResult`). When the check fails, the domain expert sees a red badge but has no information about which held-out vector caused the failure, how large the discrepancy is, or whether it is one outlier or a systematic error.

AutoQUBO's test harness (`test_qubo_matrix.py`) stores the full comparison table — sample vector, true f(x), polynomial q(x), absolute error — and displays it in the test report.

**Fix:** Introduce an `ExactnessPoint` data class and collect one record per held-out vector in `checkExactness`:

```java
// qubo/ExactnessPoint.java (new file)
public final class ExactnessPoint {
    public final int[]  vector;   // held-out binary vector x
    public final double fx;       // true objective+penalty evaluation f(x)
    public final double qx;       // polynomial approximation q(x)

    public double error() { return Math.abs(fx - qx); }
}
```

Collect in `checkExactness` before the `exact = false` branch:

```java
ExactnessPoint pt = new ExactnessPoint(x.clone(), fx, qx);
exactnessPoints.add(pt);
if (Math.abs(fx - qx) >= EPS) {
    exact = false;
    // do not break — collect all 20 points for full diagnostic
}
```

Add `List<ExactnessPoint> exactnessPoints` to `QuboResult`. Change `checkExactness` signature to return `List<ExactnessPoint>` rather than `boolean`; derive `exact` from whether any point has `error() >= EPS`.

**Files:** `qubo/ExactnessPoint.java` (new), `qubo/QuboEngine.java`, `qubo/QuboResult.java`

---

### 3. Sampling grid structure not visible to user

The AutoQUBO sampling grid — which binary vectors are evaluated, in what order, and what phase they belong to — is described only in the class-level Javadoc:

```
Total evaluations: 2 + n*(n+1) (2× v1; acceptable).
```

A domain expert looking at the derived Q matrix has no way to answer:

- "Which objects are linked in the third sample?"
- "Did the quadratic sample for `(i=2, j=5)` insert the correct association links?"
- "What is the objective value when only truck T1 visits bin B3?"

This blocks manual verification and trust-building in the derivation.

**Fix:** Add a "Sampling" tab to `QuboMatrixView`. The tab contains two sub-panels stacked vertically:

**Top — Sample heatmap table:**

| # | Phase | x₀ | x₁ | … | xₙ₋₁ | Cost f_obj(x) | Penalty f_pen(x) | Derived term |
|---|---|---|---|---|---|---|---|---|
| 0 | cost_const | 0 | 0 | … | 0 | 12.0 | 3.0 | c |
| 1 | cost_lin_i=0 | 1 | 0 | … | 0 | 15.0 | 4.0 | Q[0,0] |
| … | … | … | … | … | … | … | … | … |

- Variable columns: cell coloured dark grey if `x[i]=1`, light grey if `x[i]=0`. Width capped at 20px per variable column (abbreviated).
- "Derived term" column: `c` for constant, `Q[i,i]` for linear (diagonal), `Q[i,j]` for quadratic.
- Rows grouped visually: cost pass (rows 0 … 1+n*(n-1)/2) separated by a divider from penalty pass.

**Bottom — Exactness check table:**

| # | x (Hamming wt) | f(x) | q(x) | \|error\| | Status |
|---|---|---|---|---|---|
| 0 | …0110… (wt=4) | 18.5 | 18.5 | 0.000 | ✓ |
| … | … | … | … | … | … |

- Header background: green if all pass, red if any fail.
- Worst-error row highlighted in light red.
- Tooltip on each row shows the full binary vector as `assoc(a,b)=1` variable assignments (using `varLabels` from `QuboResult`).

Implementation entry point: `QuboMatrixView.buildSamplingTab(QuboResult)` — new private method returning a `JComponent`. Add to the existing `JTabbedPane` after the "Graph" tab.

**Files:** `ui/QuboMatrixView.java`

---

## Files Changed

| File | Change |
|---|---|
| `qubo/SampleRecord.java` | New data class: vector, phase, costValue, penValue, derivedI, derivedJ |
| `qubo/ExactnessPoint.java` | New data class: vector, fx, qx, error() |
| `qubo/QuboResult.java` | Add `List<SampleRecord> samples` and `List<ExactnessPoint> exactnessPoints` fields; update constructor and `toString()` |
| `qubo/QuboEngine.java` | Collect SampleRecord per evalCost/evalPenalty call; refactor checkExactness to return `List<ExactnessPoint>`; derive `exact` flag from list |
| `ui/QuboMatrixView.java` | New "Sampling" tab via `buildSamplingTab(QuboResult)`; heatmap table + exactness table |

No changes to `QuboContext`, `QuboContextBuilder`, `QuboGraphPanel`, `QuboConfigView`, `DeriveQuboAction`, or any action/exporter class.

---

## Verification

1. `mvn clean package` — no compile errors.
2. Load `GarbageTruckRouting.use` + config; run "Derive QUBO Matrix".
3. **Sample count:** "Sampling" tab row count = `2 + n*(n+1)` total rows (split: `1 + n*(n-1)/2` cost rows, same penalty rows). Verify for known n (e.g. n=6 → 44 rows).
4. **Heatmap correctness:** Row labelled `cost_lin_i=k` must have `x[k]=1` and all others 0. Row `cost_quad_i=a,j=b` must have `x[a]=1`, `x[b]=1`, all others 0.
5. **Exactness table:** All 20 rows present. Hamming weight of each vector ≥ 3 (enforced by JAVA-011 fix). If `QuboResult.exact = true`, all status cells show ✓ and header is green.
6. **Regression:** Matrix, Terms, and Graph tabs unaffected; stats panel `exact` badge unchanged.
7. **Failure path:** Manually break one OCL invariant (e.g., edit `.use` to add a false-always invariant). Verify red header, worst-error row highlighted, and discrepancy value > 0 in exactness table.
