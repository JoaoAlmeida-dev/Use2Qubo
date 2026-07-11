package org.tzi.use.plugin.use2qubo.qubo.context;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.tzi.use.plugin.use2qubo.qubo.config.QuboConfig;
import org.tzi.use.plugin.use2qubo.qubo.config.QuboConfigPaths;
import org.tzi.use.plugin.use2qubo.util.PluginLog;
import org.tzi.use.uml.mm.MAssociation;
import org.tzi.use.uml.mm.MClassInvariant;
import org.tzi.use.uml.mm.MModel;
import org.tzi.use.uml.sys.MLink;
import org.tzi.use.uml.sys.MLinkSet;
import org.tzi.use.uml.sys.MObject;
import org.tzi.use.uml.sys.MSystem;
import org.tzi.use.uml.sys.MSystemState;

/**
 * Assembles a {@link QuboContext} for a live {@link MSystem}: locates and parses
 * {@code qubo_config.json} ({@link QuboConfig}), snapshots objects grouped by class,
 * separates fixed links from decision-variable associations, and computes the total
 * binary variable count {@code nVars}. Every list produced here is sorted (by object/association
 * name) so {@link QuboContext#varIndex} and {@link org.tzi.use.plugin.use2qubo.qubo.engine.QuboEngine}'s
 * flat variable ordering are deterministic across runs of the same model/state.
 */
public class QuboContextBuilder {

    /** Resolves {@code qubo_config.json} next to the loaded model, then delegates to {@link #build(MSystem, Path)}. */
    public static QuboContext build(MSystem system) throws IOException {
        File configFile = QuboConfigPaths.resolveConfigFile(system);
        PluginLog.info("Resolved config path: " + configFile);
        return build(system, configFile.toPath());
    }

    public static QuboContext build(MSystem system, Path configPath) throws IOException {
        PluginLog.info("Loading qubo_config.json from: " + configPath);
        String raw = new String(Files.readAllBytes(configPath), StandardCharsets.UTF_8);
        QuboConfig config = QuboConfig.parse(raw);
        PluginLog.info("Config parsed: " + config.decisionVarEntries.size()
                + " decision-var entries, objective minimise=" + config.minimise);

        MModel model = system.model();
        MSystemState liveState = system.state();

        List<MClassInvariant> invariants = new ArrayList<>(model.classInvariants());
        PluginLog.info("Model invariants: " + invariants.size());

        Map<String, List<MObject>> liveObjectsByClass = buildObjectsByClass(liveState);
        PluginLog.debug("Objects by class: " + liveObjectsByClass.keySet());

        Map<String, List<MLink>> liveFixedLinks = buildFixedLinks(model, liveState, config);
        PluginLog.debug("Fixed-link associations: " + liveFixedLinks.size());

        SandboxSystemFactory.Sandbox sandbox;
        try {
            sandbox = SandboxSystemFactory.build(model, liveState, liveObjectsByClass, true, liveFixedLinks);
        } catch (Exception e) {
            throw new IOException("Failed to build sandbox system for derivation", e);
        }
        PluginLog.info("Derivation isolated on sandbox system (live model will not be mutated)");

        Map<String, List<MObject>> objectsByClass = buildObjectsByClass(sandbox.state);
        Map<String, List<MLink>> fixedLinks = buildFixedLinks(model, sandbox.state, config);

        List<DecisionVar> decisionVars = buildDecisionVars(config, objectsByClass);

        int nVars = computeNVars(decisionVars, objectsByClass);
        PluginLog.info("Context ready: nVars=" + nVars + ", decisionVars=" + decisionVars.size());

        return new QuboContext(sandbox.system, model, sandbox.state, invariants, objectsByClass,
                fixedLinks, decisionVars, nVars, config.objectiveExpr, config.minimise, config.maxDegree);
    }

    // ------------------------------------------------------------------

    private static Map<String, List<MObject>> buildObjectsByClass(MSystemState state) {
        Map<String, List<MObject>> map = new LinkedHashMap<>();
        for (MObject obj : state.allObjects()) {
            map.computeIfAbsent(obj.cls().name(), k -> new ArrayList<>()).add(obj);
        }
        for (List<MObject> list : map.values()) {
            list.sort(Comparator.comparing(MObject::name));
        }
        return map;
    }

    private static Map<String, List<MLink>> buildFixedLinks(MModel model,
                                                             MSystemState state,
                                                             QuboConfig config) {
        Map<String, List<MLink>> map = new LinkedHashMap<>();
        for (MAssociation assoc : sortedAssocs(model.associations())) {
            if (config.isDecisionVar(assoc.name())) continue;
            MLinkSet linkSet = state.linksOfAssociation(assoc);
            if (linkSet.size() == 0) continue;
            List<MLink> sorted = linkSet.links().stream()
                    .sorted(Comparator.comparing(l ->
                            l.linkedObjects().stream()
                                    .map(MObject::name)
                                    .collect(Collectors.joining(","))))
                    .collect(Collectors.toList());
            map.put(assoc.name(), sorted);
        }
        return map;
    }

    private static List<DecisionVar> buildDecisionVars(QuboConfig config,
                                                        Map<String, List<MObject>> objectsByClass) {
        List<DecisionVar> result = new ArrayList<>();
        for (String[] entry : config.decisionVarEntries) {
            String type    = entry[0];
            String assoc   = entry[1];
            String classA  = entry[2];
            String classB  = entry[3];
            List<MObject> domain = objectsByClass.getOrDefault(classA, Collections.emptyList());
            result.add(new DecisionVar(type, assoc, classA, classB, domain));
        }
        return result;
    }

    private static int computeNVars(List<DecisionVar> decisionVars,
                                     Map<String, List<MObject>> objectsByClass) {
        int total = 0;
        for (DecisionVar dv : decisionVars) {
            int bCount = objectsByClass.getOrDefault(dv.classB, Collections.emptyList()).size();
            total += dv.domain.size() * bCount;
        }
        return total;
    }

    private static List<MAssociation> sortedAssocs(Collection<MAssociation> assocs) {
        return assocs.stream()
                .sorted(Comparator.comparing(MAssociation::name))
                .collect(Collectors.toList());
    }
}
