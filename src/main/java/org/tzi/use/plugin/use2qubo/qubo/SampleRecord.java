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
 * </ul>
 *
 * <p>Derived-term display: (-1,-1) → "c"; (i,i) → "Q[i,i]"; (i,j) i≠j → "Q[i,j]".
 */
public final class SampleRecord {

    /** Binary assignment x ∈ {0,1}^n for this sample. */
    public final int[]  vector;
    /** Phase label encoding pass (cost/pen) and term type. */
    public final String phase;
    /** Raw OCL evaluation — cost value if in costSamples, penalty value if in penaltySamples. */
    public final double rawValue;
    /** Q row index this sample contributes to; -1 for the constant term. */
    public final int    derivedI;
    /** Q col index; equals derivedI for linear (diagonal) terms; -1 for the constant term. */
    public final int    derivedJ;

    public SampleRecord(int[] vector, String phase, double rawValue, int derivedI, int derivedJ) {
        this.vector    = vector.clone();
        this.phase     = phase;
        this.rawValue  = rawValue;
        this.derivedI  = derivedI;
        this.derivedJ  = derivedJ;
    }
}
