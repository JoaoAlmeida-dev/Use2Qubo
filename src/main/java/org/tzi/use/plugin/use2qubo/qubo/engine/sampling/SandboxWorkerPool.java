package org.tzi.use.plugin.use2qubo.qubo.engine.sampling;

import org.tzi.use.plugin.use2qubo.qubo.context.QuboContext;
import org.tzi.use.plugin.use2qubo.qubo.context.QuboContextBuilder;
import org.tzi.use.plugin.use2qubo.qubo.context.SandboxSystemFactory;
import org.tzi.use.plugin.use2qubo.qubo.engine.ProgressEvent;
import org.tzi.use.plugin.use2qubo.qubo.engine.eval.DecisionLinkSampler;
import org.tzi.use.plugin.use2qubo.qubo.engine.eval.ObjectiveEvaluator;
import org.tzi.use.plugin.use2qubo.qubo.engine.eval.PenaltyEvaluator;
import org.tzi.use.plugin.use2qubo.qubo.engine.index.DVPair;
import org.tzi.use.plugin.use2qubo.util.PluginLog;
import org.tzi.use.plugin.use2qubo.util.QuboConstants;
import org.tzi.use.uml.mm.MModel;
import org.tzi.use.uml.ocl.expr.Evaluator;
import org.tzi.use.uml.ocl.expr.Expression;
import org.tzi.use.uml.sys.MObject;
import org.tzi.use.uml.sys.MSystemState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * A pool of {@link SandboxWorker}s, each holding its own throwaway {@code MSystem}/{@code MSystemState}
 * clone (via {@link SandboxSystemFactory}), so {@code C(n,m)} sample evaluations for one degree pass
 * can run on N threads without contending on a single shared, mutable {@link MSystemState} — the
 * blocker that kept {@code PolySampler}'s sample loop and {@code ExactnessChecker}'s exhaustive
 * {@code 2^n} loop sequential. Built once per {@code QuboEngine.derive} call and reused across the
 * cost pass, the penalty pass, every degree-escalation iteration, and the exhaustive exactness check —
 * none of those mutate anything a sandbox clone depends on (decision-var links are always
 * inserted/stripped per sample; other attributes/links are read-only during derivation).
 */
public final class SandboxWorkerPool implements AutoCloseable {

    /** Which black-box function a batch evaluates. */
    public enum EvalKind { COST, PENALTY }

    /** A pool worker's public face: evaluates the true OCL cost/penalty on its own sandbox, for
     *  callers (like {@code ExactnessChecker}) that need both per point rather than one batched
     *  {@link EvalKind}. */
    public interface Worker {
        double evalCost(int[] x) throws Exception;
        double evalPenalty(int[] x) throws Exception;
    }

    /** One arbitrary per-index unit of work handed to {@link #evaluateIndexed}, run against a
     *  single worker's sandbox; {@code index}/{@code x} are the {@code 2^n} enumeration index and
     *  its binary vector. */
    @FunctionalInterface
    public interface IndexTask<R> {
        R run(Worker worker, long index, int[] x) throws Exception;
    }

    /** One worker's private sandbox: its own MModel-sharing MSystem/MSystemState clone, decision
     *  variables and penalty tasks remapped to that clone's MObjects, and its own Evaluator (not
     *  thread-safe to share — mutable eval-context field, per PenaltyEvaluator's existing javadoc). */
    private static final class SandboxWorker implements Worker {
        final MModel model;
        final MSystemState state;
        final List<DVPair> flatVars;
        final List<PenaltyEvaluator.PenaltyTask> penaltyTasks;
        final Expression objExpr;
        final boolean minimise;
        final Evaluator evaluator = new Evaluator();

        SandboxWorker(MModel model, MSystemState state, List<DVPair> flatVars,
                      List<PenaltyEvaluator.PenaltyTask> penaltyTasks, Expression objExpr, boolean minimise) {
            this.model = model;
            this.state = state;
            this.flatVars = flatVars;
            this.penaltyTasks = penaltyTasks;
            this.objExpr = objExpr;
            this.minimise = minimise;
        }

        @Override
        public double evalCost(int[] x) throws Exception {
            return DecisionLinkSampler.withTemporaryLinks(x, flatVars, model, state, () -> {
                double obj = ObjectiveEvaluator.computeObjective(objExpr, evaluator, state);
                return minimise ? obj : -obj;
            });
        }

        @Override
        public double evalPenalty(int[] x) throws Exception {
            return DecisionLinkSampler.withTemporaryLinks(x, flatVars, model, state,
                    () -> PenaltyEvaluator.computePenalty(penaltyTasks, state));
        }

