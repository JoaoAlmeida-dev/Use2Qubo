package org.tzi.use.plugin.use2qubo.qubo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Generalised AutoQUBO sampling (Moraglio et al., GECCO '22, Algorithm 1): samples a black-box
 * function at all {@code m}-hot binary vectors for {@code fromDegree <= m <= toDegree} and
 * recovers the coefficients of its exact pseudo-Boolean polynomial representation by
 * inclusion-exclusion, {@code c_J = f(x^J) - sum_{I subsetneq J} c_I}.
 *
 * <p>Degree 2 (the historical special case) reduces to exactly the original pairwise sampling
 * loop: {@code c_i = f(e_i) - c}, {@code c_ij = f(e_i+e_j) - c_i - c_j - c}. Escalating to a
 * higher {@code toDegree} reuses previously computed lower-degree coefficients rather than
 * resampling them.
 */
public final class PolySampler {

    private PolySampler() {}

    @FunctionalInterface
    public interface Evaluable {
        double eval(int[] x) throws Exception;
    }

    public static final class Result {
        /** Exact pseudo-Boolean polynomial coefficients for every sampled degree, keyed by term. */
        public final Map<VarSet, Double> coeffs;
        /** One record per sampled point, in sampling order. */
        public final List<SampleRecord> samples;

        Result(Map<VarSet, Double> coeffs, List<SampleRecord> samples) {
            this.coeffs = coeffs;
            this.samples = samples;
        }
    }

    /**
     * @param n              number of binary decision variables
     * @param fromDegree     lowest degree to sample this call (0 for a fresh derivation)
     * @param toDegree       highest degree to sample this call (inclusive)
     * @param existingCoeffs coefficients already known for all degrees below {@code fromDegree};
     *                       carried into the result unchanged and used for inclusion-exclusion subtraction
     * @param samplePrefix   {@code "cost"} or {@code "pen"} — matches historical {@link SampleRecord#phase} naming
     * @param f              the black-box function to sample (cost or penalty, no OCL access needed here)
     */
    public static Result sample(int n, int fromDegree, int toDegree, Map<VarSet, Double> existingCoeffs,
            String samplePrefix, Evaluable f, Consumer<String> progress) throws Exception {
        Map<VarSet, Double> coeffs = new LinkedHashMap<>(existingCoeffs);
        List<SampleRecord> samples = new ArrayList<>();

        for (int m = fromDegree; m <= toDegree; m++) {
            List<VarSet> terms = VarSet.combinations(n, m);
            int count = 0;
            for (VarSet J : terms) {
                count++;
                report(progress, "Sampling: " + samplePrefix + " degree " + m
                        + " (" + count + "/" + terms.size() + ")...");
                int[] x = J.toVector(n);
                double raw = f.eval(x);

                double sub = 0.0;
                for (VarSet I : J.properSubsets()) {
                    sub += coeffs.getOrDefault(I, 0.0);
                }
                double c = raw - sub;
                coeffs.put(J, c);
                samples.add(toSampleRecord(x, J, samplePrefix, raw));
            }
        }
        return new Result(coeffs, samples);
    }

    private static SampleRecord toSampleRecord(int[] x, VarSet J, String prefix, double rawValue) {
        int[] vars = J.vars();
        int derivedI;
        int derivedJ;
        String phase;
        if (vars.length == 0) {
            derivedI = -1;
            derivedJ = -1;
            phase = prefix + "_const";
        } else if (vars.length == 1) {
            derivedI = vars[0];
            derivedJ = vars[0];
            phase = prefix + "_lin_i=" + vars[0];
        } else if (vars.length == 2) {
            derivedI = vars[0];
            derivedJ = vars[1];
            phase = prefix + "_quad_i=" + vars[0] + "_j=" + vars[1];
        } else {
            derivedI = -2;
            derivedJ = -2;
            StringBuilder sb = new StringBuilder(prefix).append("_deg").append(vars.length);
            for (int v : vars) sb.append("_").append(v);
            phase = sb.toString();
        }
        return new SampleRecord(x, phase, rawValue, derivedI, derivedJ, vars);
    }

    private static void report(Consumer<String> cb, String msg) {
        if (cb != null) cb.accept(msg);
    }
}
