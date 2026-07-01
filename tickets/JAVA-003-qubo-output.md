# JAVA-003 — QUBO Output (`qubo.json`)

## Goal

Write the `QuboResult` to a `qubo.json` file. This is the primary handoff artifact between the Java plugin and the Python submit script (PY-001). Also produces optional `to_latex` output for the paper.

## Output Format: `qubo.json`

```json
{
  "n_vars": 18,
  "n_samples": 172,
  "exact": true,
  "constant": 42.0,
  "var_labels": [
    "RouteVisits(route0,depot)",
    "RouteVisits(route0,n1)",
    "..."
  ],
  "linear": {
    "0": 5.0,
    "1": -3.5
  },
  "quadratic": {
    "0,1": -1.5,
    "0,3": 2.0
  }
}
```

- `linear` keys: variable index as string
- `quadratic` keys: `"i,j"` with `i < j`
- Zero coefficients omitted (threshold 1e-12)
- `var_labels` ordered by variable index; consumer uses these to map back to decision variables

## Implementation

```java
public class QuboOutputWriter {
    public static void write(QuboResult result, File outputFile) throws IOException {
        SimpleJsonWriter w = new SimpleJsonWriter(outputFile);
        w.objectOpen();
        w.keyValue("n_vars",    result.nVars);
        w.keyValue("n_samples", result.nSamples);
        w.keyValue("exact",     result.exact);
        w.keyValue("constant",  result.constant);
        // var_labels array
        w.key("var_labels"); w.arrayOpen();
        for (String lbl : result.varLabels) w.stringValue(lbl);
        w.arrayClose();
        // linear
        w.key("linear"); w.objectOpen();
        for (Map.Entry<Integer,Double> e : result.linear.entrySet())
            w.keyValue(String.valueOf(e.getKey()), e.getValue());
        w.objectClose();
        // quadratic
        w.key("quadratic"); w.objectOpen();
        for (Map.Entry<int[],Double> e : result.quadratic.entrySet())
            w.keyValue(e.getKey()[0] + "," + e.getKey()[1], e.getValue());
        w.objectClose();
        w.objectClose();
    }
}
```

Reuse existing `SimpleJsonWriter` where possible; extend as needed.

## LaTeX Output (optional, for paper)

```java
public static String toLaTeX(QuboResult result, String style) {
    // style "indexed": H = c + c_i x_{i} + c_{ij} x_{i} x_{j} + ...
    // Sorted by |coeff| descending
    // Appended legend: % x_0 = RouteVisits(route0,depot)
}
```

Same contract as TICKET-006 `to_latex`. Produce this on request via a separate "Export LaTeX" action or a flag on the "Derive QUBO" action.

## Acceptance Criteria

```java
QuboResult r = QuboEngine.derive(ctx);
File out = new File("/tmp/test_qubo.json");
QuboOutputWriter.write(r, out);

// Read back and verify
JSONObject json = new JSONObject(Files.readString(out.toPath()));
assert json.getInt("n_vars") == 18;
assert json.getJSONArray("var_labels").length() == 18;
assert json.getBoolean("exact") == r.exact;
// quadratic keys are "i,j" strings
json.getJSONObject("quadratic").keys().forEachRemaining(k ->
    assert k.matches("\\d+,\\d+")
);
```

## Dependencies

JAVA-002 (`QuboResult`). Extends existing `SimpleJsonWriter`.