        double eval(EvalKind kind, int[] x) throws Exception {
            return kind == EvalKind.COST ? evalCost(x) : evalPenalty(x);
        }
    }

    private final List<SandboxWorker> workers;
    private final ExecutorService executor;

    private SandboxWorkerPool(List<SandboxWorker> workers, ExecutorService executor) {
        this.workers = workers;
        this.executor = executor;
    }

    /**
     * Builds one sandbox clone per worker (via {@link SandboxSystemFactory}), sized
     * {@code min(availableProcessors(), MAX_SAMPLE_WORKERS)}. Each clone's objects/links mirror
     * {@code ctx}'s at build time; {@code flatVars} and the penalty tasks are remapped into each
     * clone's own {@code MObject}s through the sandbox's name-keyed map.
     */
    public static SandboxWorkerPool build(QuboContext ctx, List<DVPair> flatVars, Expression objExpr)
            throws Exception {
        return build(ctx, flatVars, objExpr, null);
    }

    /**
     * @param workerOverride explicit worker count, or {@code null} to fall back to
     *                       {@code min(availableProcessors(), MAX_SAMPLE_WORKERS)}. An explicit
     *                       value bypasses {@code MAX_SAMPLE_WORKERS} — the caller asked for it
     *                       directly (e.g. the CLI's {@code --workers} flag) — but is still floored
     *                       at 1.
     */
    public static SandboxWorkerPool build(QuboContext ctx, List<DVPair> flatVars, Expression objExpr,
                                           Integer workerOverride) throws Exception {
        int available = Runtime.getRuntime().availableProcessors();
        int nWorkers = workerOverride != null
                ? Math.max(1, workerOverride)
                : Math.max(1, Math.min(available, QuboConstants.MAX_SAMPLE_WORKERS));

        long startNanos = System.nanoTime();
        List<SandboxWorker> workers = new ArrayList<>(nWorkers);
        for (int i = 0; i < nWorkers; i++) {
            workers.add(buildWorker(ctx, flatVars, objExpr));
        }
        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;

        PluginLog.info("SandboxWorkerPool: built " + nWorkers + " workers (availableProcessors="
                + available + ", cap=" + QuboConstants.MAX_SAMPLE_WORKERS
                + (workerOverride != null ? ", explicit override=" + workerOverride : "")
                + "), sandbox cloning took " + elapsedMs + "ms total");

        ExecutorService executor = Executors.newFixedThreadPool(nWorkers);
        return new SandboxWorkerPool(workers, executor);
    }

    private static SandboxWorker buildWorker(QuboContext ctx, List<DVPair> flatVars, Expression objExpr)
            throws Exception {
        // Fresh snapshot of ctx.state's *current* objects — not the stale ctx.objectsByClass field
        // (frozen at context-build time, before decision-link stripping ran; for an association-class
        // decision variable, that stripping also destroys the underlying MObjects, see javadoc above).
        Map<String, List<MObject>> currentObjectsByClass = QuboContextBuilder.buildObjectsByClass(ctx.state);

        SandboxSystemFactory.Sandbox sandbox = SandboxSystemFactory.build(
                ctx.model, ctx.state, currentObjectsByClass, /*copyAttributes*/ true, ctx.fixedLinks);

        Map<String, List<MObject>> sandboxObjectsByClass = new LinkedHashMap<>();
        for (Map.Entry<String, List<MObject>> e : currentObjectsByClass.entrySet()) {
            List<MObject> mapped = new ArrayList<>(e.getValue().size());
            for (MObject o : e.getValue()) mapped.add(sandbox.byName.get(o.name()));
            sandboxObjectsByClass.put(e.getKey(), mapped);
        }

        List<DVPair> sandboxFlatVars = new ArrayList<>(flatVars.size());
        for (DVPair p : flatVars) {
            sandboxFlatVars.add(new DVPair(p.dv, sandbox.byName.get(p.a.name()), sandbox.byName.get(p.b.name())));
        }

        List<PenaltyEvaluator.PenaltyTask> sandboxPenaltyTasks =
                PenaltyEvaluator.buildPenaltyTasks(ctx.invariants, sandboxObjectsByClass);

        return new SandboxWorker(ctx.model, sandbox.state, sandboxFlatVars, sandboxPenaltyTasks,
                objExpr, ctx.minimise);
    }

