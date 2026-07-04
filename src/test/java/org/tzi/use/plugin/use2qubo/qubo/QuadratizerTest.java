package org.tzi.use.plugin.use2qubo.qubo;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuadratizerTest {

    private static double evalPoly(Map<VarSet, Double> coeffs, int[] x) {
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

    private static double evalQuadratic(Quadratizer.Result r, int[] x) {
        double[] full = new double[x.length + r.nAncilla];
        for (int i = 0; i < x.length; i++) full[i] = x[i];
        for (int k = 0; k < r.nAncilla; k++) {
            int[] pair = r.ancillaPairs.get(k);
            full[x.length + k] = full[pair[0]] * full[pair[1]];
        }
        double result = r.constant;
        for (int i = 0; i < r.lin.length; i++) result += r.lin[i] * full[i];
        for (int i = 0; i < r.quad.length; i++) {
            for (int j = i + 1; j < r.quad.length; j++) {
                result += r.quad[i][j] * full[i] * full[j];
            }
        }
        return result;
    }

    /** For every binary vector, the quadratized form (with ancillas set to their exact product)
     *  must reproduce the original pseudo-Boolean polynomial. */
    private static void assertReproducesOnAllVectors(Map<VarSet, Double> coeffs, int n, List<String> labels) {
        Quadratizer.Result reduced = Quadratizer.reduce(n, coeffs, labels);
        for (int mask = 0; mask < (1 << n); mask++) {
            int[] x = new int[n];
            for (int i = 0; i < n; i++) x[i] = (mask >> i) & 1;
            double expected = evalPoly(coeffs, x);
            double actual = evalQuadratic(reduced, x);
            assertEquals(expected, actual, 1e-9,
                    "mismatch at x=" + java.util.Arrays.toString(x));
        }
    }

    @Test
    void degreeTwoPolynomialNeedsNoAncillas() {
        Map<VarSet, Double> coeffs = new HashMap<>();
        coeffs.put(VarSet.EMPTY, 5.0);
        coeffs.put(VarSet.of(0), 2.0);
        coeffs.put(VarSet.of(1), -1.0);
        coeffs.put(VarSet.of(0, 1), 3.0);

        Quadratizer.Result r = Quadratizer.reduce(2, coeffs, List.of("x0", "x1"));
        assertEquals(0, r.nAncilla);
        assertReproducesOnAllVectors(coeffs, 2, List.of("x0", "x1"));
    }

    @Test
    void cubicTermIsReducedWithOneAncilla() {
        Map<VarSet, Double> coeffs = new HashMap<>();
        coeffs.put(VarSet.of(0, 1, 2), 3.0);

        List<String> labels = List.of("x0", "x1", "x2");
        Quadratizer.Result r = Quadratizer.reduce(3, coeffs, labels);
        assertEquals(1, r.nAncilla);
        assertReproducesOnAllVectors(coeffs, 3, labels);
    }

    @Test
    void mixedDegreeCostPlusPenaltyIsReducedExactly() {
        Map<VarSet, Double> coeffs = new HashMap<>();
        coeffs.put(VarSet.EMPTY, 1.0);
        coeffs.put(VarSet.of(0), 2.0);
        coeffs.put(VarSet.of(1), -3.0);
        coeffs.put(VarSet.of(2), 0.5);
        coeffs.put(VarSet.of(0, 1), 4.0);
        coeffs.put(VarSet.of(1, 2), -2.0);
        coeffs.put(VarSet.of(0, 1, 2), -5.0);

        List<String> labels = List.of("x0", "x1", "x2");
        assertReproducesOnAllVectors(coeffs, 3, labels);
    }

    @Test
    void degreeFourTermReducesWithTwoChainedAncillas() {
        Map<VarSet, Double> coeffs = new HashMap<>();
        coeffs.put(VarSet.of(0, 1, 2, 3), 2.0);

        List<String> labels = List.of("x0", "x1", "x2", "x3");
        Quadratizer.Result r = Quadratizer.reduce(4, coeffs, labels);
        assertEquals(2, r.nAncilla);
        assertReproducesOnAllVectors(coeffs, 4, labels);
    }

    @Test
    void repeatedPairAcrossTermsReusesTheSameAncilla() {
        Map<VarSet, Double> coeffs = new HashMap<>();
        coeffs.put(VarSet.of(0, 1, 2), 1.0);
        coeffs.put(VarSet.of(0, 1, 3), -2.0);

        List<String> labels = List.of("x0", "x1", "x2", "x3");
        Quadratizer.Result r = Quadratizer.reduce(4, coeffs, labels);
        // Both terms share the (x0,x1) prefix pair, so only one ancilla should be created.
        assertEquals(1, r.nAncilla);
        assertReproducesOnAllVectors(coeffs, 4, labels);
    }

    @Test
    void varSetCombinationsMatchLexicographicPairwiseOrder() {
        List<VarSet> combos = VarSet.combinations(4, 2);
        List<String> asStrings = new ArrayList<>();
        for (VarSet vs : combos) asStrings.add(vs.toString());
        assertEquals(6, combos.size());
        assertTrue(asStrings.contains("[0, 1]"));
        assertTrue(asStrings.contains("[2, 3]"));
    }
}
