package org.tzi.use.plugin.use2qubo.qubo.engine;

import org.tzi.use.parser.ocl.OCLCompiler;
import org.tzi.use.plugin.use2qubo.qubo.context.DecisionVar;
import org.tzi.use.plugin.use2qubo.qubo.context.QuboContext;
import org.tzi.use.plugin.use2qubo.qubo.result.ExactnessPoint;
import org.tzi.use.plugin.use2qubo.qubo.result.QuboResult;
import org.tzi.use.plugin.use2qubo.qubo.result.SampleRecord;
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
        PluginLog.info("QuboEngine.derive: nVars=" + n + ", maxDegree=" + ctx.maxDegree);

        report(progress, "Building variable index…");
        List<DVPair> flatVars = buildFlatVars(ctx);
        if (flatVars.size() != n) {
            throw new IllegalStateException(
                    "Flat var count " + flatVars.size() + " != nVars " + n);
        }
        List<String> varLabels = buildVarLabels(flatVars);
        Expression objExpr = compileObjective(ctx, progress);
        Evaluator evaluator = new Evaluator();

        Map<String, Set<MLink>> savedLinks = saveAndStripLinks(ctx);
        try {
            QuboResult result = deriveWithClearedState(ctx, n, flatVars, varLabels,
                    objExpr, evaluator, progress, savedLinks);
            PluginLog.info("Derive complete: " + result);
            return result;
        } finally {
            restoreLinks(ctx, savedLinks);
        }
    }

    /** Runs the sampling (with degree escalation), quadratization and exactness-check steps,
     *  assuming decision-var links are already stripped. */
    private static QuboResult deriveWithClearedState(QuboContext ctx, int n,
            List<DVPair> flatVars, List<String> varLabels, Expression objExpr, Evaluator evaluator,
            Consumer<String> progress, Map<String, Set<MLink>> savedLinks) throws Exception {

        int maxDegree = Math.max(2, ctx.maxDegree);

        report(progress, "Sampling: cost degree ≤2…");
        PolySampler.Result cost = PolySampler.sample(n, 0, 2, Collections.emptyMap(), "cost",
                x -> evalCost(x, flatVars, ctx, evaluator, objExpr), progress);
        double[] costLin = new double[n];
        double[][] costQuad = new double[n][n];
        flattenDegree2(cost.coeffs, n, costLin, costQuad);
        double B = computePenaltyWeight(n, costLin, costQuad);

        report(progress, "Sampling: penalty degree ≤2…");
        PolySampler.Result penalty = PolySampler.sample(n, 0, 2, Collections.emptyMap(), "pen",
                x -> evalPenalty(x, flatVars, ctx, evaluator), progress);

        Map<VarSet, Double> combined = combine(cost.coeffs, penalty.coeffs, B);
        int degree = 2;

        restoreLinks(ctx, savedLinks);
        report(progress, "Running exactness check (degree " + degree + ")…");
        List<ExactnessPoint> exactnessPoints = checkExactness(n, combined, flatVars, ctx, evaluator, objExpr, B, savedLinks);
        boolean degreeExact = logExactnessOutcome(exactnessPoints, degree);

        while (!degreeExact && degree < maxDegree) {
            int nextDegree = degree + 1;
            report(progress, "Exactness failed at degree " + degree + "; escalating to degree " + nextDegree + "…");
            PluginLog.info("QuboEngine: escalating sampling to degree " + nextDegree);

            // checkExactness restores the original scenario links in its own finally block;
            // strip them again before sampling, same as the initial degree-2 pass does.
            stripDecisionLinks(ctx);

            PolySampler.Result costNext = PolySampler.sample(n, nextDegree, nextDegree, cost.coeffs, "cost",
                    x -> evalCost(x, flatVars, ctx, evaluator, objExpr), progress);
            List<SampleRecord> costSamples = new ArrayList<>(cost.samples);
            costSamples.addAll(costNext.samples);
            cost = new PolySampler.Result(costNext.coeffs, costSamples);

            PolySampler.Result penaltyNext = PolySampler.sample(n, nextDegree, nextDegree, penalty.coeffs, "pen",
                    x -> evalPenalty(x, flatVars, ctx, evaluator), progress);
            List<SampleRecord> penaltySamples = new ArrayList<>(penalty.samples);
            penaltySamples.addAll(penaltyNext.samples);
            penalty = new PolySampler.Result(penaltyNext.coeffs, penaltySamples);

            combined = combine(cost.coeffs, penalty.coeffs, B);
            degree = nextDegree;

            restoreLinks(ctx, savedLinks);
            report(progress, "Running exactness check (degree " + degree + ")…");
            exactnessPoints = checkExactness(n, combined, flatVars, ctx, evaluator, objExpr, B, savedLinks);
            degreeExact = logExactnessOutcome(exactnessPoints, degree);
        }

        int nSamples = cost.samples.size() + penalty.samples.size();
        return buildResult(n, nSamples, combined, varLabels, B,
                cost.samples, penalty.samples, exactnessPoints, degreeExact, degree);
    }

    private static List<String> buildVarLabels(List<DVPair> flatVars) {
        List<String> varLabels = new ArrayList<>(flatVars.size());
        for (DVPair p : flatVars) {
            varLabels.add(p.dv.association + "(" + p.a.name() + "," + p.b.name() + ")");
        }
        return varLabels;
    }

    private static Expression compileObjective(QuboContext ctx, Consumer<String> progress) {
        report(progress, "Compiling objective OCL…");
        StringWriter errBuf = new StringWriter();
        Expression objExpr = OCLCompiler.compileExpression(
                ctx.model, ctx.objectiveExpr, "objective",
                new PrintWriter(errBuf), new VarBindings());
        if (objExpr == null) {
            throw new IllegalStateException("Cannot parse objective OCL: " + errBuf);
        }
        PluginLog.debug("Objective OCL compiled: " + ctx.objectiveExpr);
        return objExpr;
    }

    /** Captures all existing decision-var links, then strips them from state so sampling starts from a clean slate. */
    private static Map<String, Set<MLink>> saveAndStripLinks(QuboContext ctx) {
        Map<String, Set<MLink>> savedLinks = new HashMap<>();
        for (DecisionVar dv : ctx.decisionVars) {
            MAssociation assoc = ctx.model.getAssociation(dv.association);
            if (assoc == null) continue;
            Set<MLink> existing = new HashSet<>(ctx.state.linksOfAssociation(assoc).links());
            savedLinks.put(dv.association, existing);
        }
        stripDecisionLinks(ctx);
        return savedLinks;
    }

    private static boolean logExactnessOutcome(List<ExactnessPoint> exactnessPoints, int degree) {
        boolean exact = exactnessPoints.stream()
                .noneMatch(p -> p.evalFailed || p.error() >= EPS);
        if (!exact) {
            PluginLog.warn("QuboEngine: exactness check FAILED at degree " + degree
                + " — q(x) ≠ f(x) on held-out points. "
                + "Common causes: "
                + "(1) An OCL invariant uses a boolean pass/fail condition — "
                + "reformulate by counting violations (integer sum) instead of returning true/false. "
                + "(2) The objective/penalty involves interactions between more decision variables "
                + "than the current degree cap allows — raise max_degree.");
        } else {
            PluginLog.info("Exactness check: PASS at degree " + degree);
        }
        return exact;
    }

    private static void report(Consumer<String> cb, String msg) {
        if (cb != null) cb.accept(msg);
    }

    // -------------------------------------------------------------------------
    // Degree-2 flattening / penalty weight / combination
    // -------------------------------------------------------------------------

    /** Extracts the degree-1 and degree-2 coefficients of {@code coeffs} into flat arrays
     *  (used only for the Verma-Lewis penalty-weight computation, which is degree-2 specific by design). */
    private static void flattenDegree2(Map<VarSet, Double> coeffs, int n, double[] lin, double[][] quad) {
        for (Map.Entry<VarSet, Double> e : coeffs.entrySet()) {
            VarSet J = e.getKey();
            if (J.degree() == 1) {
                lin[J.vars()[0]] += e.getValue();
            } else if (J.degree() == 2) {
                int[] v = J.vars();
                quad[v[0]][v[1]] += e.getValue();
            }
        }
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

    /** Merges cost(x) + B * penalty(x) into a single pseudo-Boolean polynomial, any degree. */
    private static Map<VarSet, Double> combine(Map<VarSet, Double> cost, Map<VarSet, Double> penalty, double B) {
        Map<VarSet, Double> combined = new LinkedHashMap<>(cost);
        for (Map.Entry<VarSet, Double> e : penalty.entrySet()) {
            combined.merge(e.getKey(), B * e.getValue(), Double::sum);
        }
        return combined;
    }

    /** Evaluates a pseudo-Boolean polynomial (any degree) at binary vector x. */
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

    // -------------------------------------------------------------------------
    // Result assembly (quadratization when degree > 2)
    // -------------------------------------------------------------------------

    /** Trims near-zero coefficients (per EPS), quadratizes if needed, and assembles the final QuboResult. */
    private static QuboResult buildResult(int n, int nSamples, Map<VarSet, Double> combined,
            List<String> varLabels, double B, List<SampleRecord> costSamples, List<SampleRecord> penaltySamples,
            List<ExactnessPoint> exactnessPoints, boolean degreeExact, int degree) {

        double constant = combined.getOrDefault(VarSet.EMPTY, 0.0);

        if (degree <= 2) {
            Map<Integer, Double> linearMap = new LinkedHashMap<>();
            Map<String, Double> quadMap = new LinkedHashMap<>();
            for (Map.Entry<VarSet, Double> e : combined.entrySet()) {
                VarSet J = e.getKey();
                if (J.degree() == 1 && Math.abs(e.getValue()) >= EPS) {
                    linearMap.put(J.vars()[0], e.getValue());
                } else if (J.degree() == 2 && Math.abs(e.getValue()) >= EPS) {
                    int[] v = J.vars();
                    quadMap.put(v[0] + "," + v[1], e.getValue());
                }
            }
            return new QuboResult(n, nSamples, degreeExact, constant, linearMap, quadMap,
                    varLabels, B, 0L, costSamples, penaltySamples, exactnessPoints, degree, 0, 0.0);
        }

        Quadratizer.Result qz = Quadratizer.reduce(n, combined, varLabels);
        boolean quadExact = verifyQuadratization(qz, n, exactnessPoints);
        boolean exact = degreeExact && quadExact;
        if (!quadExact) {
            PluginLog.warn("QuboEngine: quadratization verification FAILED — the reduced QUBO does not "
                    + "reproduce the degree-" + degree + " polynomial on held-out points.");
        } else {
            PluginLog.info("Quadratization verification: PASS (nAncilla=" + qz.nAncilla
                    + ", penaltyWeight=" + qz.penaltyWeight + ")");
        }

        int totalVars = n + qz.nAncilla;
        List<String> extendedLabels = new ArrayList<>(varLabels);
        extendedLabels.addAll(qz.ancillaLabels);

        Map<Integer, Double> linearMap = new LinkedHashMap<>();
        for (int i = 0; i < totalVars; i++) {
            if (Math.abs(qz.lin[i]) >= EPS) linearMap.put(i, qz.lin[i]);
        }
        Map<String, Double> quadMap = new LinkedHashMap<>();
        for (int i = 0; i < totalVars; i++) {
            for (int j = i + 1; j < totalVars; j++) {
                if (Math.abs(qz.quad[i][j]) >= EPS) quadMap.put(i + "," + j, qz.quad[i][j]);
            }
        }

        return new QuboResult(totalVars, nSamples, exact, qz.constant, linearMap, quadMap,
                extendedLabels, B, 0L, costSamples, penaltySamples, exactnessPoints, degree,
                qz.nAncilla, qz.penaltyWeight);
    }

    /**
     * Confirms the quadratized QUBO reproduces the true f(x) on the same held-out points already
     * used for degree-K exactness, by setting each ancilla bit to the exact product it encodes
     * (Rosenberg's substitution guarantees the minimum over the ancilla reaches this value, so no
     * actual minimisation is needed to check it).
     */
    private static boolean verifyQuadratization(Quadratizer.Result qz, int n, List<ExactnessPoint> exactnessPoints) {
        if (qz.nAncilla == 0) return true;
        for (ExactnessPoint p : exactnessPoints) {
            if (p.evalFailed) continue;
            double[] full = new double[n + qz.nAncilla];
            for (int i = 0; i < n; i++) full[i] = p.vector[i];
            for (int k = 0; k < qz.nAncilla; k++) {
                // ancilla pairs are encoded positionally in ancillaLabels' creation order; recompute
                // the product directly from the already-resolved earlier entries of full[].
                full[n + k] = ancillaProduct(qz, k, full);
            }
            double qx = evalQuadratic(qz.constant, qz.lin, qz.quad, full);
            if (Math.abs(qx - p.fx) >= EPS) return false;
        }
        return true;
    }

    private static double ancillaProduct(Quadratizer.Result qz, int k, double[] full) {
        int[] pair = qz.ancillaPairs.get(k);
        return full[pair[0]] * full[pair[1]];
    }

    private static double evalQuadratic(double c, double[] lin, double[][] quad, double[] x) {
        double r = c;
        for (int i = 0; i < lin.length; i++) r += lin[i] * x[i];
        for (int i = 0; i < quad.length; i++) {
            for (int j = i + 1; j < quad.length; j++) {
                r += quad[i][j] * x[i] * x[j];
            }
        }
        return r;
    }

    // -------------------------------------------------------------------------

    /**
     * Evaluates the derived polynomial q(x) against the true f(x) on up to
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
    private static List<ExactnessPoint> checkExactness(int n, Map<VarSet, Double> combined,
            List<DVPair> flatVars, QuboContext ctx, Evaluator evaluator, Expression objExpr, double B,
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
                double qx = evalPoly(combined, x);

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
