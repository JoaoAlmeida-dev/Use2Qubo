package org.tzi.use.plugin.use2qubo.qubo;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Parses qubo_config.json for QUBO pipeline use.
 */
public class QuboConfig {

    private static final Gson GSON = new Gson();

    /** Association names that are decision variables (excluded from fixedLinks). */
    public final Set<String> decisionVarAssocs;

    /** Ordered list of decision-var entries: [type, association, classA, classB]. */
    public final List<String[]> dvEntries;

    /** OCL expression string from objective.expression. */
    public final String objectiveExpr;

    /** True = minimise the objective. */
    public final boolean minimise;

    private QuboConfig(Set<String> decisionVarAssocs,
                       List<String[]> dvEntries,
                       String objectiveExpr,
                       boolean minimise) {
        this.decisionVarAssocs = decisionVarAssocs;
        this.dvEntries         = dvEntries;
        this.objectiveExpr     = objectiveExpr;
        this.minimise          = minimise;
    }

    boolean isDecisionVar(String assocName) {
        return decisionVarAssocs.contains(assocName);
    }

    // ------------------------------------------------------------------
    // Parsing
    // ------------------------------------------------------------------

    public static QuboConfig parse(String json) {
        ExportConfigModel model = GSON.fromJson(json, ExportConfigModel.class);

        Set<String> assocs = model.decision_var_associations == null
                ? Collections.emptySet()
                : new LinkedHashSet<>(model.decision_var_associations);

        List<String[]> dvEntries = new ArrayList<>();
        if (model.decision_vars != null) {
            for (ExportConfigModel.DvEntry e : model.decision_vars) {
                List<String> domain = e.domain == null ? Collections.emptyList() : e.domain;
                String classA = domain.size() > 0 ? domain.get(0) : "";
                String classB = domain.size() > 1 ? domain.get(1) : "";
                dvEntries.add(new String[]{e.type, e.association, classA, classB});
            }
        }

        String expr = model.objective == null ? null : model.objective.expression;
        boolean minimise = model.objective == null || model.objective.minimise;

        return new QuboConfig(assocs, dvEntries, expr, minimise);
    }
}
