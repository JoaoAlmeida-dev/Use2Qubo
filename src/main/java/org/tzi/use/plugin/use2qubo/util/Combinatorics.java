package org.tzi.use.plugin.use2qubo.util;

/** Small combinatorics helpers shared across the plugin. */
public final class Combinatorics {

    private Combinatorics() {}

    /** n choose k. Returns 0 for out-of-range k (negative or &gt; n). */
    public static int binomial(int n, int k) {
        if (k < 0 || k > n) return 0;
        long result = 1;
        for (int i = 0; i < k; i++) {
            result = result * (n - i) / (i + 1);
        }
        return (int) result;
    }
}
