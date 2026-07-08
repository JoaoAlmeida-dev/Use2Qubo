package org.tzi.use.plugin.use2qubo.qubo.config;

import org.tzi.use.config.Options;
import org.tzi.use.uml.sys.MSystem;

import java.io.File;
import java.io.IOException;

/**
 * Locates {@code qubo_config.json} for the currently loaded model: the file is expected
 * alongside the {@code .use} model file, i.e. {@code <model-dir>/qubo_config.json}. The
 * model path is read from {@link MModel#filename()} first, falling back to
 * {@link Options#specFilename} (set when USE compiles the spec but before an {@code MSystem}
 * wraps it).
 */
public final class QuboConfigPaths {

    private QuboConfigPaths() {}

    /** @throws IOException if no {@code .use} file is loaded, so the config directory is unknown */
    public static File resolveConfigFile(MSystem system) throws IOException {
        String specFile = system.model().filename();
        if (specFile == null || specFile.isEmpty()) specFile = Options.specFilename;
        if (specFile == null || specFile.isEmpty())
            throw new IOException("No .use file loaded; cannot locate qubo_config.json");
        return new File(new File(specFile).getParentFile(), "qubo_config.json");
    }
}