    /**
     * Evaluates {@code f(x)} for every {@code m}-subset of {@code {0,...,n-1}}, in the same
     * lexicographic order {@link VarSet#combinations} yields them, returning raw values aligned
     * to that order (index {@code i} = the {@code i}-th combo in that enumeration). Takes
     * {@code (n, m, total)} rather than a materialised {@code List<VarSet>} — each worker
     * generates its own chunk lazily via {@link VarSet#combinationsRange}, so no more than
     * {@code nWorkers} chunk-sized combo objects are ever alive at once, regardless of how large
     * {@code total} = {@code C(n,m)} is. (An earlier version passed a pre-built
     * {@code List<VarSet>} for chunking, which reintroduced the exact out-of-memory failure mode
     * the lazy iterator was originally written to avoid, at large n.)
     *
     * <p>Below {@link QuboConstants#PARALLEL_SAMPLE_THRESHOLD}, runs sequentially on a single
     * worker — dispatch/chunking overhead would outweigh any parallel win. Progress is reported
     * from the calling thread only: workers merely bump a shared counter, this method polls it,
     * preserving {@code progress}/{@code structuredProgress}'s "called from the calling thread"
     * contract that the GUI's Swing progress bar relies on.
     */
    public double[] evaluate(int n, int m, int total, EvalKind kind, String samplePrefix, int degree,
                              Consumer<String> progress, Consumer<ProgressEvent> structuredProgress) throws Exception {
        double[] raw = new double[total];
        if (total == 0) return raw;

        dispatchChunked(total, samplePrefix + " degree " + degree + ", " + total + " combos",
                (worker, start, end, tick) -> {
                    int i = start;
                    for (VarSet J : VarSet.combinationsRange(n, m, start, end - start)) {
                        raw[i] = worker.eval(kind, J.toVector(n));
                        i++;
                        tick.run();
                    }
                },
                (counts, totals) -> reportBatch(progress, structuredProgress, samplePrefix, degree,
                        counts, totals, sum(counts), total));

        return raw;
    }

    /** One arbitrary per-chunk unit of work handed to {@link #dispatchChunked}, run against a single
     *  worker's sandbox over {@code [start, end)}; {@code tick} must be called once per completed
     *  item so the shared progress counter (single-counter sequential fallback, per-worker
     *  {@code AtomicInteger} when pooled) stays accurate. No result-storage type: each caller's
     *  lambda closes over and writes directly into its own result array/list, sidestepping the
     *  double-boxing cost of a generic return value at {@code C(n,m)} scale. */
    @FunctionalInterface
    private interface ChunkWork {
        void run(SandboxWorker worker, int start, int end, Runnable tick) throws Exception;
    }

    /** Reports one progress tick: {@code counts}/{@code totals} are per-worker when pooled (same
     *  shape {@link #reportBatch} expects), or single-element {@code {done}}/{@code {total}} in the
     *  sequential fallback. Called from the calling thread only — either synchronously after each
     *  sequential item, or from the poll loop while pooled — matching the "calling thread only"
     *  contract {@code progress}/{@code structuredProgress} callers rely on. */
    @FunctionalInterface
    private interface ProgressTick {
        void onTick(int[] counts, int[] totals);
    }

    /**
     * Shared dispatch skeleton for {@link #evaluate} and {@link #evaluateIndexed}: below
     * {@link QuboConstants#PARALLEL_SAMPLE_THRESHOLD} (or with only one worker), runs {@code work}
     * on a single sandbox worker sequentially, ticking {@code onPollTick} after every item;
     * otherwise splits {@code [0, total)} into {@code nWorkers} contiguous chunks, submits one
     * {@code work} call per chunk to the executor, and polls a per-worker progress counter every
     * {@link QuboConstants#SAMPLE_PROGRESS_POLL_MS} until all chunks finish, surfacing the first
     * worker exception via {@code future.get()}.
     */
    private void dispatchChunked(int total, String logPrefix, ChunkWork work, ProgressTick onPollTick)
            throws Exception {
        int nWorkers = Math.min(workers.size(), Math.max(1, total));
        boolean pooled = total >= QuboConstants.PARALLEL_SAMPLE_THRESHOLD && nWorkers > 1;

        PluginLog.info("SandboxWorkerPool: " + (pooled ? "pooled" : "sequential fallback")
                + " batch — " + logPrefix
                + (pooled ? " across " + nWorkers + " workers" : " (threshold="
                        + QuboConstants.PARALLEL_SAMPLE_THRESHOLD + ")"));

        if (!pooled) {
            SandboxWorker w = workers.get(0);
            int[] count = {0};
            work.run(w, 0, total, () -> {
                count[0]++;
                onPollTick.onTick(new int[]{count[0]}, new int[]{total});
            });
            return;
        }

        int chunkSize = (total + nWorkers - 1) / nWorkers;
        int[] chunkTotal = new int[nWorkers];
        AtomicInteger[] chunkProgress = new AtomicInteger[nWorkers];
        for (int w = 0; w < nWorkers; w++) chunkProgress[w] = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>(nWorkers);
        for (int w = 0; w < nWorkers; w++) {
            int start = w * chunkSize;
            int end = Math.min(total, start + chunkSize);
            if (start >= end) continue;
            SandboxWorker worker = workers.get(w);
            int workerIdx = w;
            chunkTotal[w] = end - start;
            futures.add(executor.submit((Callable<Void>) () -> {
                work.run(worker, start, end, chunkProgress[workerIdx]::incrementAndGet);
                return null;
            }));
        }

        int lastReportedTotal = -1;
        while (!allDone(futures)) {
            Thread.sleep(QuboConstants.SAMPLE_PROGRESS_POLL_MS);
            int[] counts = currentCounts(chunkProgress);
            int sum = sum(counts);
            if (sum != lastReportedTotal) {
                lastReportedTotal = sum;
                onPollTick.onTick(counts, chunkTotal);
            }
        }
        for (Future<?> f : futures) f.get(); // surface any worker exception
        if (lastReportedTotal != total) {
            onPollTick.onTick(chunkTotal, chunkTotal);
        }
    }

