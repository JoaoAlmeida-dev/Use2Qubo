package org.tzi.use.plugin.use2qubo.util;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Minimal JSON serializer — no external dependencies.
 * Supports the exact structure needed for ocl2qubo instance.json.
 */
public class SimpleJsonWriter {

    private final StringBuilder sb = new StringBuilder();
    private int indent = 0;
    private static final String INDENT_UNIT = "  ";

    // ---------------------------------------------------------------
    // Public entry point
    // ---------------------------------------------------------------

    public String toString() {
        return sb.toString();
    }

    // ---------------------------------------------------------------
    // Structural builders (return this for chaining)
    // ---------------------------------------------------------------

    public SimpleJsonWriter objectOpen() {
        sb.append("{\n");
        indent++;
        return this;
    }

    public SimpleJsonWriter objectClose(boolean trailingComma) {
        indent--;
        pad();
        sb.append("}");
        if (trailingComma) sb.append(",");
        sb.append("\n");
        return this;
    }

    public SimpleJsonWriter arrayOpen() {
        sb.append("[\n");
        indent++;
        return this;
    }

    public SimpleJsonWriter arrayClose(boolean trailingComma) {
        indent--;
        pad();
        sb.append("]");
        if (trailingComma) sb.append(",");
        sb.append("\n");
        return this;
    }

    // ---------------------------------------------------------------
    // Key-value pairs
    // ---------------------------------------------------------------

    /** Write a key followed by a nested object (caller handles the object body). */
    public SimpleJsonWriter key(String key) {
        pad();
        sb.append(quoted(key)).append(": ");
        return this;
    }

    /** Write a complete key: value line. */
    public SimpleJsonWriter keyValue(String key, Object value, boolean trailingComma) {
        pad();
        sb.append(quoted(key)).append(": ").append(renderValue(value));
        if (trailingComma) sb.append(",");
        sb.append("\n");
        return this;
    }

    // ---------------------------------------------------------------
    // Array items
    // ---------------------------------------------------------------

    /** Write a plain string item in an array. */
    public SimpleJsonWriter arrayItem(String value, boolean trailingComma) {
        pad();
        sb.append(quoted(value));
        if (trailingComma) sb.append(",");
        sb.append("\n");
        return this;
    }

    /** Write a raw (already-serialized) item in an array. */
    public SimpleJsonWriter arrayItemRaw(String raw, boolean trailingComma) {
        pad();
        sb.append(raw);
        if (trailingComma) sb.append(",");
        sb.append("\n");
        return this;
    }

    // ---------------------------------------------------------------
    // High-level helpers for common patterns
    // ---------------------------------------------------------------

    /**
     * Write a complete "key": "obj1,obj2" array from a list of link strings.
     * Used for plain (non-association-class) links.
     */
    public SimpleJsonWriter linkArray(String key, List<String> items, boolean trailingComma) {
        pad();
        sb.append(quoted(key)).append(": [");
        if (items.isEmpty()) {
            sb.append("]");
        } else if (items.size() == 1) {
            sb.append(quoted(items.get(0))).append("]");
        } else {
            sb.append("\n");
            indent++;
            for (int i = 0; i < items.size(); i++) {
                pad();
                sb.append(quoted(items.get(i)));
                if (i < items.size() - 1) sb.append(",");
                sb.append("\n");
            }
            indent--;
            pad();
            sb.append("]");
        }
        if (trailingComma) sb.append(",");
        sb.append("\n");
        return this;
    }

    /**
     * Write a "key": [ { "objects": [...], "attr": val, ... }, ... ] array.
     * Used for association-class links.
     *
     * @param key        association name
     * @param linkMaps   each entry: "objects" -> List<String>, plus attribute entries
     * @param trailingComma whether to append a comma after the closing ]
     */
    public SimpleJsonWriter assocClassLinkArray(
            String key,
            List<Map<String, Object>> linkMaps,
            boolean trailingComma) {

        pad();
        sb.append(quoted(key)).append(": [\n");
        indent++;
        for (int i = 0; i < linkMaps.size(); i++) {
            Map<String, Object> map = linkMaps.get(i);
            pad();
            sb.append("{\n");
            indent++;

            // "objects" first
            @SuppressWarnings("unchecked")
            List<String> objects = (List<String>) map.get("objects");
            pad();
            sb.append(quoted("objects")).append(": [");
            for (int j = 0; j < objects.size(); j++) {
                sb.append(quoted(objects.get(j)));
                if (j < objects.size() - 1) sb.append(", ");
            }
            sb.append("]");
            if (map.size() > 1) sb.append(",");
            sb.append("\n");

            // remaining attributes (preserving insertion order)
            int attrCount = 0;
            int totalAttrs = map.size() - 1;
            for (Map.Entry<String, Object> e : map.entrySet()) {
                if (e.getKey().equals("objects")) continue;
                pad();
                sb.append(quoted(e.getKey())).append(": ").append(renderValue(e.getValue()));
                attrCount++;
                if (attrCount < totalAttrs) sb.append(",");
                sb.append("\n");
            }

            indent--;
            pad();
            sb.append("}");
            if (i < linkMaps.size() - 1) sb.append(",");
            sb.append("\n");
        }
        indent--;
        pad();
        sb.append("]");
        if (trailingComma) sb.append(",");
        sb.append("\n");
        return this;
    }

    /**
     * Inject a pre-serialized JSON fragment (e.g. a verbatim block from a config file).
     * The fragment is indented at the current level.
     */
    public SimpleJsonWriter rawFragment(String raw, boolean trailingComma) {
        String[] lines = raw.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            if (i == 0) {
                pad();
                sb.append(lines[i]);
            } else {
                sb.append("\n");
                if (!lines[i].trim().isEmpty()) {
                    pad();
                }
                sb.append(lines[i]);
            }
        }
        if (trailingComma) sb.append(",");
        sb.append("\n");
        return this;
    }

    // ---------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------

    private void pad() {
        for (int i = 0; i < indent; i++) sb.append(INDENT_UNIT);
    }

    private static String quoted(String s) {
        return "\"" + escape(s) + "\"";
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    static String renderValue(Object v) {
        if (v == null) return "null";
        if (v instanceof Boolean) return v.toString();
        if (v instanceof Integer || v instanceof Long) return v.toString();
        if (v instanceof Double) {
            double d = (Double) v;
            if (d == Math.floor(d) && !Double.isInfinite(d)) {
                return String.format(Locale.ROOT, "%.1f", d);
            }
            return String.format(Locale.ROOT, "%s", Double.toString(d));
        }
        if (v instanceof Number) return v.toString();
        return quoted(v.toString());
    }
}
