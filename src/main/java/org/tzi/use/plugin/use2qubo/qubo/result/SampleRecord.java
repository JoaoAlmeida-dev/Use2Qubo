package org.tzi.use.plugin.use2qubo.qubo.result;

/**
 * One raw OCL evaluation captured during AutoQUBO sampling.
 * Instances live in either {@link QuboResult#costSamples} or {@link QuboResult#penaltySamples};
 * {@link #rawValue} is the cost or penalty value respectively — never both.
 *
 * <p>Phase label conventions:
 * <ul>
 *   <li>{@code "cost_const"} / {@code "pen_const"} — constant term; derivedI=derivedJ=-1</li>
 *   <li>{@code "cost_lin_i=k"} / {@code "pen_lin_i=k"} — linear (diagonal); derivedI=derivedJ=k</li>
 *   <li>{@code "cost_quad_i=a_j=b"} / {@code "pen_quad_i=a_j=b"} — quadratic; derivedI=a, derivedJ=b</li>
 *   <li>{@code "cost_deg3_a_b_c"} / {@code "pen_deg3_a_b_c"} (and higher) — degree ≥ 3 probe,
 *       one non-matrix term; derivedI=derivedJ=-2, full variable tuple in {@link #termVars}</li>
 * </ul>
 *
 * <p>Derived-term display: (-1,-1) → "c"; (i,i) → "Q[i,i]"; (i,j) i≠j → "Q[i,j]";
 * degree ≥ 3 → "Q[i,j,k,...]" built from {@link #termVars}.
 *
 * <p>The full binary assignment x ∈ {0,1}^n is not stored: every sampled point is m-hot by
 * construction (1s exactly at {@link #termVars}, 0 elsewhere), so {@link #toVector} rebuilds it
 * on demand instead of every instance carrying a redundant dense {@code int[n]}.
 */
public final class SampleRecord {

    /** Phase label encoding pass (cost/pen) and term type. */
    public final String phase;
    /** Raw OCL evaluation — cost value if in costSamples, penalty value if in penaltySamples. */
    public final double rawValue;
    /** Q row index this sample contributes to; -1 for the constant term, -2 for a degree-3+ term. */
    public final int    derivedI;
    /** Q col index; equals derivedI for linear (diagonal) terms; -1 for constant, -2 for degree-3+. */
    public final int    derivedJ;
    /** Full sorted variable-index tuple for this term (empty for constant, one entry for linear, etc.). */
    public final int[]  termVars;

    public SampleRecord(String phase, double rawValue, int derivedI, int derivedJ) {
        this(phase, rawValue, derivedI, derivedJ, defaultTermVars(derivedI, derivedJ));
    }

    public SampleRecord(String phase, double rawValue, int derivedI, int derivedJ, int[] termVars) {
        this.phase     = phase;
        this.rawValue  = rawValue;
        this.derivedI  = derivedI;
        this.derivedJ  = derivedJ;
        this.termVars  = termVars.clone();
    }

    /** Rebuilds the dense binary assignment for n variables: 1 at each {@link #termVars} index, 0 elsewhere. */
    public int[] toVector(int n) {
        int[] v = new int[n];
        for (int idx : termVars) v[idx] = 1;
        return v;
    }

    private static int[] defaultTermVars(int derivedI, int derivedJ) {
        if (derivedI == -1) return new int[0];
        if (derivedI == derivedJ) return new int[]{derivedI};
        return new int[]{derivedI, derivedJ};
    }
}
