package org.tzi.use.plugin.use2qubo.qubo.result;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

import org.tzi.use.plugin.use2qubo.util.PluginLog;
import org.tzi.use.plugin.use2qubo.util.SimpleJsonWriter;

/**
 * Serialises a {@link QuboResult} to the {@code qubo.json} export format (nVars, nSamples,
 * exact, constant, polyDegree, ancilla count/penalty, sparse {@code linear}/{@code quadratic}
 * coefficient maps, and {@code varLabels}) via {@link SimpleJsonWriter}. This is the sole writer
 * of {@code qubo.json}; both the "Derive QUBO Matrix" GUI action and {@link
 * org.tzi.use.plugin.use2qubo.cli.QuboCli} funnel through it so headless and interactive runs
 * produce byte-identical output for the same input.
 */
public class QuboResultExporter {

    /** Writes {@code result} as JSON to {@code outputFile}, overwriting any existing content. */
    public static void write(QuboResult result, File outputFile) throws IOException {
        SimpleJsonWriter w = new SimpleJsonWriter();
        w.objectOpen();
        w.keyValue("nVars",       result.nVars,       true);
        w.keyValue("nSamples",    result.nSamples,    true);
        w.keyValue("exact",       result.exact,       true);
        w.keyValue("constant",    result.constant,    true);
        w.keyValue("polyDegree",  result.polyDegree,  true);
        w.keyValue("nAncillaVars", result.nAncillaVars, true);
        w.keyValue("quadratizationPenalty", result.quadratizationPenalty, true);

        w.key("linear").objectOpen();
        writeIntDoubleMap(w, result.linear);
        w.objectClose(true);

        w.key("quadratic").objectOpen();
        writeStringDoubleMap(w, result.quadratic);
        w.objectClose(true);

        w.linkArray("varLabels", result.varLabels, false);
        w.objectClose(false);

        byte[] bytes = w.toString().getBytes(StandardCharsets.UTF_8);
        Files.write(outputFile.toPath(), bytes);
        PluginLog.info("QUBO result written: " + outputFile.getAbsolutePath()
                + " (" + bytes.length + " bytes, nVars=" + result.nVars
                + ", |linear|=" + result.linear.size()
                + ", |quadratic|=" + result.quadratic.size() + ")");
    }

    private static void writeIntDoubleMap(SimpleJsonWriter w, Map<Integer, Double> map) {
        int count = 0;
        int total = map.size();
        for (Map.Entry<Integer, Double> e : map.entrySet()) {
            count++;
            w.keyValue(String.valueOf(e.getKey()), e.getValue(), count < total);
        }
    }

    private static void writeStringDoubleMap(SimpleJsonWriter w, Map<String, Double> map) {
        int count = 0;
        int total = map.size();
        for (Map.Entry<String, Double> e : map.entrySet()) {
            count++;
            w.keyValue(e.getKey(), e.getValue(), count < total);
        }
    }
}
