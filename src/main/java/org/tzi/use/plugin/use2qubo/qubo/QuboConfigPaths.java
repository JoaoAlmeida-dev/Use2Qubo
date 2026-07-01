package org.tzi.use.plugin.use2qubo.qubo;

import org.tzi.use.config.Options;
import org.tzi.use.uml.sys.MSystem;

import java.io.File;
import java.io.IOException;

public final class QuboConfigPaths {

    private QuboConfigPaths() {}

    public static File resolveConfigFile(MSystem system) throws IOException {
        String specFile = system.model().filename();
        if (specFile == null || specFile.isEmpty()) specFile = Options.specFilename;
        if (specFile == null || specFile.isEmpty())
            throw new IOException("No .use file loaded; cannot locate export_config.json");
        return new File(new File(specFile).getParentFile(), "export_config.json");
    }
}
