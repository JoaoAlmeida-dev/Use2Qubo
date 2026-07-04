package org.tzi.use.plugin.use2qubo.qubo;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class QuboResult {

    public final int nVars;
    public final int nSamples;
    public final boolean exact;
    public final double constant;
    /** Linear coefficients: variable index → c_i (zero entries omitted). */
    public final Map<Integer, Double> linear;
    /** Quadratic coefficients: "i,j" (i<j) → c_{ij} (zero entries omitted). */
    public final Map<String, Double> quadratic;
    /** Variable labels in varIndex order: "AssocName(objA,objB)". */
    public final List<String> varLabels;
    /** Penalty weight B (Verma-Lewis per-row max). */
    public final double penaltyWeight;
    /** Wall-clock time for the full derive() call in milliseconds. */
    public final long derivationMs;
    /** Raw cost evaluations from pass 1; one record per sample vector. */
    public final List<SampleRecord> costSamples;
    /** Raw penalty evaluations from pass 2; one record per sample vector. */
    public final List<SampleRecord> penaltySamples;
    /** Held-out evaluation points from the exactness check; up to 20 records. */
    public final List<ExactnessPoint> exactnessPoints;
    /** Pseudo-Boolean polynomial degree actually explored (2 = no escalation needed). */
    public final int polyDegree;
    /** Number of Rosenberg-quadratization ancilla variables appended after the original nVars. */
    public final int nAncillaVars;
    /** Penalty weight applied to each ancilla-consistency term; 0 when no quadratization was needed. */
    public final double quadratizationPenalty;

    QuboResult(int nVars, int nSamples, boolean exact, double constant,
               Map<Integer, Double> linear, Map<String, Double> quadratic,
               List<String> varLabels, double penaltyWeight, long derivationMs,
               List<SampleRecord> costSamples, List<SampleRecord> penaltySamples,
               List<ExactnessPoint> exactnessPoints, int polyDegree, int nAncillaVars,
               double quadratizationPenalty) {
        this.nVars                 = nVars;
        this.nSamples               = nSamples;
        this.exact                  = exact;
        this.constant                = constant;
        this.linear                  = Collections.unmodifiableMap(linear);
        this.quadratic               = Collections.unmodifiableMap(quadratic);
        this.varLabels               = Collections.unmodifiableList(varLabels);
        this.penaltyWeight           = penaltyWeight;
        this.derivationMs            = derivationMs;
        this.costSamples             = Collections.unmodifiableList(costSamples);
        this.penaltySamples          = Collections.unmodifiableList(penaltySamples);
        this.exactnessPoints         = Collections.unmodifiableList(exactnessPoints);
        this.polyDegree              = polyDegree;
        this.nAncillaVars            = nAncillaVars;
        this.quadratizationPenalty   = quadratizationPenalty;
    }

    /** Returns copy of this result with derivationMs set to ms. */
    public QuboResult withDerivationMs(long ms) {
        return new QuboResult(nVars, nSamples, exact, constant,
                linear, quadratic, varLabels, penaltyWeight, ms,
                costSamples, penaltySamples, exactnessPoints, polyDegree, nAncillaVars,
                quadratizationPenalty);
    }

    /** Evaluate the QUBO polynomial q(x) = c + sum_i c_i*x_i + sum_{i<j} c_ij*x_i*x_j. */
    public double eval(int[] x) {
        double result = constant;
        for (Map.Entry<Integer, Double> e : linear.entrySet()) {
            result += e.getValue() * x[e.getKey()];
        }
        for (Map.Entry<String, Double> e : quadratic.entrySet()) {
            String[] parts = e.getKey().split(",");
            int i = Integer.parseInt(parts[0]);
            int j = Integer.parseInt(parts[1]);
            result += e.getValue() * x[i] * x[j];
        }
        return result;
    }

    @Override
    public String toString() {
        return String.format("QuboResult{nVars=%d, nSamples=%d, exact=%b, "
                + "constant=%.4f, |linear|=%d, |quadratic|=%d, "
                + "|costSamples|=%d, |penaltySamples|=%d, |exactnessPoints|=%d, "
                + "polyDegree=%d, nAncillaVars=%d}",
                nVars, nSamples, exact, constant, linear.size(), quadratic.size(),
                costSamples.size(), penaltySamples.size(), exactnessPoints.size(),
                polyDegree, nAncillaVars);
    }
}
