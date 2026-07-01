package org.tzi.use.plugin.use2qubo.qubo;

import org.tzi.use.parser.ocl.OCLCompiler;
import org.tzi.use.plugin.use2qubo.util.PluginLog;
import org.tzi.use.plugin.use2qubo.util.QuboConstants;
import org.tzi.use.uml.mm.MAssociation;
import org.tzi.use.uml.mm.MClassInvariant;
import org.tzi.use.uml.ocl.expr.Evaluator;
import org.tzi.use.uml.ocl.expr.Expression;
import org.tzi.use.uml.ocl.value.BooleanValue;
import org.tzi.use.uml.ocl.value.IntegerValue;
import org.tzi.use.uml.ocl.value.ObjectValue;
import org.tzi.use.uml.ocl.value.RealValue;
import org.tzi.use.uml.ocl.value.Value;
import org.tzi.use.uml.ocl.value.VarBindings;
import org.tzi.use.uml.sys.MLink;
import org.tzi.use.uml.sys.MObject;
import org.tzi.use.uml.sys.MSystemState;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
 * Total evaluations: 2 + n*(n+1) (2× v1; acceptable).
 *
 * <p><b>Boolean penalty limitation:</b> OCL invariants are evaluated as boolean
 * pass/fail and each failure contributes +1 to the penalty. This is a step-function
 * over binary vectors — not degree-2 representable if any invariant body involves a
 * sum of many decision variables (e.g. {@code self.bins->size() = k}).
 * The exactness check detects this. To guarantee exactness, either ensure each
 * invariant instance references at most two decision variables, or reformulate
 * as an integer-valued violation count (e.g. {@code (self.bins->size() - k).abs()})
 * evaluated via a custom objective expression rather than a class invariant.
 */
public class QuboEngine {

    private static final double EPS = QuboConstants.EPS;

    /**
     * Derives the QUBO Q-matrix from the given context.
     *
     * @param ctx      QUBO derivation context (model, state, config)
     * @param progress optional callback that receives human-readable step labels;
     *                 may be {@code null}. Called from the calling thread.
     */
    public static QuboResult derive(QuboContext ctx, Consumer<String> progress) throws Exception {
        int n = ctx.nVars;
        int nSamples = 2 + n * (n + 1);  // two passes of (1 + n*(n+1)/2)
        PluginLog.info("QuboEngine.derive: nVars=" + n + ", nSamples=" + nSamples);

        report(progress, "Building variable index…");
        List<DVPair> flatVars = buildFlatVars(ctx);
        if (flatVars.size() != n) {
            throw new IllegalStateException(
                    "Flat var count " + flatVars.size() + " != nVars " + n);
        }

        List<String> varLabels = new ArrayList<>(n);
        for (DVPair p : flatVars) {
            varLabels.add(p.dv.association + "(" + p.a.name() + "," + p.b.name() + ")");
        }

        report(progress, "Compiling objective OCL…");
        StringWriter errBuf = new StringWriter();
        Expression objExpr = OCLCompiler.compileExpression(
                ctx.model, ctx.objectiveExpr, "objective",
                new PrintWriter(errBuf), new VarBindings());
        if (objExpr == null) {
            throw new IllegalStateException("Cannot parse objective OCL: " + errBuf);
        }
        PluginLog.debug("Objective OCL compiled: " + ctx.objectiveExpr);

        Evaluator evaluator = new Evaluator();

        // Save and strip all decision-var links from state.
        Map<String, Set<MLink>> savedLinks = new HashMap<>();
        for (DecisionVar dv : ctx.decisionVars) {
            MAssociation assoc = ctx.model.getAssociation(dv.association);
            if (assoc == null) continue;
            Set<MLink> existing = new HashSet<>(ctx.state.linksOfAssociation(assoc).links());
            savedLinks.put(dv.association, existing);
        }
        stripDecisionLinks(ctx);

        try {
            SamplingPass cost = sampleCostTerms(n, flatVars, ctx, evaluator, objExpr, progress);
            double B = computePenaltyWeight(n, cost.lin, cost.quad);
            SamplingPass penalty = samplePenaltyTerms(n, flatVars, ctx, evaluator, progress);

            CombinedCoefficients combined = combine(n, cost, penalty, B);

            restoreLinks(ctx, savedLinks);

            report(progress, "Running exactness check…");
            List<ExactnessPoint> exactnessPoints = checkExactness(n, combined.c, combined.lin,
                    combined.quad, flatVars, ctx, evaluator, objExpr, B, savedLinks);
            boolean exact = exactnessPoints.stream()
                    .noneMatch(p -> p.evalFailed || p.error() >= EPS);

            if (!exact) {
                PluginLog.warn("QuboEngine: exactness check FAILED — q(x) ≠ f(x) on held-out points. "
                    + "The combined objective+penalty is not representable as a degree-2 polynomial. "
                    + "Common causes: "
                    + "(1) An OCL invariant uses a boolean pass/fail condition — "
                    + "reformulate by counting violations (integer sum) instead of returning true/false. "
                    + "(2) The objective OCL expression contains interactions between 3+ decision variables "
                    + "(e.g. a product of three link memberships) — decompose into pairwise terms.");
            } else {
                PluginLog.info("Exactness check: PASS");
            }

            QuboResult result = buildResult(n, nSamples, combined, varLabels, B,
                    cost.samples, penalty.samples, exactnessPoints, exact);
            PluginLog.info("Derive complete: " + result);
            return result;

        } catch (Exception e) {
            restoreLinks(ctx, savedLinks);
            throw e;
        }
    }

