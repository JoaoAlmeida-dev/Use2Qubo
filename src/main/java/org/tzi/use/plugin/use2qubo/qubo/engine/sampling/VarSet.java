package org.tzi.use.plugin.use2qubo.qubo.engine.sampling;

import org.tzi.use.plugin.use2qubo.util.Combinatorics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

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

    /** Enumerates all {@code m}-subsets of {@code {0, ..., n-1}} in lexicographic order, lazily:
     *  one {@code int[m]} state array is reused across the walk instead of materialising all
     *  {@code C(n,m)} combinations up front (that count is astronomical for large n/m). */
    public static Iterable<VarSet> combinations(int n, int m) {
        if (m < 0 || m > n) return Collections.emptyList();
        if (m == 0) return List.of(EMPTY);
        return () -> boundedIterator(n, m, initCombo(m), Long.MAX_VALUE);
    }

    /**
     * Enumerates exactly {@code count} {@code m}-subsets starting at lexicographic rank
     * {@code startRank} (0-indexed, same order as {@link #combinations}), lazily — same O(1)
     * per-step memory as {@link #combinations}, just seeded at an arbitrary offset instead of
     * always starting from rank 0. Lets {@code SandboxWorkerPool} hand each worker a contiguous
     * rank range to process without ever materialising the full {@code C(n,m)} combination list
     * (which, for large n, is exactly the out-of-memory failure mode the lazy {@link #combinations}
     * iterator was originally written to avoid — chunking naively via a {@code List<VarSet>} would
     * reintroduce it).
     */
    public static Iterable<VarSet> combinationsRange(int n, int m, long startRank, long count) {
        if (m < 0 || m > n || count <= 0) return Collections.emptyList();
        if (m == 0) return startRank == 0 ? List.of(EMPTY) : Collections.emptyList();
        int[] startCombo = unrank(n, m, startRank);
        return () -> boundedIterator(n, m, startCombo, count);
    }

    /**
     * The {@code m}-subset of {@code {0, ..., n-1}} at 0-indexed lexicographic rank {@code rank}
     * (same order as {@link #combinations}). Standard combinatorial unranking: at each position,
     * try the smallest not-yet-excluded candidate and skip past it (subtracting the number of
     * combinations it would account for) until the remaining rank falls within its share.
     */
    static int[] unrank(int n, int m, long rank) {
        int[] combo = new int[m];
        int x = 0;
        long remaining = rank;
        for (int i = 0; i < m; i++) {
            int c = x;
            while (true) {
                long countWithThisChoice = Combinatorics.binomial(n - c - 1, m - i - 1);
                if (remaining < countWithThisChoice) {
                    combo[i] = c;
                    x = c + 1;
                    break;
                }
                remaining -= countWithThisChoice;
                c++;
            }
        }
        return combo;
    }

    /** Shared stepping logic behind {@link #combinations} and {@link #combinationsRange}: walks
     *  forward from {@code startCombo} (which the caller owns — mutated in place), yielding at
     *  most {@code limit} terms or until lexicographic exhaustion, whichever comes first. */
    private static Iterator<VarSet> boundedIterator(int n, int m, int[] startCombo, long limit) {
        return new Iterator<VarSet>() {
            private final int[] combo = startCombo;
            private long remaining = limit;
            private boolean hasNext = remaining > 0;

            @Override
            public boolean hasNext() {
                return hasNext;
            }

            @Override
            public VarSet next() {
                if (!hasNext) throw new NoSuchElementException();
                VarSet result = VarSet.of(combo.clone());
                remaining--;
                if (remaining > 0) advance(); else hasNext = false;
                return result;
            }

            private void advance() {
                int i = m - 1;
                while (i >= 0 && combo[i] == n - m + i) i--;
                if (i < 0) {
                    hasNext = false;
                    return;
                }
                combo[i]++;
                for (int j = i + 1; j < m; j++) combo[j] = combo[j - 1] + 1;
            }
        };
    }

    private static int[] initCombo(int m) {
        int[] combo = new int[m];
        for (int i = 0; i < m; i++) combo[i] = i;
        return combo;
    }
}
