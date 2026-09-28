package org.tzi.use.plugin.use2qubo.qubo.engine;

import org.tzi.use.plugin.use2qubo.qubo.context.QuboContext;
import org.tzi.use.plugin.use2qubo.qubo.engine.eval.DecisionLinkSampler;
import org.tzi.use.plugin.use2qubo.qubo.engine.eval.ObjectiveEvaluator;
import org.tzi.use.plugin.use2qubo.qubo.engine.eval.PenaltyEvaluator;
import org.tzi.use.plugin.use2qubo.qubo.engine.exactness.ExactnessChecker;
import org.tzi.use.plugin.use2qubo.qubo.engine.exactness.ExactnessOutcome;
import org.tzi.use.plugin.use2qubo.qubo.engine.index.DVPair;
import org.tzi.use.plugin.use2qubo.qubo.engine.index.VarIndexBuilder;
import org.tzi.use.plugin.use2qubo.qubo.engine.sampling.PolyMath;
import org.tzi.use.plugin.use2qubo.qubo.engine.sampling.PolySampler;
import org.tzi.use.plugin.use2qubo.qubo.engine.sampling.SandboxWorkerPool;
import org.tzi.use.plugin.use2qubo.qubo.engine.sampling.VarSet;
import org.tzi.use.plugin.use2qubo.qubo.result.QuboResult;
import org.tzi.use.plugin.use2qubo.qubo.result.SampleRecord;
import org.tzi.use.plugin.use2qubo.util.Combinatorics;
import org.tzi.use.plugin.use2qubo.util.PluginLog;
import org.tzi.use.uml.ocl.expr.Evaluator;
import org.tzi.use.uml.ocl.expr.Expression;
import org.tzi.use.uml.sys.MLink;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Derives QUBO Q-matrix coefficients from a QuboContext using the
 * AutoQUBO data-driven sampling algorithm (Moraglio et al., GECCO '22 §4)
 * with the Verma-Lewis penalty weight (Pauckert et al., GECCO '23 §2.1).
 *
 * <p>Two-pass sampling: cost and penalty evaluated separately so B can be
 * derived from cost coefficients alone (per-row max; tighter than the global
 * sum bound, giving a better annealing landscape).
 *
 * <p><b>Higher-order terms.</b> Sampling starts at degree 2 (the historical
 * behaviour). If the combined cost+penalty polynomial fails the exactness
 * check at degree 2 — which happens whenever an OCL invariant or objective
 * term touches 3+ decision variables in one expression (e.g. a boolean
 * pass/fail invariant over a sum of many decision variables, or an
 * {@code exists}/{@code or} across several) — sampling escalates to degree 3,
 * degree 4, etc. (AutoQUBO §4.3, Algorithm 1), up to {@link QuboContext#maxDegree}.
 * Once an exact higher-degree polynomial is found, it is reduced to a QUBO by
 * Rosenberg pair-substitution quadratization ({@link Quadratizer}; Rosenberg
 * 1975, surveyed in Dattani 2019, arXiv:1901.04405), introducing ancillary
 * binary variables appended after the original decision variables. If no
 * degree up to the cap is exact, the best-effort degree-2 approximation is
 * exported with {@code exact=false}, as before.
 *
 * <p>The actual sampling/evaluation/verification work is delegated to focused
 * collaborator classes in this package: {@link VarIndexBuilder} (decision-variable
 * indexing), {@link DecisionLinkSampler} (temporary link insert/strip/restore),
 * {@link ObjectiveEvaluator}/{@link PenaltyEvaluator} (OCL evaluation), {@link PolyMath}
 * (pseudo-Boolean polynomial math), {@link ExactnessChecker} (q(x) vs f(x) certification)
 * and {@link ResultAssembler} (quadratization + final {@link QuboResult} assembly). This
 * class only orchestrates the sequence: sample, compute B, check exactness, escalate
 * degree if needed, assemble result.
 */
public class QuboEngine {

    /** Asked before sampling escalates to a higher degree, so a caller can let the user
     *  decide whether the extra sample cost is worth paying. */
    @FunctionalInterface
    public interface EscalationConfirm {
        /** Called before sampling toDegree; expectedSamples counts both cost+penalty passes. */
        boolean proceed(int fromDegree, int toDegree, long expectedSamples);
    }

    private static final EscalationConfirm ALWAYS_PROCEED = (from, to, expected) -> true;

    /**
     * Optional tuning knobs for {@link #derive}, all defaulted via {@link #defaults()}. Immutable;
     * each {@code withX} returns a new instance rather than mutating in place.
     */
    public static final class DeriveOptions {
        /** Human-readable step labels; may be {@code null}. Called from the calling thread. */
        public final Consumer<String> progress;
        /** {@link ProgressEvent}s for phase transitions and per-sample counts; may be {@code null}.
         *  Called from the calling thread. */
        public final Consumer<ProgressEvent> structuredProgress;
        /** Asked before escalating to a higher degree; declining stops escalation. */
        public final EscalationConfirm confirm;
        /** Whether to retain per-point {@link SampleRecord}s for the result's
         *  {@code costSamples}/{@code penaltySamples} (needed by the GUI's Sampling tab).
         *  {@code false} for headless/CLI derivation, where nothing reads them and retaining one
         *  record per sampled combination is the dominant memory cost at large n. */
        public final boolean collectSamples;
        /** Explicit {@code SandboxWorkerPool} worker count, or {@code null} to fall back to the
         *  pool's own {@code min(availableProcessors(), MAX_SAMPLE_WORKERS)} sizing. An explicit
         *  value bypasses that cap — the caller (e.g. the CLI's {@code --workers} flag) asked for
         *  it directly. */
        public final Integer workerOverride;

        private DeriveOptions(Consumer<String> progress, Consumer<ProgressEvent> structuredProgress,
                               EscalationConfirm confirm, boolean collectSamples, Integer workerOverride) {
            this.progress = progress;
            this.structuredProgress = structuredProgress;
            this.confirm = confirm;
            this.collectSamples = collectSamples;
            this.workerOverride = workerOverride;
        }

        /** {@code progress=null, structuredProgress=null, confirm=ALWAYS_PROCEED,
         *  collectSamples=true, workerOverride=null}. */
        public static DeriveOptions defaults() {
            return new DeriveOptions(null, null, ALWAYS_PROCEED, true, null);
        }

        public DeriveOptions withProgress(Consumer<String> progress) {
            return new DeriveOptions(progress, structuredProgress, confirm, collectSamples, workerOverride);
        }

        public DeriveOptions withStructuredProgress(Consumer<ProgressEvent> structuredProgress) {
            return new DeriveOptions(progress, structuredProgress, confirm, collectSamples, workerOverride);
        }

        public DeriveOptions withConfirm(EscalationConfirm confirm) {
            return new DeriveOptions(progress, structuredProgress, confirm, collectSamples, workerOverride);
        }

        public DeriveOptions withCollectSamples(boolean collectSamples) {
            return new DeriveOptions(progress, structuredProgress, confirm, collectSamples, workerOverride);
        }

        public DeriveOptions withWorkerOverride(Integer workerOverride) {
            return new DeriveOptions(progress, structuredProgress, confirm, collectSamples, workerOverride);
        }
    }

    /** Result of {@link #evaluateTrue}: the true OCL objective/penalty for one binary vector,
     *  as opposed to the derived QUBO polynomial's approximation of the same quantities. */
    public static final class TrueEval {
        /** Objective value, sign already flipped per {@code ctx.minimise}. */
        public final double cost;
        /** Raw violated-invariant count (unweighted). */
        public final double penalty;

        public TrueEval(double cost, double penalty) {
            this.cost = cost;
            this.penalty = penalty;
        }

        public double weighted(double B) {
            return cost + B * penalty;
        }
    }

    /**
     * Evaluates the true OCL objective and penalty at a user-chosen binary vector over the
     * original decision variables (no ancillas), for the "Try It" tab's q(x)-vs-f(x) comparison.
     * Strips any existing decision-var links, inserts exactly those implied by {@code x}, evaluates,
     * then restores the original links, unwinding via the same sequence used during derivation.
     *
     * @param ctx context whose {@code state} is already the derive-time sandbox (JAVA-015 isolation)
     * @param x   binary vector, length must equal {@code ctx.nVars}
     */
    public static TrueEval evaluateTrue(QuboContext ctx, int[] x) throws Exception {
        if (x.length != ctx.nVars) {
            throw new IllegalArgumentException(
                    "Vector length " + x.length + " != ctx.nVars " + ctx.nVars);
        }
        List<DVPair> flatVars = VarIndexBuilder.buildFlatVars(ctx);
        Expression objExpr = ObjectiveEvaluator.compileObjective(ctx, null);
        Evaluator evaluator = new Evaluator();
        List<PenaltyEvaluator.PenaltyTask> penaltyTasks = PenaltyEvaluator.buildPenaltyTasks(ctx);

        Map<String, Set<MLink>> savedLinks = DecisionLinkSampler.saveAndStripLinks(ctx);
        try {
            double cost = ObjectiveEvaluator.evalCost(x, flatVars, ctx, evaluator, objExpr);
            double penalty = PenaltyEvaluator.evalPenalty(x, flatVars, ctx, penaltyTasks);
            return new TrueEval(cost, penalty);
        } finally {
            DecisionLinkSampler.restoreLinks(ctx, savedLinks);
        }
    }

    /**
     * Derives the QUBO Q-matrix from the given context.
     *
     * @param ctx     QUBO derivation context (model, state, config)
     * @param options tuning knobs; see {@link DeriveOptions#defaults()} for defaults
     */
    public static QuboResult derive(QuboContext ctx, DeriveOptions options) throws Exception {
        int n = ctx.nVars;
        PluginLog.info("QuboEngine.derive: nVars=" + n + ", maxDegree=" + ctx.maxDegree);

        ProgressEvent.reportPhase(options.progress, options.structuredProgress, "Building variable index…");
        List<DVPair> flatVars = VarIndexBuilder.buildFlatVars(ctx);
        if (flatVars.size() != n) {
            throw new IllegalStateException(
                    "Flat var count " + flatVars.size() + " != nVars " + n);
        }
        List<String> varLabels = VarIndexBuilder.buildVarLabels(flatVars);
        Expression objExpr = ObjectiveEvaluator.compileObjective(ctx, options.progress, options.structuredProgress);
        Evaluator evaluator = new Evaluator();
        List<PenaltyEvaluator.PenaltyTask> penaltyTasks = PenaltyEvaluator.buildPenaltyTasks(ctx);

        Map<String, Set<MLink>> savedLinks = DecisionLinkSampler.saveAndStripLinks(ctx);
        try {
            QuboResult result = deriveWithClearedState(ctx, n, flatVars, varLabels,
                    objExpr, evaluator, penaltyTasks, savedLinks, options);
            PluginLog.info("Derive complete: " + result);
            return result;
        } finally {
            DecisionLinkSampler.restoreLinks(ctx, savedLinks);
        }
    }

    /** Runs the sampling (with degree escalation), quadratization and exactness-check steps,
     *  assuming decision-var links are already stripped. */
    private static QuboResult deriveWithClearedState(QuboContext ctx, int n,
            List<DVPair> flatVars, List<String> varLabels, Expression objExpr, Evaluator evaluator,
            List<PenaltyEvaluator.PenaltyTask> penaltyTasks,
            Map<String, Set<MLink>> savedLinks, DeriveOptions options) throws Exception {

        Consumer<String> progress = options.progress;
        Consumer<ProgressEvent> structuredProgress = options.structuredProgress;
        EscalationConfirm confirm = options.confirm;
        boolean collectSamples = options.collectSamples;
        Integer workerOverride = options.workerOverride;

        int maxDegree = Math.max(2, ctx.maxDegree);

        // One pool, one sandbox clone per worker, reused for both passes and every escalation
        // degree — none of them mutate anything a sandbox depends on (attributes/fixed links are
        // read-only during derivation; decision links are always local to each sampled point).
        try (SandboxWorkerPool pool = SandboxWorkerPool.build(ctx, flatVars, objExpr, workerOverride)) {
            ProgressEvent.reportPhase(progress, structuredProgress, "Sampling: cost degree ≤2…");
            PolySampler.Result cost = PolySampler.sample(n, 0, 2, Collections.emptyMap(), "cost",
                    pool, SandboxWorkerPool.EvalKind.COST, progress, structuredProgress, collectSamples);
            double B = PolyMath.computePenaltyWeight(n, cost.coeffs);

            ProgressEvent.reportPhase(progress, structuredProgress, "Sampling: penalty degree ≤2…");
            PolySampler.Result penalty = PolySampler.sample(n, 0, 2, Collections.emptyMap(), "pen",
                    pool, SandboxWorkerPool.EvalKind.PENALTY, progress, structuredProgress, collectSamples);

            Map<VarSet, Double> combined = PolyMath.combine(cost.coeffs, penalty.coeffs, B);
            int degree = 2;

            DecisionLinkSampler.restoreLinks(ctx, savedLinks);
            ProgressEvent.reportPhase(progress, structuredProgress, "Running exactness check (degree " + degree + ")…");
            ExactnessOutcome exactnessOutcome = ExactnessChecker.checkExactness(n, combined, flatVars, ctx, evaluator, penaltyTasks, objExpr, B, savedLinks, progress, pool);
            boolean degreeExact = ExactnessChecker.logExactnessOutcome(exactnessOutcome, degree);

            while (!degreeExact && degree < maxDegree) {
                int nextDegree = degree + 1;
                long expectedSamples = 2L * Combinatorics.binomial(n, nextDegree);
                if (!confirm.proceed(degree, nextDegree, expectedSamples)) {
                    PluginLog.info("QuboEngine: user declined escalation to degree " + nextDegree
                            + "; stopping at degree " + degree);
                    break;
                }

                ProgressEvent.reportPhase(progress, structuredProgress,
                        "Exactness failed at degree " + degree + "; escalating to degree " + nextDegree + "…");
                PluginLog.info("QuboEngine: escalating sampling to degree " + nextDegree);

                PolySampler.Result costNext = PolySampler.sample(n, nextDegree, nextDegree, cost.coeffs, "cost",
                        pool, SandboxWorkerPool.EvalKind.COST, progress, structuredProgress, collectSamples);
                List<SampleRecord> costSamples;
                if (collectSamples) {
                    costSamples = new ArrayList<>(cost.samples);
                    costSamples.addAll(costNext.samples);
                } else {
                    costSamples = Collections.emptyList();
                }
                cost = new PolySampler.Result(costNext.coeffs, costSamples);

                PolySampler.Result penaltyNext = PolySampler.sample(n, nextDegree, nextDegree, penalty.coeffs, "pen",
                        pool, SandboxWorkerPool.EvalKind.PENALTY, progress, structuredProgress, collectSamples);
                List<SampleRecord> penaltySamples;
                if (collectSamples) {
                    penaltySamples = new ArrayList<>(penalty.samples);
                    penaltySamples.addAll(penaltyNext.samples);
                } else {
                    penaltySamples = Collections.emptyList();
                }
                penalty = new PolySampler.Result(penaltyNext.coeffs, penaltySamples);

                combined = PolyMath.combine(cost.coeffs, penalty.coeffs, B);
                degree = nextDegree;

                DecisionLinkSampler.restoreLinks(ctx, savedLinks);
                ProgressEvent.reportPhase(progress, structuredProgress, "Running exactness check (degree " + degree + ")…");
                exactnessOutcome = ExactnessChecker.checkExactness(n, combined, flatVars, ctx, evaluator, penaltyTasks, objExpr, B, savedLinks, progress, pool);
                degreeExact = ExactnessChecker.logExactnessOutcome(exactnessOutcome, degree);
            }

            int nSamples = cost.samples.size() + penalty.samples.size();
            return ResultAssembler.buildResult(n, nSamples, combined, varLabels, B,
                    cost.samples, penalty.samples, exactnessOutcome, degreeExact, degree);
        }
    }
}
