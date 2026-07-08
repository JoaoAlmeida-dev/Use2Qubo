# `action`

The two USE menu commands the plugin registers (`src/main/resources/useplugin.xml`).
Both delegate to `qubo/` for logic and `ui/` for display; this package is pure
Swing/USE-runtime glue, no derivation or parsing logic of its own.

| Class | Menu item | Does |
|---|---|---|
| `DeriveQuboAction` | "Derive QUBO Matrix" | Builds a `QuboContext`, runs `QuboEngine.derive` off the EDT with a cancellable progress dialog, opens the result in `QuboMatrixView`. |
| `EditQuboConfigAction` | "Edit QUBO Config" | Resolves `qubo_config.json` next to the loaded model, opens it in `QuboConfigView`. |

Both require a loaded model (`shouldBeEnabled` checks `session.hasSystem()`); `DeriveQuboAction`
additionally aborts with a warning dialog if the system state is empty (no `.cmd` script run yet).

```mermaid
sequenceDiagram
    actor User
    participant Action as DeriveQuboAction
    participant Builder as QuboContextBuilder
    participant Engine as QuboEngine (SwingWorker thread)
    participant View as QuboMatrixView

    User->>Action: click "Derive QUBO Matrix"
    Action->>Action: check session has system + non-empty state
    Action->>Builder: build(session.system())
    Builder-->>Action: QuboContext
    Action->>Action: show modal progress dialog
    Action->>Engine: derive(ctx, progress callback)
    Engine-->>Action: step labels (published to dialog)
    Engine-->>Action: QuboResult
    Action->>View: new QuboMatrixView(result)
    Action->>User: dock ViewFrame, show result
```

```mermaid
sequenceDiagram
    actor User
    participant Action as EditQuboConfigAction
    participant Paths as QuboConfigPaths
    participant View as QuboConfigView

    User->>Action: click "Edit QUBO Config"
    Action->>Paths: resolveConfigFile(system)
    Paths-->>Action: File (qubo_config.json)
    Action->>View: new QuboConfigView(file, model)
    Action->>User: dock ViewFrame, show form
```
