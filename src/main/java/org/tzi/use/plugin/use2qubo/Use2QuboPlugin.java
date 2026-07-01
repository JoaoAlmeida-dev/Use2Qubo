package org.tzi.use.plugin.use2qubo;

import java.util.logging.Logger;

import org.tzi.use.runtime.IPlugin;
import org.tzi.use.runtime.IPluginRuntime;

/**
 * Required plugin startup class. USE calls getPluginClass() on every registered
 * plugin (even those with no diagram extension), so a non-null IPlugin is needed
 * to avoid a NullPointerException in DiagramExtensionPoint.registerDiagramManipulators.
 */
public class Use2QuboPlugin implements IPlugin {

    private static final Logger LOG = Logger.getLogger(Use2QuboPlugin.class.getName());

    @Override
    public String getName() {
        return "Use 2 Qubo Plugin";
    }

    @Override
    public void run(IPluginRuntime pluginRuntime) throws Exception {
        LOG.info("Use 2 Qubo Plugin started");
    }
}
