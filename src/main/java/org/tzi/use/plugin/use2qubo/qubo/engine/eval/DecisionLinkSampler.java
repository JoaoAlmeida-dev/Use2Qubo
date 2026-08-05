package org.tzi.use.plugin.use2qubo.qubo.engine.eval;

import org.tzi.use.plugin.use2qubo.qubo.context.DecisionVar;
import org.tzi.use.plugin.use2qubo.qubo.context.QuboContext;
import org.tzi.use.plugin.use2qubo.qubo.engine.index.DVPair;
import org.tzi.use.plugin.use2qubo.util.PluginLog;
import org.tzi.use.uml.mm.MAssociation;
import org.tzi.use.uml.sys.MLink;
import org.tzi.use.uml.sys.MSystemState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility class for managing temporary decision-variable links during QUBO sampling.
 * Handles insertion, stripping, and restoration of links representing binary variable assignments.
 */
public final class DecisionLinkSampler {

    private DecisionLinkSampler() {}

    public interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    /**
     * Captures all existing decision-var links, then strips them from state so sampling starts from a clean slate.
     */
    public static Map<String, Set<MLink>> saveAndStripLinks(QuboContext ctx) {
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

    /**
     * Strips all decision-var links from state without saving them.
     * Used before sampling passes and before held-out exactness evaluation.
     */
    public static void stripDecisionLinks(QuboContext ctx) {
        for (DecisionVar dv : ctx.decisionVars) {
            MAssociation assoc = ctx.model.getAssociation(dv.association);
            if (assoc == null) continue;
            for (MLink link : new HashSet<>(ctx.state.linksOfAssociation(assoc).links())) {
                try { ctx.state.deleteLink(link); }
                catch (Exception e) { PluginLog.warn("Failed to delete link during state restore: " + link, e); }
            }
        }
    }

    /**
     * Purges any lingering decision-var links from the state, then re-inserts
     * every link captured in {@code savedLinks}.
     */
    public static void restoreLinks(QuboContext ctx, Map<String, Set<MLink>> savedLinks) {
        stripDecisionLinks(ctx);
        for (DecisionVar dv : ctx.decisionVars) {
            Set<MLink> orig = savedLinks.get(dv.association);
            if (orig == null) continue;
            for (MLink l : orig) {
                ctx.state.insertLink(l);
            }
        }
    }

    /**
     * Inserts the links implied by binary vector {@code x}, runs {@code body},
     * then removes exactly the links it inserted (best-effort) — regardless of
     * whether {@code body} succeeds. Shared by evalCost and evalPenalty,
     * which previously duplicated this insert/evaluate/cleanup sequence.
     */
    public static <T> T withTemporaryLinks(int[] x, List<DVPair> flatVars, QuboContext ctx,
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
}
