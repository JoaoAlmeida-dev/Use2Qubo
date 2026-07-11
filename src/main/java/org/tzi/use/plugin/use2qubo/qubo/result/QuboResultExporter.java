package org.tzi.use.plugin.use2qubo.qubo.result;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.tzi.use.plugin.use2qubo.util.PluginLog;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Serialises a {@link QuboResult} to the {@code qubo.json} export format (nVars, nSamples,
 * exact, constant, polyDegree, ancilla count/penalty, sparse {@code linear}/{@code quadratic}
 * coefficient maps, and {@code varLabels}) via Gson. This is the sole writer of {@code
 * qubo.json}; both the "Derive QUBO Matrix" GUI action and {@link
 * org.tzi.use.plugin.use2qubo.cli.QuboCli} funnel through it so headless and interactive runs
 * produce byte-identical output for the same input.
 */
public class QuboResultExporter {

    private static final Gson PRETTY_GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Writes {@code result} as JSON to {@code outputFile}, overwriting any existing content. */
    public static void write(QuboResult result, File outputFile) throws IOException {
        ResultJsonModel model = new ResultJsonModel();
        model.nVars = result.nVars;
        model.nSamples = result.nSamples;
        model.exact = result.exact;
        model.constant = result.constant;
        model.polyDegree = result.polyDegree;
        model.nAncillaVars = result.nAncillaVars;
        model.quadratizationPenalty = result.quadratizationPenalty;
        model.exactnessMethod = result.exactnessMethod;
        model.exactnessMatchCount = result.exactnessMatchCount;
        model.exactnessTotalCount = result.exactnessTotalCount;
        model.linear = result.linear;
        model.quadratic = result.quadratic;
        model.varLabels = result.varLabels;

        byte[] bytes = PRETTY_GSON.toJson(model).getBytes(StandardCharsets.UTF_8);
        Files.write(outputFile.toPath(), bytes);
        PluginLog.info("QUBO result written: " + outputFile.getAbsolutePath()
                + " (" + bytes.length + " bytes, nVars=" + result.nVars
                + ", |linear|=" + result.linear.size()
                + ", |quadratic|=" + result.quadratic.size() + ")");
    }
}
