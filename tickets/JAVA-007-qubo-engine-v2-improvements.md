# JAVA-007 — QuboEngine: AutoQUBO v2 Improvements

## Goal

Apply two improvements from AutoQUBO v2 (Pauckert et al., GECCO '23) to `QuboEngine.java`, plus fixes surfaced by code review. Improves penalty weight correctness, solver result quality, and user-facing diagnostics.

## Fix 1 — Analytical Penalty Weight (Priority: High)

**File:** `QuboEngine.java`

**Current:**
```java
double B = ctx.nVars + 1.0;   // in evalF, line 268
```

**Problem:** `n+1` is the v1 MAX-CLIQUE heuristic. It assumes objective values are bounded by n (true only when the objective counts selected binary variables). For any domain where costs are real-valued (distances, weights, durations), this bound is violated and infeasible solutions can beat feasible ones in the final QUBO, making the solver return wrong answers. The tool must be domain-agnostic.

**Root cause:** `evalF` currently evaluates `obj(x) + B·penalty(x)` as a single black box. B cannot be derived from those combined coefficients because they already encode the penalty term.

**Fix (two-pass sampling):** sample cost and penalty separately; derive B from cost coefficients only; combine.

Sampling cost: `1 + n*(n+1)/2` evaluations of `evalCost(x)` only.  
Sampling penalty: another `1 + n*(n+1)/2` evaluations of `evalPenalty(x)` only.  
Total: `2 + n*(n+1)` evaluations (2× the current count — acceptable).

```java
// --- Pass 1: sample cost-only ---
// evalCost returns computeObjective only (no penalty), negated if maximising
double costC = evalCost(zeros, flatVars, ctx, evaluator, objExpr);
double[] costLin = new double[n];
double[][] costQuad = new double[n][n];

for (int i = 0; i < n; i++) {
    int[] ei = unitVector(n, i);
    costLin[i] = evalCost(ei, ...) - costC;
}
for (int i = 0; i < n; i++) {
    for (int j = i + 1; j < n; j++) {
        int[] eij = unitVector(n, i, j);
        costQuad[i][j] = evalCost(eij, ...) - costLin[i] - costLin[j] - costC;
    }
}

// --- Compute B from cost coefficients (sum method — Verma-Lewis bound) ---
// Constant term cancels in max-min, so exclude it.
double costRange = 0.0;
for (double ci : costLin)       costRange += Math.abs(ci);
for (double[] row : costQuad)
    for (double cij : row)      costRange += Math.abs(cij);
double B = costRange + 1.0;

// --- Pass 2: sample penalty-only ---
double penC = evalPenalty(zeros, ...);
double[] penLin = new double[n];
double[][] penQuad = new double[n][n];
// ... same structure as pass 1 ...

// --- Combine ---
double c         = costC + B * penC;
linCoeffs[i]     = costLin[i]     + B * penLin[i];
quadCoeffs[i][j] = costQuad[i][j] + B * penQuad[i][j];
```

Split helpers to replace current `evalF`:

```java
private static double evalCost(int[] x, List<DVPair> flatVars,
        QuboContext ctx, Evaluator evaluator, Expression objExpr) throws Exception {
    // insert/delete links same as current evalF
    double obj = computeObjective(objExpr, evaluator, state);
    if (!ctx.minimise) obj = -obj;
    return obj;
}

private static double evalPenalty(int[] x, List<DVPair> flatVars,
        QuboContext ctx, Evaluator evaluator) throws Exception {
    // insert/delete links same as current evalF
    return computePenalty(ctx, evaluator, state);
}
```

**Why sum method is correct:** for binary variables, `max(cost) − min(cost) ≤ Σᵢ|costLin[i]| + Σᵢ<ⱼ|costQuad[i][j]|`. Setting `B = costRange + 1` guarantees every 1-unit penalty violation exceeds the entire reachable objective range, regardless of domain or coefficient magnitude.

**Acceptance criterion:** for a small instance (n ≤ 6), enumerate all 2ⁿ vectors and confirm `min(f_combined over infeasible) > max(f_combined over feasible)`.

---

## Fix 2 — Surface Exactness Failure (Priority: Medium)

**File:** `QuboEngine.java:131-133`

**Current:**
```java
boolean exact = checkExactness(...);
PluginLog.info("Exactness check: " + (exact ? "PASS" : "FAIL"));
```

**Problem:** FAIL is logged at INFO and silently ignored. The user receives a potentially wrong QUBO with no visible warning. AutoQUBO v2 (§4.3) explicitly uses exactness failure as a debugging signal: the combined function is not degree-2, so sampling cannot recover it exactly.

**Fix:** log at WARN on failure with actionable diagnosis covering both root causes:

```java
if (!exact) {
    PluginLog.warn("QuboEngine: exactness check FAILED — q(x) ≠ f(x) on held-out points. "
        + "The combined objective+penalty is not representable as a degree-2 polynomial. "
        + "Common causes: "
        + "(1) An OCL invariant uses a boolean pass/fail condition — "
        + "reformulate by counting violations (integer sum) instead of returning true/false. "
        + "(2) The objective OCL expression contains interactions between 3+ decision variables "
        + "(e.g. a product of three link memberships) — decompose into pairwise terms.");
}
```

Also ensure `QuboResult.exact` is visible in the matrix dialog so the user sees PASS/FAIL without inspecting logs.

**Acceptance criterion:** running on a deliberately non-quadratic formulation (invariant returning Boolean, or cubic objective) shows WARN in log and FAIL indicator in UI.

---

## Fix 3 — Silent Failure in `computeObjective` (Priority: Medium)

**File:** `QuboEngine.java:301-311`

**Current:**
```java
} catch (Exception e) {
    // fall through — treat as 0
}
return 0.0;
```

**Problem:** if the OCL objective fails to evaluate (type error, missing variable, wrong return type), every `evalCost` call returns 0.0 silently. All cost coefficients become zero. The derived QUBO encodes only penalty — the solver minimises constraint violations and ignores the actual objective entirely. No warning is raised.

**Fix:** log at WARN and surface the failure:

```java
} catch (Exception e) {
    PluginLog.warn("QuboEngine: objective OCL evaluation failed — treating as 0.0. "
        + "Check that the objective expression returns Integer or Real. Error: " + e.getMessage());
}
return 0.0;
```

For stricter behaviour (recommended): propagate the exception so `derive()` fails fast rather than silently producing a wrong QUBO.

**Acceptance criterion:** deliberately break the objective OCL expression; confirm a WARN appears in the log.

---

## Fix 4 — Extract Duplicated Link-Strip Logic (Priority: Low)

**Files:** `QuboEngine.java:83-91` (in `derive()`) and `QuboEngine.java:176-182` (in `checkExactness()`)

**Problem:** both methods independently strip decision-var links from state before evaluation. Duplicated logic; divergence risk if stripping conditions change. `restoreLinks` already exists as a shared helper — same pattern should apply here.

**Fix:** extract private static helper:

```java
private static void stripDecisionLinks(QuboContext ctx) {
    for (DecisionVar dv : ctx.decisionVars) {
        MAssociation assoc = ctx.model.getAssociation(dv.association);
        if (assoc == null) continue;
        for (MLink link : new HashSet<>(ctx.state.linksOfAssociation(assoc).links())) {
            try { ctx.state.deleteLink(link); } catch (Exception e) { /* best effort */ }
        }
    }
}
```

Call from both `derive()` and `checkExactness()`.

**Acceptance criterion:** single code path for stripping; existing behaviour unchanged.

---

## Dependencies

JAVA-002 (`QuboEngine` — already implemented). No new external dependencies.

## References

- AutoQUBO v1: Moraglio, Georgescu, Sadowski. GECCO '22. §4.
- AutoQUBO v2: Pauckert, Ayodele, García, Georgescu, Parizy. GECCO '23. §1.1, §2.1.
- Verma-Lewis penalty method: Verma & Lewis. *Discrete Optimization* 44 (2022), 100594.
