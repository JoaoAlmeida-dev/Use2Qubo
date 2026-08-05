package org.tzi.use.plugin.use2qubo.qubo.engine.exactness;

import org.tzi.use.plugin.use2qubo.qubo.context.QuboContext;
import org.tzi.use.plugin.use2qubo.qubo.engine.ProgressEvent;
import org.tzi.use.plugin.use2qubo.qubo.engine.eval.DecisionLinkSampler;
import org.tzi.use.plugin.use2qubo.qubo.engine.eval.ObjectiveEvaluator;
import org.tzi.use.plugin.use2qubo.qubo.engine.eval.PenaltyEvaluator;
import org.tzi.use.plugin.use2qubo.qubo.engine.index.DVPair;
import org.tzi.use.plugin.use2qubo.qubo.engine.sampling.PolyMath;
import org.tzi.use.plugin.use2qubo.qubo.engine.sampling.VarSet;
import org.tzi.use.plugin.use2qubo.qubo.result.ExactnessPoint;
import org.tzi.use.plugin.use2qubo.util.PluginLog;
import org.tzi.use.plugin.use2qubo.util.QuboConstants;
import org.tzi.use.uml.ocl.expr.Evaluator;
import org.tzi.use.uml.ocl.expr.Expression;
import org.tzi.use.uml.sys.MLink;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Evaluates the derived polynomial q(x) against the true f(x) to certify exactness.
 * When {@code n <= QuboConstants.EXACTNESS_EXHAUSTIVE_MAX_N}, every one of the {@code 2^n}
 * binary vectors is checked, a proof, not a spot-check. Otherwise falls back to
 * {@link QuboConstants#EXACTNESS_SAMPLE_COUNT} random held-out vectors (Hamming weight
 * &ge; {@link QuboConstants#EXACTNESS_MIN_HAMMING_WEIGHT}), which is necessary but not
 * sufficient: a pass only means no mismatch was found among the points actually checked.
 */
public final class ExactnessChecker {

    private static final double EPS = QuboConstants.EPS;

    private ExactnessChecker() {}

    public static boolean logExactnessOutcome(ExactnessOutcome outcome, int degree) {
        if (!outcome.exact) {
            PluginLog.warn("QuboEngine: exactness check FAILED at degree " + degree
                + " (" + outcome.method + ", " + outcome.matchCount + "/" + outcome.totalCount + " matched)"
                + " — q(x) ≠ f(x). "
                + "Common causes: "
                + "(1) An OCL invariant uses a boolean pass/fail condition — "
                + "reformulate by counting violations (integer sum) instead of returning true/false. "
                + "(2) The objective/penalty involves interactions between more decision variables "
                + "than the current degree cap allows — raise max_degree.");
        } else {
            PluginLog.info("Exactness check: PASS at degree " + degree
                + " (" + outcome.method + ", " + outcome.matchCount + "/" + outcome.totalCount + " matched)");
        }
        return outcome.exact;
    }

    public static ExactnessOutcome checkExactness(int n, Map<VarSet, Double> combined,
            List<DVPair> flatVars, QuboContext ctx, Evaluator evaluator, List<PenaltyEvaluator.PenaltyTask> penaltyTasks,
            Expression objExpr, double B,
            Map<String, Set<MLink>> savedLinks, Consumer<String> progress) throws Exception {
        if (n <= QuboConstants.EXACTNESS_EXHAUSTIVE_MAX_N) {
            return checkExactnessExhaustive(n, combined, flatVars, ctx, evaluator, penaltyTasks, objExpr, B, savedLinks, progress);
        }
        return checkExactnessSampled(n, combined, flatVars, ctx, evaluator, penaltyTasks, objExpr, B, savedLinks);
    }

    /** Exhaustive branch: enumerates every {@code 2^n} vector, proving exactness rather than sampling it. */
    public static ExactnessOutcome checkExactnessExhaustive(int n, Map<VarSet, Double> combined,
            List<DVPair> flatVars, QuboContext ctx, Evaluator evaluator, List<PenaltyEvaluator.PenaltyTask> penaltyTasks,
            Expression objExpr, double B,
            Map<String, Set<MLink>> savedLinks, Consumer<String> progress) throws Exception {
        DecisionLinkSampler.stripDecisionLinks(ctx);

        long total = 1L << n;
        long reportEvery = Math.max(1L, total / 50L);
        int matchCount = 0;
        int evalFailedCount = 0;
        List<ExactnessPoint> mismatches = new ArrayList<>();
        List<ExactnessPoint> matchesSample = new ArrayList<>();
        try {
            for (long i = 0; i < total; i++) {
                if (i % reportEvery == 0) {
                    ProgressEvent.report(progress, "Exhaustive exactness check: " + i + "/" + total + "…");
                }
                int[] x = new int[n];
                for (int b = 0; b < n; b++) x[b] = (int) ((i >> b) & 1);
                double qx = PolyMath.evalPoly(combined, x);
                try {
                    double fx = ObjectiveEvaluator.evalCost(x, flatVars, ctx, evaluator, objExpr)
                              + B * PenaltyEvaluator.evalPenalty(x, flatVars, ctx, penaltyTasks);
                    if (Math.abs(qx - fx) < EPS) {
                        matchCount++;
                        if (matchesSample.size() < QuboConstants.EXACTNESS_SAMPLE_COUNT) {
                            matchesSample.add(new ExactnessPoint(x, fx, qx));
                        }
                    } else if (mismatches.size() < QuboConstants.EXACTNESS_SAMPLE_COUNT) {
                        mismatches.add(new ExactnessPoint(x, fx, qx));
                    }
                } catch (Exception e) {
                    evalFailedCount++;
                    if (mismatches.size() < QuboConstants.EXACTNESS_SAMPLE_COUNT) {
                        mismatches.add(new ExactnessPoint(x));
                    }
                }
            }
        } finally {
            DecisionLinkSampler.restoreLinks(ctx, savedLinks);
        }

        int totalCount = (int) total - evalFailedCount;
        boolean exact = evalFailedCount == 0 && matchCount == totalCount && totalCount > 0;
        List<ExactnessPoint> points = !mismatches.isEmpty() ? mismatches : matchesSample;
        PluginLog.info("Exactness check (exhaustive): " + matchCount + "/" + totalCount
                + " matched over all " + total + " vectors (n=" + n + ")"
                + (evalFailedCount > 0 ? ", " + evalFailedCount + " eval failures" : ""));
        return new ExactnessOutcome(Collections.unmodifiableList(points), "exhaustive", matchCount, totalCount, exact);
    }

    /**
     * Sampled branch (used when {@code n} exceeds the exhaustive threshold): evaluates q(x) against
     * the true f(x) on up to {@link QuboConstants#EXACTNESS_SAMPLE_COUNT} random held-out binary
     * vectors and returns all evaluation points for diagnostic display. Does not short-circuit on
     * mismatch, all points are collected so the full error table is available to the user.
     *
     * <p>For small n where fewer than {@link QuboConstants#EXACTNESS_SAMPLE_COUNT}
     * distinct qualifying vectors exist, the method stops after the retry budget
     * is exhausted and returns however many points were collected.
     */
    public static ExactnessOutcome checkExactnessSampled(int n, Map<VarSet, Double> combined,
            List<DVPair> flatVars, QuboContext ctx, Evaluator evaluator, List<PenaltyEvaluator.PenaltyTask> penaltyTasks,
            Expression objExpr, double B,
            Map<String, Set<MLink>> savedLinks) {
        DecisionLinkSampler.stripDecisionLinks(ctx);

        List<ExactnessPoint> points = new ArrayList<>(QuboConstants.EXACTNESS_SAMPLE_COUNT);
        try {
            Random rand = new Random(QuboConstants.EXACTNESS_SEED);
            int attempts = 0;
            // Cap retries to avoid infinite loop for small n where few qualifying vectors exist.
            int maxAttempts = Math.max(QuboConstants.EXACTNESS_MIN_ATTEMPTS,
                    n * n * QuboConstants.EXACTNESS_ATTEMPTS_PER_N_SQUARED);
            int k = 0;
            while (k < QuboConstants.EXACTNESS_SAMPLE_COUNT && attempts < maxAttempts) {
                attempts++;
                int[] x = new int[n];
                for (int i = 0; i < n; i++) x[i] = rand.nextInt(2);
                // skip vectors below the minimum Hamming weight, they are training samples, exact by construction
                int ones = 0;
                for (int v : x) ones += v;
                if (ones < QuboConstants.EXACTNESS_MIN_HAMMING_WEIGHT) continue;

                k++;
                double qx = PolyMath.evalPoly(combined, x);

                try {
                    double fx = ObjectiveEvaluator.evalCost(x, flatVars, ctx, evaluator, objExpr)
                              + B * PenaltyEvaluator.evalPenalty(x, flatVars, ctx, penaltyTasks);
                    points.add(new ExactnessPoint(x, fx, qx));
                } catch (Exception e) {
                    PluginLog.warn("Exactness check: eval failed on held-out vector", e);
                    points.add(new ExactnessPoint(x));
                }
            }
            if (attempts >= maxAttempts && k < QuboConstants.EXACTNESS_SAMPLE_COUNT) {
                PluginLog.info("Exactness check: only " + k
                        + " qualifying vectors found in " + attempts + " attempts (n=" + n + ")");
            }
        } finally {
            DecisionLinkSampler.restoreLinks(ctx, savedLinks);
        }

        int evalFailedCount = 0;
        int matchCount = 0;
        for (ExactnessPoint p : points) {
            if (p.evalFailed) evalFailedCount++;
            else if (p.error() < EPS) matchCount++;
        }
        int totalCount = points.size() - evalFailedCount;
        boolean exact = points.stream().noneMatch(p -> p.evalFailed || p.error() >= EPS);
        return new ExactnessOutcome(Collections.unmodifiableList(points), "sampled", matchCount, totalCount, exact);
    }
}
