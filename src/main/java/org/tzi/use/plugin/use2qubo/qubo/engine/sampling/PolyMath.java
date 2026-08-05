package org.tzi.use.plugin.use2qubo.qubo.engine.sampling;

import org.tzi.use.plugin.use2qubo.util.PluginLog;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Static utility methods for pseudo-Boolean polynomial manipulation and evaluation.
 */
public final class PolyMath {

    private PolyMath() {}

    /**
     * Verma-Lewis per-row max penalty weight (Pauckert et al., GECCO '23 §2.1), computed directly
     * off the sparse coefficient map instead of a dense {@code n x n} matrix (which at large n is
     * the dominant allocation for a value that only needs O(n) working memory).
     * For each row i: posRow = lin[i]+ + sum of positive quad[i][j] (j>i only, matching the
     *                  upper-triangular convention: each quadratic coefficient is attributed to
     *                  its lower-index row, never both);
     *                 negRow = |lin[i]-| + sum of |negative quad[i][j]|.
     * B = max over all rows of max(posRow, negRow) + 1.
     * Tighter than the global sum bound; smaller B = better annealing landscape.
     */
    public static double computePenaltyWeight(int n, Map<VarSet, Double> costCoeffs) {
        double[] lin = new double[n];
        double[] posRowQuad = new double[n];
        double[] negRowQuad = new double[n];
        for (Map.Entry<VarSet, Double> e : costCoeffs.entrySet()) {
            VarSet J = e.getKey();
            double c = e.getValue();
            if (J.degree() == 1) {
                lin[J.vars()[0]] += c;
            } else if (J.degree() == 2) {
                int i = J.vars()[0]; // vars() is sorted ascending, so vars()[0] < vars()[1]
                if (c > 0) posRowQuad[i] += c;
                else       negRowQuad[i] += -c;
            }
        }

        double B = 1.0;
        for (int i = 0; i < n; i++) {
            double posRow = (lin[i] > 0 ? lin[i] : 0.0) + posRowQuad[i];
            double negRow = (lin[i] < 0 ? -lin[i] : 0.0) + negRowQuad[i];
            B = Math.max(B, Math.max(posRow, negRow));
        }
        B += 1.0;
        PluginLog.info("QuboEngine: B=" + B + " (Verma-Lewis per-row max)");
        return B;
    }

    /** Merges cost(x) + B * penalty(x) into a single pseudo-Boolean polynomial, any degree. */
    public static Map<VarSet, Double> combine(Map<VarSet, Double> cost, Map<VarSet, Double> penalty, double B) {
        Map<VarSet, Double> combined = new LinkedHashMap<>(cost);
        for (Map.Entry<VarSet, Double> e : penalty.entrySet()) {
            combined.merge(e.getKey(), B * e.getValue(), Double::sum);
        }
        return combined;
    }

    /** Evaluates a pseudo-Boolean polynomial (any degree) at binary vector x. */
    public static double evalPoly(Map<VarSet, Double> coeffs, int[] x) {
        double sum = 0.0;
        for (Map.Entry<VarSet, Double> e : coeffs.entrySet()) {
            boolean allOne = true;
            for (int v : e.getKey().vars()) {
                if (x[v] == 0) { allOne = false; break; }
            }
            if (allOne) sum += e.getValue();
        }
        return sum;
    }
}
