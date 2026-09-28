package org.tzi.use.plugin.use2qubo.qubo.engine.eval;

import org.tzi.use.parser.ocl.OCLCompiler;
import org.tzi.use.plugin.use2qubo.qubo.context.QuboContext;
import org.tzi.use.plugin.use2qubo.qubo.engine.ProgressEvent;
import org.tzi.use.plugin.use2qubo.qubo.engine.index.DVPair;
import org.tzi.use.plugin.use2qubo.util.PluginLog;
import org.tzi.use.uml.ocl.expr.Evaluator;
import org.tzi.use.uml.ocl.expr.Expression;
import org.tzi.use.uml.ocl.value.IntegerValue;
import org.tzi.use.uml.ocl.value.RealValue;
import org.tzi.use.uml.ocl.value.Value;
import org.tzi.use.uml.ocl.value.VarBindings;
import org.tzi.use.uml.sys.MSystemState;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.function.Consumer;

/**
 * Utility class for compiling and evaluating objective functions in QUBO formulation.
 */
public final class ObjectiveEvaluator {

    private ObjectiveEvaluator() {}

    public static Expression compileObjective(QuboContext ctx, Consumer<String> progress) {
        return compileObjective(ctx, progress, null);
    }

    public static Expression compileObjective(QuboContext ctx, Consumer<String> progress,
                                        Consumer<ProgressEvent> structuredProgress) {
        ProgressEvent.reportPhase(progress, structuredProgress, "Compiling objective OCL…");
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

    /**
     * Evaluates the objective cost for binary vector x (no penalty).
     * State must have no decision-var links before this call; restored after.
     */
    public static double evalCost(int[] x, List<DVPair> flatVars,
                           QuboContext ctx, Evaluator evaluator,
                           Expression objExpr) throws Exception {
        return DecisionLinkSampler.withTemporaryLinks(x, flatVars, ctx, () -> {
            double obj = computeObjective(objExpr, evaluator, ctx.state);
            return ctx.minimise ? obj : -obj;
        });
    }

    public static double computeObjective(Expression objExpr, Evaluator evaluator,
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
}
