package org.tzi.use.plugin.use2qubo.qubo.engine;

import org.tzi.use.plugin.use2qubo.qubo.engine.exactness.ExactnessOutcome;
import org.tzi.use.plugin.use2qubo.qubo.engine.sampling.VarSet;
import org.tzi.use.plugin.use2qubo.qubo.result.ExactnessPoint;
import org.tzi.use.plugin.use2qubo.qubo.result.QuboResult;
import org.tzi.use.plugin.use2qubo.qubo.result.SampleRecord;
import org.tzi.use.plugin.use2qubo.util.PluginLog;
import org.tzi.use.plugin.use2qubo.util.QuboConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility for assembling QUBO results from polynomial coefficients, with quadratization
 * and exactness verification.
 */
final class ResultAssembler {

    private static final double EPS = QuboConstants.EPS;

    private ResultAssembler() {}

    /**
     * Trims near-zero coefficients (per EPS), quadratizes if needed, and assembles the final
     * QuboResult.
     */
    static QuboResult buildResult(int n, int nSamples, Map<VarSet, Double> combined,
            List<String> varLabels, double B, List<SampleRecord> costSamples,
            List<SampleRecord> penaltySamples, ExactnessOutcome exactnessOutcome,
            boolean degreeExact, int degree) {

        double constant = combined.getOrDefault(VarSet.EMPTY, 0.0);
        List<ExactnessPoint> exactnessPoints = exactnessOutcome.points;

        if (degree <= 2) {
            Map<Integer, Double> linearMap = new LinkedHashMap<>();
            Map<String, Double> quadMap = new LinkedHashMap<>();
            for (Map.Entry<VarSet, Double> e : combined.entrySet()) {
                VarSet J = e.getKey();
                if (J.degree() == 1 && Math.abs(e.getValue()) >= EPS) {
                    linearMap.put(J.vars()[0], e.getValue());
                } else if (J.degree() == 2 && Math.abs(e.getValue()) >= EPS) {
                    int[] v = J.vars();
                    quadMap.put(v[0] + "," + v[1], e.getValue());
                }
            }
            return new QuboResult(n, nSamples, degreeExact, constant, linearMap, quadMap,
                    varLabels, B, 0L, costSamples, penaltySamples, exactnessPoints, degree, 0,
                    0.0, Collections.emptyList(), exactnessOutcome.method,
                    exactnessOutcome.matchCount, exactnessOutcome.totalCount);
        }

        Quadratizer.Result qz = Quadratizer.reduce(n, combined, varLabels);
        boolean quadExact = verifyQuadratization(qz, n, exactnessPoints);
        boolean exact = degreeExact && quadExact;
        if (!quadExact) {
            PluginLog.warn("QuboEngine: quadratization verification FAILED — the reduced QUBO does not "
                    + "reproduce the degree-" + degree + " polynomial on held-out points.");
        } else {
            PluginLog.info("Quadratization verification: PASS (nAncilla=" + qz.nAncilla
                    + ", penaltyWeight=" + qz.penaltyWeight + ")");
        }

        int totalVars = n + qz.nAncilla;
        List<String> extendedLabels = new ArrayList<>(varLabels);
        extendedLabels.addAll(qz.ancillaLabels);

        Map<Integer, Double> linearMap = new LinkedHashMap<>();
        for (int i = 0; i < totalVars; i++) {
            if (Math.abs(qz.lin[i]) >= EPS) linearMap.put(i, qz.lin[i]);
        }
        Map<String, Double> quadMap = new LinkedHashMap<>();
        qz.forEachQuadTerm((i, j, coeff) -> {
            if (Math.abs(coeff) >= EPS) quadMap.put(i + "," + j, coeff);
        });

        return new QuboResult(totalVars, nSamples, exact, qz.constant, linearMap, quadMap,
                extendedLabels, B, 0L, costSamples, penaltySamples, exactnessPoints, degree,
                qz.nAncilla, qz.penaltyWeight, qz.ancillaPairs, exactnessOutcome.method,
                exactnessOutcome.matchCount, exactnessOutcome.totalCount);
    }

    /**
     * Confirms the quadratized QUBO reproduces the true f(x) on the same held-out/exhaustive
     * points already collected for degree-K exactness, by setting each ancilla bit to the exact
     * product it encodes (Rosenberg's substitution guarantees the minimum over the ancilla
     * reaches this value, so no actual minimisation is needed to check it). Only re-verifies
     * the (possibly truncated) stored diagnostic points, not the full exhaustive sweep.
     */
    static boolean verifyQuadratization(Quadratizer.Result qz, int n,
            List<ExactnessPoint> exactnessPoints) {
        if (qz.nAncilla == 0) return true;
        for (ExactnessPoint p : exactnessPoints) {
            if (p.evalFailed) continue;
            double[] full = new double[n + qz.nAncilla];
            for (int i = 0; i < n; i++) full[i] = p.vector[i];
            for (int k = 0; k < qz.nAncilla; k++) {
                // ancilla pairs are encoded positionally in ancillaLabels' creation order;
                // recompute the product directly from the already-resolved earlier entries
                // of full[].
                full[n + k] = ancillaProduct(qz, k, full);
            }
            double qx = evalQuadratic(qz, full);
            if (Math.abs(qx - p.fx) >= EPS) return false;
        }
        return true;
    }

    static double ancillaProduct(Quadratizer.Result qz, int k, double[] full) {
        int[] pair = qz.ancillaPairs.get(k);
        return full[pair[0]] * full[pair[1]];
    }

    static double evalQuadratic(Quadratizer.Result qz, double[] x) {
        double[] r = {qz.constant};
        for (int i = 0; i < qz.lin.length; i++) r[0] += qz.lin[i] * x[i];
        qz.forEachQuadTerm((i, j, coeff) -> r[0] += coeff * x[i] * x[j]);
        return r[0];
    }
}
