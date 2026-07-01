package org.tzi.use.plugin.use2qubo.qubo;

import java.util.List;

/**
 * Gson deserialisation target for export_config.json. Field names match the
 * JSON keys verbatim so no custom (de)serializer is needed.
 */
class ExportConfigModel {
    List<String> decision_var_associations;
    List<DvEntry> decision_vars;
    Objective objective;

    static class DvEntry {
        String type;
        String association;
        List<String> domain;
    }

    static class Objective {
        String expression;
        boolean minimise = true;
    }
}
