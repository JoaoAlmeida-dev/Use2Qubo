package org.tzi.use.plugin.use2qubo.qubo.engine;

import org.tzi.use.plugin.use2qubo.util.QuboConstants;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reduces a pseudo-Boolean polynomial of degree &gt; 2 to an exact QUBO by Rosenberg pair
 * substitution (Rosenberg 1975; surveyed in Dattani, "Quadratization in discrete optimization
 * and quantum mechanics", arXiv:1901.04405 — cited as future work in {@link QuboEngine}'s
 * original Javadoc). One ancillary binary variable is introduced per distinct substituted pair
 * of variable indices and reused across every higher-order term that needs it, so a pair used
 * in several cubic (or higher) terms only costs one ancilla and one penalty block.
 *
 * <p>For a term {@code c * x_i * x_j * (rest)}, the pair {@code (i,j)} is replaced by a fresh
 * ancilla {@code y}, giving the degree-reduced term {@code c * y * (rest)}, plus a penalty
 * {@code delta * (x_i*x_j - 2*x_i*y - 2*x_j*y + 3*y)} that is minimised (to zero) exactly when
 * {@code y = x_i AND x_j}, and strictly positive otherwise. Terms of degree &gt; 3 are reduced
 * by repeating the substitution on the first two remaining variables (which may themselves be
 * ancillas from an earlier substitution) until only two variables remain.
 *
 * <p>The penalty weight {@code delta} used here is a conservative "big-M" bound (the L1 norm of
 * every degree ≥ 1 coefficient in the input polynomial, plus a margin) rather than a tight
 * per-ancilla bound — correct, but not necessarily minimal. Tightening this (e.g. a
 * Verma-Lewis-style per-ancilla bound, mirroring {@code QuboEngine.computePenaltyWeight}) is
 * future work.
 */
public final class Quadratizer {

    private Quadratizer() {}

    public static final class Result {
        /** Number of ancillary variables introduced (0 if the input was already degree ≤ 2). */
        public final int nAncilla;
        /** Constant term, carried through unchanged. */
        public final double constant;
        /** Linear coefficients, length {@code n + nAncilla}. */
        public final double[] lin;
        /** Quadratic coefficients, {@code (n+nAncilla) x (n+nAncilla)}; only {@code [i][j]}, i&lt;j, populated. */
        public final double[][] quad;
        /** Display labels for the ancillas, e.g. {@code "anc(RouteRoad(r1,e2),RouteRoad(r1,e3))"}. */
        public final List<String> ancillaLabels;
        /** For each ancilla k, the pair {@code {a,b}} (variable-space indices, possibly earlier
         *  ancillas themselves) it substitutes; {@code y_k = x_a AND x_b} at the optimum. */
        public final List<int[]> ancillaPairs;
        /** The penalty weight applied to every ancilla-consistency term. */
        public final double penaltyWeight;

        Result(int nAncilla, double constant, double[] lin, double[][] quad,
               List<String> ancillaLabels, List<int[]> ancillaPairs, double penaltyWeight) {
            this.nAncilla      = nAncilla;
            this.constant      = constant;
            this.lin           = lin;
            this.quad          = quad;
            this.ancillaLabels = ancillaLabels;
            this.ancillaPairs  = ancillaPairs;
            this.penaltyWeight = penaltyWeight;
        }
    }

    /**
     * @param n         number of original decision variables
     * @param coeffs    exact pseudo-Boolean polynomial coefficients of any degree, keyed by {@link VarSet}
     * @param varLabels display labels for the {@code n} original variables
     */
    public static Result reduce(int n, Map<VarSet, Double> coeffs, List<String> varLabels) {
        double constant = coeffs.getOrDefault(VarSet.EMPTY, 0.0);

        double totalAbs = 0.0;
        for (Map.Entry<VarSet, Double> e : coeffs.entrySet()) {
            if (e.getKey().degree() >= 1) totalAbs += Math.abs(e.getValue());
        }

        Map<Integer, Double> linAcc = new LinkedHashMap<>();
        Map<Long, Double> quadAcc = new LinkedHashMap<>();
        Map<Long, Integer> pairToAncilla = new LinkedHashMap<>();
        List<int[]> ancillaPairs = new ArrayList<>();

        for (Map.Entry<VarSet, Double> e : coeffs.entrySet()) {
            VarSet J = e.getKey();
            double c = e.getValue();
            if (J.degree() == 0 || Math.abs(c) < QuboConstants.EPS) continue;

            List<Integer> vars = new ArrayList<>();
            for (int v : J.vars()) vars.add(v);

            while (vars.size() > 2) {
                int a = vars.get(0);
                int b = vars.get(1);
                long key = pairKey(a, b);
                Integer ancIdx = pairToAncilla.get(key);
                if (ancIdx == null) {
                    ancIdx = ancillaPairs.size();
                    pairToAncilla.put(key, ancIdx);
                    ancillaPairs.add(new int[]{a, b});
                }
                int ancVar = n + ancIdx;
                List<Integer> rest = new ArrayList<>(vars.subList(2, vars.size()));
                rest.add(0, ancVar);
                vars = rest;
            }

            if (vars.size() == 1) {
                linAcc.merge(vars.get(0), c, Double::sum);
            } else {
                quadAcc.merge(pairKey(vars.get(0), vars.get(1)), c, Double::sum);
            }
        }

        int nAncilla = ancillaPairs.size();
        int total = n + nAncilla;
        double delta = totalAbs + QuboConstants.QUADRATIZATION_PENALTY_MARGIN;

        double[] lin = new double[total];
        double[][] quad = new double[total][total];
        for (Map.Entry<Integer, Double> e : linAcc.entrySet()) {
            lin[e.getKey()] += e.getValue();
        }
        for (Map.Entry<Long, Double> e : quadAcc.entrySet()) {
            int[] ij = unpackKey(e.getKey());
            quad[ij[0]][ij[1]] += e.getValue();
        }

        List<String> ancillaLabels = new ArrayList<>(nAncilla);
        for (int k = 0; k < nAncilla; k++) {
            int a = ancillaPairs.get(k)[0];
            int b = ancillaPairs.get(k)[1];
            int y = n + k;
            ancillaLabels.add("anc(" + label(a, varLabels, ancillaLabels, n) + ","
                    + label(b, varLabels, ancillaLabels, n) + ")");
            // Rosenberg penalty: delta * (x_a*x_b - 2*x_a*y - 2*x_b*y + 3*y), zero iff y = x_a AND x_b.
            addQuad(quad, a, b, delta);
            addQuad(quad, a, y, -2 * delta);
            addQuad(quad, b, y, -2 * delta);
            lin[y] += 3 * delta;
        }

        return new Result(nAncilla, constant, lin, quad, ancillaLabels, ancillaPairs, delta);
    }

    private static String label(int idx, List<String> varLabels, List<String> ancillaLabelsSoFar, int n) {
        if (idx < n) return idx < varLabels.size() ? varLabels.get(idx) : ("x" + idx);
        return ancillaLabelsSoFar.get(idx - n);
    }

    private static void addQuad(double[][] quad, int a, int b, double delta) {
        int i = Math.min(a, b);
        int j = Math.max(a, b);
        quad[i][j] += delta;
    }

    private static long pairKey(int a, int b) {
        int i = Math.min(a, b);
        int j = Math.max(a, b);
        return ((long) i << 32) | (j & 0xffffffffL);
    }

    private static int[] unpackKey(long key) {
        return new int[]{(int) (key >>> 32), (int) key};
    }
}
