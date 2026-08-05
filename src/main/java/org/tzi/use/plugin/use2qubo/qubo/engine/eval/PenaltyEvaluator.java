package org.tzi.use.plugin.use2qubo.qubo.engine.eval;

import org.tzi.use.plugin.use2qubo.qubo.context.QuboContext;
import org.tzi.use.plugin.use2qubo.qubo.engine.index.DVPair;
import org.tzi.use.plugin.use2qubo.util.PluginLog;
import org.tzi.use.uml.mm.MClassInvariant;
import org.tzi.use.uml.ocl.expr.Evaluator;
import org.tzi.use.uml.ocl.expr.Expression;
import org.tzi.use.uml.ocl.value.BooleanValue;
import org.tzi.use.uml.ocl.value.ObjectValue;
import org.tzi.use.uml.ocl.value.Value;
import org.tzi.use.uml.ocl.value.VarBindings;
import org.tzi.use.uml.sys.MObject;
import org.tzi.use.uml.sys.MSystemState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Utility class for evaluating penalties (constraint violations) during QUBO sampling.
 * Manages invariant tasks, batching, and parallel evaluation of OCL constraints.
 */
public final class PenaltyEvaluator {

    private PenaltyEvaluator() {}

    /** One (invariant, instance) penalty check; the flattened unit of work for computePenalty. */
    public record PenaltyTask(Expression bodyExpr, MObject obj) {}

    /** Flattens ctx.invariants x ctx.objectsByClass into a fixed task list, built once per
     *  derivation (ctx doesn't change across samples) rather than rebuilt on every evalPenalty call. */
    public static List<PenaltyTask> buildPenaltyTasks(QuboContext ctx) {
        return buildPenaltyTasks(ctx.invariants, ctx.objectsByClass);
    }

    /** Same flattening, taking the invariants/objects explicitly so a {@code SandboxWorker} can
     *  build its own task list against its sandbox's remapped {@code MObject}s instead of the
     *  live ctx's — {@code MClassInvariant} is a model-level construct (safe to share across the
     *  live system and every sandbox clone), only the per-object task binding must be sandbox-local. */
    public static List<PenaltyTask> buildPenaltyTasks(List<MClassInvariant> invariants,
                                                        Map<String, List<MObject>> objectsByClass) {
        List<PenaltyTask> tasks = new ArrayList<>();
        for (MClassInvariant inv : invariants) {
            List<MObject> objs = objectsByClass.getOrDefault(inv.cls().name(), Collections.emptyList());
            for (MObject obj : objs) {
                tasks.add(new PenaltyTask(inv.bodyExpression(), obj));
            }
        }
        return tasks;
    }

    /** Below this task count, dispatch overhead outweighs any parallel speedup. */
    private static final int PARALLEL_PENALTY_THRESHOLD = 16;

    /** Evaluator is not thread-safe (mutable eval-context field) — one per worker thread. */
    private static final ThreadLocal<Evaluator> PENALTY_EVALUATOR = ThreadLocal.withInitial(Evaluator::new);

    public static double computePenalty(List<PenaltyTask> tasks, MSystemState state) {
        Stream<PenaltyTask> stream = tasks.size() >= PARALLEL_PENALTY_THRESHOLD
                ? tasks.parallelStream() : tasks.stream();
        return stream.mapToDouble(t -> evalOneInvariant(t, state)).sum();
    }

    public static double evalOneInvariant(PenaltyTask t, MSystemState state) {
        Evaluator evaluator = PENALTY_EVALUATOR.get();
        VarBindings bindings = new VarBindings();
        bindings.push("self", new ObjectValue(t.obj().cls(), t.obj()));
        Value result;
        try {
            result = evaluator.eval(t.bodyExpr(), state, bindings);
        } catch (Exception e) {
            synchronized (PluginLog.class) {
                PluginLog.debug("Invariant eval failed for " + t.obj().name() + ": " + e.getMessage());
            }
            result = null;
        }
        boolean holds = (result instanceof BooleanValue) && ((BooleanValue) result).isTrue();
        return holds ? 0.0 : 1.0;
    }

    /**
     * Evaluates the penalty for binary vector x (no objective).
     * State must have no decision-var links before this call; restored after.
     */
    public static double evalPenalty(int[] x, List<DVPair> flatVars,
                                  QuboContext ctx, List<PenaltyTask> penaltyTasks) throws Exception {
        return DecisionLinkSampler.withTemporaryLinks(x, flatVars, ctx, () -> computePenalty(penaltyTasks, ctx.state));
    }
}
