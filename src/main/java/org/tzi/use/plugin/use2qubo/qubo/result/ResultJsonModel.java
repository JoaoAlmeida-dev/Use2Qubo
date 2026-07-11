package org.tzi.use.plugin.use2qubo.qubo.result;

import java.util.List;
import java.util.Map;

/**
 * Gson serialisation target for {@code qubo.json}. Field names/order match the emitted
 * JSON verbatim so no custom serializer is needed.
 */
class ResultJsonModel {
    int nVars;
    int nSamples;
    boolean exact;
    double constant;
    int polyDegree;
    int nAncillaVars;
    double quadratizationPenalty;
    String exactnessMethod;
    int exactnessMatchCount;
    int exactnessTotalCount;
    Map<Integer, Double> linear;
    Map<String, Double> quadratic;
    List<String> varLabels;
}
