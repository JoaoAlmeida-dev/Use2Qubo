package org.tzi.use.plugin.use2qubo.qubo.engine.exactness;

import org.tzi.use.plugin.use2qubo.qubo.result.ExactnessPoint;

import java.util.List;

/**
 * Outcome of an exactness check: whether q(x) matches the true f(x), how it was checked
 * (exhaustive proof over every {@code 2^n} vector, or a random held-out sample), how many of
 * the successfully-evaluated points matched, and a (possibly truncated, for display) subset
 * of the actual points for the UI/export diagnostics table.
 */
public final class ExactnessOutcome {
    public final List<ExactnessPoint> points;
    public final String method;
    public final int matchCount;
    public final int totalCount;
    public final boolean exact;

    public ExactnessOutcome(List<ExactnessPoint> points, String method, int matchCount, int totalCount, boolean exact) {
        this.points = points;
        this.method = method;
        this.matchCount = matchCount;
        this.totalCount = totalCount;
        this.exact = exact;
    }
}
