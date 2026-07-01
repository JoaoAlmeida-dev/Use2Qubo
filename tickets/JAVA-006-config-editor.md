# JAVA-006 — QUBO Config Editor Dialog

## Goal

Add a "Edit QUBO Config" menu action that opens a form dialog for editing `export_config.json` fields. Eliminates the need to hand-edit JSON. Reads the existing file when present; creates it on save.

## Scope

Two new classes (`EditQuboConfigAction`, `QuboConfigDialog`) and one new helper (`QuboConfigPaths`). The helper also replaces the duplicated path-resolution logic currently in `QuboContextBuilder` and `SystemStateExporter`.

## New Class: `qubo/QuboConfigPaths.java`

Package-private static helper. Single method:

```java
static File resolveConfigFile(MSystem system) throws IOException {
    String specFile = system.model().filename();
    if (specFile == null || specFile.isEmpty()) specFile = Options.specFilename;
    if (specFile == null || specFile.isEmpty())
        throw new IOException("No .use file loaded; cannot locate export_config.json");
    return new File(new File(specFile).getParentFile(), "export_config.json");
}
```

### Migration

- `QuboContextBuilder.build(MSystem)` — delegate path resolution to `QuboConfigPaths.resolveConfigFile(system)`.
- `SystemStateExporter.loadConfig()` — replace the `Options.specFilename` null check with `QuboConfigPaths.resolveConfigFile(system)` (pass `system` field). Swallow `IOException` as warning (existing behaviour).

## New Class: `action/EditQuboConfigAction.java`

Implements `IPluginActionDelegate`.

```java
@Override
public void performAction(IPluginAction pluginAction) {
    File configFile;
    try {
        configFile = QuboConfigPaths.resolveConfigFile(pluginAction.getSession().system());
    } catch (IOException e) {
        JOptionPane.showMessageDialog(..., e.getMessage(), "Edit QUBO Config", WARNING_MESSAGE);
        return;
    }
    new QuboConfigDialog(pluginAction.getParent(), configFile).setVisible(true);
}

@Override
public boolean shouldBeEnabled(IPluginAction pluginAction) {
    return pluginAction.getSession().hasSystem();
}
```

## New Class: `ui/QuboConfigDialog.java`

Extends `JDialog`. Constructor: `QuboConfigDialog(Frame parent, File configFile)`. Modal.

### Layout (`BorderLayout`)

**Centre (`GridBagLayout` form panel)**:

| Field | Component |
|---|---|
| Objective OCL | `JTextField(50)` |
| Minimise | `JCheckBox` (ticked = minimise, unticked = maximise) |
| Decision-var associations | `JList<String>` with `DefaultListModel` + "Add" / "Remove" buttons |

The `JList` panel uses `BorderLayout`: list in a `JScrollPane` centre, button row south.

**Bottom panel (`FlowLayout`, right-aligned)**:
- `"Save"` — serialises form to `configFile` using `SimpleJsonWriter`, shows success/error dialog, does not close
- `"Close"` — `dispose()`

### Pre-fill on open

If `configFile.exists()`:
```java
String raw = new String(Files.readAllBytes(configFile.toPath()), UTF_8);
QuboConfig cfg = QuboConfig.parse(raw);
objectiveField.setText(cfg.objectiveExpr);
minimiseBox.setSelected(cfg.minimise);
cfg.dvEntries.forEach(e -> listModel.addElement(e[1])); // association name only
```

If file does not exist: fields start empty, checkbox ticked (default minimise).

### Save logic

Serialise via `SimpleJsonWriter`:

```json
{
  "decision_var_associations": ["Assoc1", "Assoc2"],
  "objective": "<ocl expr>",
  "minimise": true,
  "decision_vars": [],
  "objective_weight": 1
}
```

`decision_vars` written as empty array `[]` (not touched by this editor; downstream toolchain populates it).

### Registration in `useplugin.xml`

```xml
<action id="org.tzi.use.plugin.use2qubo.action.EditQuboConfigAction"
        label="Edit QUBO Config"
        class="org.tzi.use.plugin.use2qubo.action.EditQuboConfigAction"
        menu="Plugins"
        menuitem="Edit QUBO Config"
        tooltip="Open form editor for export_config.json (objective, decision variables, minimise flag)"/>
```

## Acceptance Criteria

- "Edit QUBO Config" appears in Plugins menu; enabled only when model loaded.
- Dialog pre-fills all fields from existing `export_config.json` when present.
- Dialog shows blank form when no config file exists.
- "Save" writes valid JSON parseable by `QuboConfig.parse()`.
- "Close" / cancel leaves file unchanged.
- After save, triggering "Derive QUBO Matrix" uses the updated config.
- `mvn compile` produces zero errors.

## Dependencies

JAVA-001 (`QuboConfig`, `QuboContext`). JAVA-004 (`useplugin.xml` registration pattern). Refactors path logic from `QuboContextBuilder` and `SystemStateExporter`.
