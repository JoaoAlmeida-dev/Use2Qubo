package org.tzi.use.plugin.use2qubo.qubo;

/**
 * One held-out evaluation point from the exactness check.
 * Collected by {@code QuboEngine.checkExactness}; stored in {@link QuboResult#exactnessPoints}.
 *
 * <p>If the OCL evaluator threw during this point, {@link #evalFailed} is {@code true}
 * and {@link #fx}/{@link #qx} are {@link Double#NaN}. These points count as failures
 * when deriving the {@link QuboResult#exact} flag.
 */
public final class ExactnessPoint {

    /** Held-out binary vector x ∈ {0,1}^n. Hamming weight ≥ 3 (not a training sample). */
    public final int[]   vector;
    /** True objective+penalty f(x) = cost(x) + B·penalty(x). NaN if evalFailed. */
    public final double  fx;
    /** QUBO polynomial approximation q(x). NaN if evalFailed. */
    public final double  qx;
    /** True if the OCL evaluator threw an exception for this vector. */
    public final boolean evalFailed;

    public ExactnessPoint(int[] vector, double fx, double qx) {
        this.vector     = vector.clone();
        this.fx         = fx;
        this.qx         = qx;
        this.evalFailed = false;
    }

    /** Constructor for a point where OCL evaluation failed. */
    public ExactnessPoint(int[] vector) {
        this.vector     = vector.clone();
        this.fx         = Double.NaN;
        this.qx         = Double.NaN;
        this.evalFailed = true;
    }

    /** Absolute error |f(x) - q(x)|; NaN if evalFailed. */
    public double error() {
        return evalFailed ? Double.NaN : Math.abs(fx - qx);
    }
}