    private static int[] currentCounts(AtomicInteger[] chunkProgress) {
        int[] counts = new int[chunkProgress.length];
        for (int w = 0; w < chunkProgress.length; w++) counts[w] = chunkProgress[w].get();
        return counts;
    }

    private static int sum(int[] values) {
        int s = 0;
        for (int v : values) s += v;
        return s;
    }

    /** Reports the aggregate line (plain + structured, {@code workerIndex=-1}, unchanged from
     *  before per-worker breakdown existed) plus one structured event per active worker
     *  ({@code workerTotals[w] <= 0} means that worker had no chunk this batch — skipped). */
    private static void reportBatch(Consumer<String> progress, Consumer<ProgressEvent> structuredProgress,
                                     String samplePrefix, int degree, int[] workerCounts, int[] workerTotals,
                                     int aggregateCount, int aggregateTotal) {
        report(progress, structuredProgress, samplePrefix, degree, aggregateCount, aggregateTotal);
        if (structuredProgress == null) return;
        String phaseLabel = "Sampling " + samplePrefix + " degree " + degree;
        for (int w = 0; w < workerCounts.length; w++) {
            if (workerTotals[w] <= 0) continue;
            structuredProgress.accept(new ProgressEvent(phaseLabel, samplePrefix, degree,
                    workerCounts[w], workerTotals[w], w));
        }
    }

    /**
     * Runs {@code task} once per index in {@code [0, total)}, {@code x} being the binary vector
     * for that index (bit {@code b} of {@code index} = {@code x[b]}), against per-thread sandbox
     * workers. Used by {@code ExactnessChecker}'s exhaustive {@code 2^n} check, which needs both
     * cost and penalty per point rather than one batched {@link EvalKind}. Same threshold-gated
     * pooling and calling-thread progress-polling as {@link #evaluate}.
     */
    public <R> List<R> evaluateIndexed(int n, long total, IndexTask<R> task,
                                        String phaseLabel, Consumer<String> progress) throws Exception {
        if (total <= 0) return Collections.emptyList();
        int totalInt = Math.toIntExact(total);
        @SuppressWarnings("unchecked")
        R[] results = (R[]) new Object[totalInt];

        dispatchChunked(totalInt, phaseLabel + ", " + totalInt + " indices",
                (worker, start, end, tick) -> {
                    for (int i = start; i < end; i++) {
                        results[i] = task.run(worker, i, toVector(n, i));
                        tick.run();
                    }
                },
                (counts, totals) -> reportIndexed(progress, phaseLabel, sum(counts), totalInt));

        return Arrays.asList(results);
    }

    private static int[] toVector(int n, long index) {
        int[] x = new int[n];
        for (int b = 0; b < n; b++) x[b] = (int) ((index >> b) & 1);
        return x;
    }

    private static void reportIndexed(Consumer<String> progress, String phaseLabel, long count, long total) {
        ProgressEvent.report(progress, phaseLabel + ": " + count + "/" + total + "…");
    }

    private static boolean allDone(List<Future<?>> futures) {
        for (Future<?> f : futures) if (!f.isDone()) return false;
        return true;
    }

    private static void report(Consumer<String> progress, Consumer<ProgressEvent> structuredProgress,
                                String samplePrefix, int degree, int count, int total) {
        ProgressEvent.report(progress, "Sampling: " + samplePrefix + " degree " + degree
                + " (" + count + "/" + total + ")...");
        if (structuredProgress != null) {
            structuredProgress.accept(new ProgressEvent(
                    "Sampling " + samplePrefix + " degree " + degree, samplePrefix, degree, count, total));
        }
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