    private static void report(Consumer<String> cb, String msg) {
        if (cb != null) cb.accept(msg);
    }

    private static void checkCancelled() throws InterruptedException {
        if (Thread.interrupted()) throw new InterruptedException("Derivation cancelled");
    }

    // -------------------------------------------------------------------------
    // Sampling passes
    // -------------------------------------------------------------------------

    /** Result of one sampling pass (cost-only or penalty-only): constant, linear and quadratic terms, plus the raw samples taken. */
    private static final class SamplingPass {
        final double c;
        final double[] lin;
        final double[][] quad;
        final List<SampleRecord> samples;

        SamplingPass(double c, double[] lin, double[][] quad, List<SampleRecord> samples) {
            this.c = c;
            this.lin = lin;
            this.quad = quad;
            this.samples = samples;
        }
    }

    /** Result of merging cost and penalty passes into the final QUBO polynomial coefficients. */
    private static final class CombinedCoefficients {
        final double c;
        final double[] lin;
        final double[][] quad;

        CombinedCoefficients(double c, double[] lin, double[][] quad) {
            this.c = c;
            this.lin = lin;
            this.quad = quad;
        }
    }

    private static SamplingPass sampleCostTerms(int n, List<DVPair> flatVars, QuboContext ctx,
                                                 Evaluator evaluator, Expression objExpr,
                                                 Consumer<String> progress) throws Exception {
        double[] lin = new double[n];
        double[][] quad = new double[n][n];
        int[] zeros = new int[n];
        List<SampleRecord> samples = new ArrayList<>(1 + n + n * (n - 1) / 2);

        report(progress, "Sampling: cost constant term");
        double c = evalCost(zeros, flatVars, ctx, evaluator, objExpr);
        samples.add(new SampleRecord(zeros, "cost_const", c, -1, -1));

        for (int i = 0; i < n; i++) {
            checkCancelled();
            report(progress, "Sampling: cost linear (" + (i + 1) + "/" + n + ")…");
            int[] ei = new int[n];
            ei[i] = 1;
            double rawLin = evalCost(ei, flatVars, ctx, evaluator, objExpr);
            samples.add(new SampleRecord(ei, "cost_lin_i=" + i, rawLin, i, i));
            lin[i] = rawLin - c;
        }

        int totalPairs = n * (n - 1) / 2;
        for (int i = 0, pairCount = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                checkCancelled();
                pairCount++;
                report(progress, "Sampling: cost quadratic (" + pairCount + "/" + totalPairs + ")…");
                int[] eij = new int[n];
                eij[i] = 1;
                eij[j] = 1;
                double rawQuad = evalCost(eij, flatVars, ctx, evaluator, objExpr);
                samples.add(new SampleRecord(eij, "cost_quad_i=" + i + "_j=" + j, rawQuad, i, j));
                quad[i][j] = rawQuad - lin[i] - lin[j] - c;
            }
        }
        return new SamplingPass(c, lin, quad, samples);
    }

    private static SamplingPass samplePenaltyTerms(int n, List<DVPair> flatVars, QuboContext ctx,
                                                    Evaluator evaluator,
                                                    Consumer<String> progress) throws Exception {
        double[] lin = new double[n];
        double[][] quad = new double[n][n];
        int[] zeros = new int[n];
        List<SampleRecord> samples = new ArrayList<>(1 + n + n * (n - 1) / 2);

        report(progress, "Sampling: penalty constant term");
        double c = evalPenalty(zeros, flatVars, ctx, evaluator);
        samples.add(new SampleRecord(zeros, "pen_const", c, -1, -1));

        for (int i = 0; i < n; i++) {
            checkCancelled();
            report(progress, "Sampling: penalty linear (" + (i + 1) + "/" + n + ")…");
            int[] ei = new int[n];
            ei[i] = 1;
            double rawLin = evalPenalty(ei, flatVars, ctx, evaluator);
            samples.add(new SampleRecord(ei, "pen_lin_i=" + i, rawLin, i, i));
            lin[i] = rawLin - c;
        }

        int totalPairs = n * (n - 1) / 2;
        for (int i = 0, pairCount = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                checkCancelled();
                pairCount++;
                report(progress, "Sampling: penalty quadratic (" + pairCount + "/" + totalPairs + ")…");
                int[] eij = new int[n];
                eij[i] = 1;
                eij[j] = 1;
                double rawQuad = evalPenalty(eij, flatVars, ctx, evaluator);
                samples.add(new SampleRecord(eij, "pen_quad_i=" + i + "_j=" + j, rawQuad, i, j));
                quad[i][j] = rawQuad - lin[i] - lin[j] - c;
            }
        }
        return new SamplingPass(c, lin, quad, samples);
    }

    /**
     * Verma-Lewis per-row max penalty weight (Pauckert et al., GECCO '23 §2.1).
     * For each row i: posRow = lin[i]+ + sum of positive quad[i][j];
     *                 negRow = |lin[i]-| + sum of |negative quad[i][j]|.
     * B = max over all rows of max(posRow, negRow) + 1.
     * Tighter than the global sum bound; smaller B = better annealing landscape.
     */
    private static double computePenaltyWeight(int n, double[] costLin, double[][] costQuad) {
        double B = 1.0;
        for (int i = 0; i < n; i++) {
            double posRow = costLin[i] > 0 ? costLin[i] : 0.0;
            double negRow = costLin[i] < 0 ? -costLin[i] : 0.0;
            for (int j = i + 1; j < n; j++) {
                double cij = costQuad[i][j];
                if (cij > 0) posRow += cij;
                else         negRow += -cij;
            }
            B = Math.max(B, Math.max(posRow, negRow));
        }
        B += 1.0;
        PluginLog.info("QuboEngine: B=" + B + " (Verma-Lewis per-row max)");
        return B;
    }

    /** Merges cost(x) + B * penalty(x) into a single set of QUBO coefficients. */
    private static CombinedCoefficients combine(int n, SamplingPass cost, SamplingPass penalty, double B) {
        double c = cost.c + B * penalty.c;
        double[] lin = new double[n];
        double[][] quad = new double[n][n];
        for (int i = 0; i < n; i++) {
            lin[i] = cost.lin[i] + B * penalty.lin[i];
        }
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                quad[i][j] = cost.quad[i][j] + B * penalty.quad[i][j];
            }
        }
        return new CombinedCoefficients(c, lin, quad);
    }

    /** Trims near-zero coefficients (per EPS) and assembles the final QuboResult. */
    private static QuboResult buildResult(int n, int nSamples, CombinedCoefficients combined,
                                           List<String> varLabels, double B,
                                           List<SampleRecord> costSamples, List<SampleRecord> penaltySamples,
                                           List<ExactnessPoint> exactnessPoints, boolean exact) {
        Map<Integer, Double> linearMap = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            if (Math.abs(combined.lin[i]) >= EPS) linearMap.put(i, combined.lin[i]);
        }
        Map<String, Double> quadMap = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (Math.abs(combined.quad[i][j]) >= EPS) quadMap.put(i + "," + j, combined.quad[i][j]);
            }
        }
        return new QuboResult(n, nSamples, exact, combined.c, linearMap, quadMap,
                varLabels, B, 0L, costSamples, penaltySamples, exactnessPoints);
    }

    // -------------------------------------------------------------------------

    /**
     * Evaluates the derived QUBO polynomial q(x) against the true f(x) on up to
     * {@link QuboConstants#EXACTNESS_SAMPLE_COUNT} random held-out binary vectors
     * (Hamming weight ≥ {@link QuboConstants#EXACTNESS_MIN_HAMMING_WEIGHT}) and
     * returns all evaluation points for diagnostic display. Does not short-circuit
     * on mismatch — all points are collected so the full error table is available
     * to the user.
     *
     * <p>For small n where fewer than {@link QuboConstants#EXACTNESS_SAMPLE_COUNT}
     * distinct qualifying vectors exist, the method stops after the retry budget
     * is exhausted and returns however many points were collected.
     */
    private static List<ExactnessPoint> checkExactness(int n, double c, double[] linCoeffs,
                                                        double[][] quadCoeffs, List<DVPair> flatVars,
                                                        QuboContext ctx, Evaluator evaluator,
                                                        Expression objExpr, double B,
                                                        Map<String, Set<MLink>> savedLinks) {
        stripDecisionLinks(ctx);

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
                // skip vectors below the minimum Hamming weight — they are training samples, exact by construction
                int ones = 0;
                for (int v : x) ones += v;
                if (ones < QuboConstants.EXACTNESS_MIN_HAMMING_WEIGHT) continue;

                k++;
                double qx = c;
                for (int i = 0; i < n; i++) qx += linCoeffs[i] * x[i];
                for (int i = 0; i < n; i++) {
                    for (int j = i + 1; j < n; j++) {
                        qx += quadCoeffs[i][j] * x[i] * x[j];
                    }
                }

                try {
                    double fx = evalCost(x, flatVars, ctx, evaluator, objExpr)
                              + B * evalPenalty(x, flatVars, ctx, evaluator);
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
            restoreLinks(ctx, savedLinks);
        }
        return Collections.unmodifiableList(points);
    }

    /**
     * Strips all decision-var links from state without saving them.
     * Used before sampling passes and before held-out exactness evaluation.
     */
    private static void stripDecisionLinks(QuboContext ctx) {
        for (DecisionVar dv : ctx.decisionVars) {
            MAssociation assoc = ctx.model.getAssociation(dv.association);
            if (assoc == null) continue;
            for (MLink link : new HashSet<>(ctx.state.linksOfAssociation(assoc).links())) {
                try { ctx.state.deleteLink(link); } catch (Exception e) { /* best effort */ }
            }
        }
    }

    /**
     * Purges any lingering decision-var links from the state, then re-inserts
     * every link captured in {@code savedLinks}.
     */
    private static void restoreLinks(QuboContext ctx, Map<String, Set<MLink>> savedLinks) {
        stripDecisionLinks(ctx);
        for (DecisionVar dv : ctx.decisionVars) {
            Set<MLink> orig = savedLinks.get(dv.association);
            if (orig == null) continue;
            for (MLink l : orig) {
                ctx.state.insertLink(l);
            }
        }
    }

    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    /**
     * Inserts the links implied by binary vector {@code x}, runs {@code body},
     * then removes exactly the links it inserted (best-effort) — regardless of
     * whether {@code body} succeeds. Shared by {@link #evalCost} and {@link #evalPenalty},
     * which previously duplicated this insert/evaluate/cleanup sequence.
     */
    private static <T> T withTemporaryLinks(int[] x, List<DVPair> flatVars, QuboContext ctx,
                                             ThrowingSupplier<T> body) throws Exception {
        MSystemState state = ctx.state;
        List<MLink> inserted = new ArrayList<>();
        try {
            for (int i = 0; i < x.length; i++) {
                if (x[i] == 1) {
                    DVPair p = flatVars.get(i);
                    MAssociation assoc = ctx.model.getAssociation(p.dv.association);
                    if (assoc != null) {
                        MLink link = state.createLink(
                                assoc, Arrays.asList(p.a, p.b), Collections.emptyList());
                        inserted.add(link);
                    }
                }
            }
            return body.get();
        } finally {
            for (MLink link : inserted) {
                try { state.deleteLink(link); } catch (Exception e) { /* best effort */ }
            }
        }
    }

    /**
     * Evaluates the objective cost for binary vector x (no penalty).
     * State must have no decision-var links before this call; restored after.
     */
    private static double evalCost(int[] x, List<DVPair> flatVars,
                                   QuboContext ctx, Evaluator evaluator,
                                   Expression objExpr) throws Exception {
        return withTemporaryLinks(x, flatVars, ctx, () -> {
            double obj = computeObjective(objExpr, evaluator, ctx.state);
            return ctx.minimise ? obj : -obj;
        });
    }

    /**
     * Evaluates the penalty for binary vector x (no objective).
     * State must have no decision-var links before this call; restored after.
     */
    private static double evalPenalty(int[] x, List<DVPair> flatVars,
                                      QuboContext ctx, Evaluator evaluator) throws Exception {
        return withTemporaryLinks(x, flatVars, ctx, () -> computePenalty(ctx, evaluator, ctx.state));
    }

    private static double computePenalty(QuboContext ctx, Evaluator evaluator,
                                          MSystemState state) {
        double penalty = 0.0;
        for (MClassInvariant inv : ctx.invariants) {
            String className = inv.cls().name();
            List<MObject> objs = ctx.objectsByClass.getOrDefault(className, Collections.emptyList());
            for (MObject obj : objs) {
                VarBindings bindings = new VarBindings();
                bindings.push("self", new ObjectValue(obj.cls(), obj));
                Value result;
                try {
                    result = evaluator.eval(inv.bodyExpression(), state, bindings);
                } catch (Exception e) {
                    PluginLog.debug("Invariant eval failed for " + obj.name() + ": " + e.getMessage());
                    result = null;
                }
                boolean holds = (result instanceof BooleanValue) && ((BooleanValue) result).isTrue();
                if (!holds) penalty += 1.0;
            }
        }
        return penalty;
    }

    private static double computeObjective(Expression objExpr, Evaluator evaluator,
                                            MSystemState state) {
        try {
            Value v = evaluator.eval(objExpr, state);
            if (v instanceof IntegerValue) return ((IntegerValue) v).value();
            if (v instanceof RealValue)    return ((RealValue) v).value();
        } catch (Exception e) {
            PluginLog.warn("QuboEngine: objective OCL evaluation failed — treating as 0.0. "
                + "Check that the objective expression returns Integer or Real. Error: " + e.getMessage());
        }
        return 0.0;
    }

    // -------------------------------------------------------------------------

    /**
     * Builds the flat ordered list of (DecisionVar, objA, objB) triples that
     * mirrors the variable ordering defined in QuboContext.varIndex().
     */
    private static List<DVPair> buildFlatVars(QuboContext ctx) {
        List<DVPair> flat = new ArrayList<>(ctx.nVars);
        for (DecisionVar dv : ctx.decisionVars) {
            List<MObject> bObjs = ctx.objectsByClass.getOrDefault(
                    dv.classB, Collections.emptyList());
            List<MObject[]> pairs = new ArrayList<>(dv.domain.size() * bObjs.size());
            for (MObject a : dv.domain) {
                for (MObject b : bObjs) {
                    pairs.add(new MObject[]{a, b});
                }
            }
            pairs.sort(Comparator.<MObject[], String>comparing(p -> p[0].name())
                                  .thenComparing(p -> p[1].name()));
            for (MObject[] pair : pairs) {
                flat.add(new DVPair(dv, pair[0], pair[1]));
            }
        }
        return flat;
    }

    private static class DVPair {
        final DecisionVar dv;
        final MObject a;
        final MObject b;

        DVPair(DecisionVar dv, MObject a, MObject b) {
            this.dv = dv;
            this.a  = a;
            this.b  = b;
        }
    }
}
