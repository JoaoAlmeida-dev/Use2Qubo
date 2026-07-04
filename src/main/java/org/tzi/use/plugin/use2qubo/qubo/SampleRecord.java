package org.tzi.use.plugin.use2qubo.qubo;

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
 */
public final class SampleRecord {

    /** Binary assignment x ∈ {0,1}^n for this sample. */
    public final int[]  vector;
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

    public SampleRecord(int[] vector, String phase, double rawValue, int derivedI, int derivedJ) {
        this(vector, phase, rawValue, derivedI, derivedJ, defaultTermVars(derivedI, derivedJ));
    }

    public SampleRecord(int[] vector, String phase, double rawValue, int derivedI, int derivedJ, int[] termVars) {
        this.vector    = vector.clone();
        this.phase     = phase;
        this.rawValue  = rawValue;
        this.derivedI  = derivedI;
        this.derivedJ  = derivedJ;
        this.termVars  = termVars.clone();
    }

    private static int[] defaultTermVars(int derivedI, int derivedJ) {
        if (derivedI == -1) return new int[0];
        if (derivedI == derivedJ) return new int[]{derivedI};
        return new int[]{derivedI, derivedJ};
    }
}
