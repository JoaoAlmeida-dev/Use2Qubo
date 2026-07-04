package org.tzi.use.plugin.use2qubo.qubo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Immutable sorted set of decision-variable indices; the map key for a pseudo-Boolean
 * polynomial term of any degree (empty = constant term, size 1 = linear, size 2 = quadratic,
 * size 3+ = a higher-order term requiring quadratization before it can enter a QUBO).
 */
public final class VarSet {

    public static final VarSet EMPTY = new VarSet(new int[0]);

    private final int[] vars;

    private VarSet(int[] sortedDistinctVars) {
        this.vars = sortedDistinctVars;
    }

    public static VarSet of(int... vars) {
        int[] copy = vars.clone();
        Arrays.sort(copy);
        return new VarSet(copy);
    }

    public int degree() {
        return vars.length;
    }

    public int[] vars() {
        return vars.clone();
    }

    /** All subsets of this set, including itself and the empty set, as {@code VarSet}s. */
    public List<VarSet> subsets() {
        int m = vars.length;
        List<VarSet> result = new ArrayList<>(1 << m);
        for (int mask = 0; mask < (1 << m); mask++) {
            int[] sub = new int[Integer.bitCount(mask)];
            int idx = 0;
            for (int bit = 0; bit < m; bit++) {
                if ((mask & (1 << bit)) != 0) sub[idx++] = vars[bit];
            }
            result.add(new VarSet(sub));
        }
        return result;
    }

    /** All proper subsets of this set (everything except itself), including the empty set. */
    public List<VarSet> properSubsets() {
        List<VarSet> all = subsets();
        all.remove(all.size() - 1); // full set is always generated last by the mask loop above
        return all;
    }

    /** Builds the binary vector x^J of length n with x_i = 1 for i in this set, 0 elsewhere. */
    public int[] toVector(int n) {
        int[] x = new int[n];
        for (int v : vars) x[v] = 1;
        return x;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VarSet)) return false;
        return Arrays.equals(vars, ((VarSet) o).vars);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(vars);
    }

    @Override
    public String toString() {
        return Arrays.toString(vars);
    }

    /** Enumerates all {@code m}-subsets of {@code {0, ..., n-1}} in lexicographic order. */
    public static List<VarSet> combinations(int n, int m) {
        List<VarSet> result = new ArrayList<>();
        if (m == 0) {
            result.add(EMPTY);
            return result;
        }
        if (m > n) return result;
        int[] combo = new int[m];
        for (int i = 0; i < m; i++) combo[i] = i;
        while (true) {
            result.add(VarSet.of(combo.clone()));
            int i = m - 1;
            while (i >= 0 && combo[i] == n - m + i) i--;
            if (i < 0) break;
            combo[i]++;
            for (int j = i + 1; j < m; j++) combo[j] = combo[j - 1] + 1;
        }
        return result;
    }
}
