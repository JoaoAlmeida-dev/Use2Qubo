package org.tzi.use.plugin.use2qubo.qubo.config;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.tzi.use.plugin.use2qubo.util.QuboConstants;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Parses qubo_config.json for QUBO pipeline use.
 */
public class QuboConfig {

    private static final Gson GSON = new Gson();
    private static final Gson PRETTY_GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Association names that are decision variables (excluded from fixedLinks). */
    public final Set<String> decisionVarAssocs;

    /** Ordered list of decision-var entries: [type, association, classA, classB]. */
    public final List<String[]> decisionVarEntries;

    /** OCL expression string from objective.expression. */
    public final String objectiveExpr;

    /** True = minimise the objective. */
    public final boolean minimise;

    /** Cap on pseudo-Boolean polynomial degree explored before quadratization (see {@code max_degree}). */
    public final int maxDegree;

    private QuboConfig(Set<String> decisionVarAssocs,
                       List<String[]> decisionVarEntries,
                       String objectiveExpr,
                       boolean minimise,
                       int maxDegree) {
        this.decisionVarAssocs  = decisionVarAssocs;
        this.decisionVarEntries = decisionVarEntries;
        this.objectiveExpr      = objectiveExpr;
        this.minimise           = minimise;
        this.maxDegree          = maxDegree;
    }

    public boolean isDecisionVar(String assocName) {
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
        int maxDegree = (model.objective != null && model.objective.max_degree != null)
                ? model.objective.max_degree
                : QuboConstants.DEFAULT_MAX_POLY_DEGREE;

        return new QuboConfig(assocs, dvEntries, expr, minimise, maxDegree);
    }

    // ------------------------------------------------------------------
    // Writing
    // ------------------------------------------------------------------

    /** Builds a {@code QuboConfig} from raw form state (e.g. the config editor UI). */
    public static QuboConfig of(Set<String> decisionVarAssocs,
                                List<String[]> decisionVarEntries,
                                String objectiveExpr,
                                boolean minimise,
                                int maxDegree) {
        return new QuboConfig(decisionVarAssocs, decisionVarEntries, objectiveExpr, minimise, maxDegree);
    }

    /** Serialises this config back to {@code qubo_config.json} format. */
    public String toJson() {
        ExportConfigModel model = new ExportConfigModel();
        model.decision_var_associations = new ArrayList<>(decisionVarAssocs);

        model.decision_vars = new ArrayList<>();
        for (String[] e : decisionVarEntries) {
            ExportConfigModel.DvEntry dv = new ExportConfigModel.DvEntry();
            dv.type = e[0];
            dv.association = e[1];
            dv.domain = List.of(e[2], e[3]);
            model.decision_vars.add(dv);
        }

        model.objective = new ExportConfigModel.Objective();
        model.objective.expression = objectiveExpr;
        model.objective.minimise = minimise;
        model.objective.max_degree = maxDegree;

        return PRETTY_GSON.toJson(model);
    }

    /** Writes this config's JSON representation to {@code file}, overwriting any existing content. */
    public void writeTo(File file) throws IOException {
        Files.write(file.toPath(), toJson().getBytes(StandardCharsets.UTF_8));
    }
}
