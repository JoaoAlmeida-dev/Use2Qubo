# JAVA-004 — "Derive QUBO" Plugin Action

## Goal

Wire JAVA-001 + JAVA-002 + JAVA-003 into a Swing plugin action. User clicks "Plugins → Derive QUBO", picks an output path, and gets `qubo.json`. Mirrors the existing `use2quboAction` pattern.

## Scope

**In scope:**
- New `DeriveQuboAction` class implementing `IPluginActionDelegate`
- Menu registration in `use2quboPlugin` alongside existing "Export Instance"
- Guards: model loaded + non-empty system state + `qubo_config.json` present
- Progress dialog or status bar message during derivation (optional for POC)
- Error dialog on failure (invariant eval error, write error)

**Out of scope:**
- LaTeX export (future; add a separate action if needed)
- D-Wave submission (PY-001 handles this)
- Changing the existing "Export Instance" action

## Implementation Sketch

```java
public class DeriveQuboAction implements IPluginActionDelegate {

    @Override
    public void performAction(IPluginContext ctx) {
        MSystem system = ctx.getSystem();
        if (system == null || system.state().allObjects().isEmpty()) {
            JOptionPane.showMessageDialog(null,
                "No system state. Load a model and run a .cmd script first.");
            return;
        }

        // Locate qubo_config.json next to the .use file
        Path configPath = resolveConfig(system);
        if (configPath == null || !Files.exists(configPath)) {
            JOptionPane.showMessageDialog(null,
                "qubo_config.json not found next to .use file. " +
                "Add decision_vars and objective before deriving QUBO.");
            return;
        }

        // Output file chooser
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("qubo.json"));
        if (fc.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) return;
        File outFile = fc.getSelectedFile();

        try {
            QuboContext  qctx   = QuboContextBuilder.build(system, configPath);
            QuboResult   result = QuboEngine.derive(qctx);
            QuboOutputWriter.write(result, outFile);
            JOptionPane.showMessageDialog(null,
                String.format("QUBO written: n_vars=%d, n_terms=%d, exact=%b",
                    result.nVars,
                    result.linear.size() + result.quadratic.size(),
                    result.exact));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                "Error deriving QUBO: " + e.getMessage());
        }
    }
}
```

## Plugin Registration

```java
// In use2quboPlugin.java
@Override
public List<IPluginAction> getActions() {
    return List.of(
        new use2quboAction(),   // existing
        new DeriveQuboAction()        // new
    );
}
```

Consult USE plugin API for exact action registration method (may differ from above).

## Acceptance Criteria

1. "Derive QUBO" appears in Plugins menu alongside "Export Instance"
2. Without loaded model: error dialog shown, no crash
3. Without `qubo_config.json`: informative error dialog
4. With valid state: `qubo.json` written; dialog shows n_vars=18, exact=true for GarbageTruckRouting scenario
5. `qubo.json` passes JAVA-003 schema check

## Dependencies

JAVA-001, JAVA-002, JAVA-003.
