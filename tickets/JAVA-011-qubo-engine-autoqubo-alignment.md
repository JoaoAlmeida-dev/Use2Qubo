# JAVA-011 — QuboEngine AutoQUBO Alignment

**Status:** Done  
**Priority:** Medium  
**Depends on:** JAVA-007 (QuboEngine v2)  
**Context:** Comparison of `QuboEngine.java` against the official AutoQUBO reference implementation (`FujitsuResearch/autoqubo`) revealed three deviations: a mislabelled penalty weight method, a weak exactness check, and a known limitation in penalty function expressiveness.

---

## Problems

### 1. Penalty weight comment claims "Verma-Lewis" but implements "sum"

`QuboEngine.java` line 130:

```java
// --- Compute B from cost coefficients (Verma-Lewis bound) ---
double costRange = 0.0;
for (double ci : costLin)    costRange += Math.abs(ci);
for (double[] row : costQuad)
    for (double cij : row)   costRange += Math.abs(cij);
double B = costRange + 1.0;
```

This computes `sum(|all cost coefficients|) + 1` — equivalent to AutoQUBO's `"sum"` penalty method plus 1.

The actual Verma-Lewis bound (`penalty_weights.py:verma_lewis()`) is a **per-row maximum**:

```python
pos_sum = [C[i][i] + C[i, i+1:][C[i, i+1:] > 0].sum() for i in range(n)]
neg_sum = [-C[i][i] - C[i, i+1:][C[i, i+1:] < 0].sum() for i in range(n)]
return max(pos_sum + neg_sum)
```

Verma-Lewis B ≤ sum-of-all B, often significantly smaller.  
Large B flattens the cost landscape relative to the penalty and degrades annealer performance — the QUBO is correct but not optimal for hardware.

**Fix:** Implement true Verma-Lewis and update the class-level Javadoc.

```java
// --- Compute B via Verma-Lewis per-row max (Pauckert et al., GECCO '23 §2.1) ---
double B = 1.0;
for (int i = 0; i < n; i++) {
    double posRow = costLin[i] > 0 ? costLin[i] : 0.0;
    double negRow = costLin[i] < 0 ? -costLin[i] : 0.0;
    for (int j = i + 1; j < n; j++) {
        double cij = costQuad[i][j];
        if (cij > 0) posRow += cij;
        else         negRow += -cij;
    }
    B = Math.max(B, Math.max(posRow, negRow));
}
B += 1.0;
```

**Files:** `qubo/QuboEngine.java`

---

### 2. Exactness check samples training vectors

`QuboEngine.java` line 243–247 generates 20 random binary vectors with no Hamming-weight filter:

```java
for (int i = 0; i < n; i++) x[i] = rand.nextInt(2);
```

0-hot, 1-hot, and 2-hot vectors are guaranteed correct by construction (they are training samples). For small n (e.g. n = 6: 64 vectors total, ~28 are 0/1/2-hot = ~44%), a large fraction of the 20 random points may be training samples — the check is weaker than it appears.

AutoQUBO's `test_qubo_matrix` explicitly excludes training samples:

```python
if sum(sample) > 2:   # only ≥3-hot vectors not in training set
    test_samples.add(sample)
```

**Fix:** Skip vectors with Hamming weight ≤ 2.

```java
// reject training-set vectors (0/1/2-hot are exact by construction)
int ones = 0;
for (int v : x) ones += v;
if (ones <= 2) { k--; continue; }
```

**Files:** `qubo/QuboEngine.java`

---

### 3. Document boolean penalty limitation clearly (no code change required)

`computePenalty` counts boolean OCL invariant violations (+1 per object per invariant). AutoQUBO uses quadratic-form penalties, e.g. `(sum - 1)^2`, which are naturally degree-2 in the binary variables. A boolean step-function is not degree-2 representable when the invariant body involves sums of many decision variables — this is why the exactness check can fail.

The warning message at line 188 already describes this. No code change needed, but the class-level Javadoc should explicitly state:

> OCL invariants must be degree-2 in the decision variables for the exactness check to pass. Invariants that check a boolean condition over a sum of many variables (e.g. `self.bins->size() = k`) are not representable as degree-2 polynomials; reformulate as integer-valued violation counts (e.g. `(self.bins->size() - k).abs()`) or ensure each invariant instance references at most two decision variables.

**Files:** `qubo/QuboEngine.java` (Javadoc only)

---

## Files Changed

| File | Change |
|---|---|
| `qubo/QuboEngine.java` | Fix B calculation (Verma-Lewis); fix exactness check sampling; update Javadoc |

No changes to `QuboResult`, `QuboContext`, `QuboMatrixView`, `DeriveQuboAction`, or any UI class.

---

## Verification

1. `mvn clean package` — no compile errors.
2. Load `GarbageTruckRouting.use` + config; run "Derive QUBO Matrix".
3. **B value:** logged B should be ≤ previous B for the same model (Verma-Lewis ≤ sum bound).
4. **Exactness check:** confirm all 20 test vectors have Hamming weight ≥ 3 (add temporary log if needed).
5. **Correctness unchanged:** `QuboResult.exact` still true for the garbage truck scenario.
6. **Regression:** Matrix, Graph, Terms, and Stats panels unaffected.
