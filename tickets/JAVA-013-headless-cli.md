# JAVA-013 — Headless CLI: derive QUBO without USE GUI

## Goal

Add a standalone CLI entry point that runs the full derive-QUBO pipeline
(`.use` model + `.cmd` state script + `qubo_config.json` → `qubo.json`)
without launching USE's Swing UI or requiring a human to click
"Derive QUBO Matrix". Needed for batch/scripted QUBO generation
(experiment sweeps, CI, non-interactive pipelines).

## Why this is feasible

`QuboContextBuilder.build(MSystem, Path)` and `QuboEngine.derive(QuboContext, ...)`
only touch `MSystem`/`MModel`/`MSystemState` (`use-core`). Neither depends on
`use-gui` or Swing. `session.system()` in `DeriveQuboAction` is the only
GUI-sourced piece — everywhere else in the plugin already works headlessly
in principle. `QuboResultExporter` writes `qubo.json` with no UI dependency
either.

The one missing piece: something to build an `MSystem` from a `.use` file
and populate its state from a `.cmd` script, outside `MainWindow`/`Session`.
`use-core` supports this via its own compiler/shell classes
(`org.tzi.use.parser.use.USECompiler` or equivalent — confirm exact class/API
against the checked-out `use-core-7.5.0` sources in the sibling `use` repo
build, `Options`/`Shell` init patterns used by USE's own `use` console
launcher are the reference implementation to copy).

## Scope

**New package:** `org.tzi.use.plugin.use2qubo.cli`

**New class:** `QuboCli` with a `main(String[] args)`:

```
usage: use2qubo-cli --model <model.use> --cmd <script.cmd> [--config <qubo_config.json>] [--out <qubo.json>]
```

- `--model` (required) — path to `.use` file.
- `--cmd` (required) — `.cmd` script to populate the object diagram (mirrors
  what the example `.cmd` files under `examples/*/` already do interactively).
- `--config` (optional) — defaults to `qubo_config.json` next to the model
  file, same resolution rule as `QuboConfigPaths.resolveConfigFile`.
- `--out` (optional) — defaults to `qubo.json` next to the model file.

**Pipeline:**
1. Compile `.use` file → `MModel`, construct `MSystem`.
2. Run `.cmd` script against that system to populate state (reuse USE's
   own command interpreter; do not hand-roll link/object creation).
3. `QuboContextBuilder.build(system, configPath)`.
4. `QuboEngine.derive(ctx, progress -> System.err.println(progress))` —
   reuse the existing progress-callback signature, just print to stderr
   instead of a Swing label.
5. `QuboResultExporter.export(result, outPath)` (confirm exact method name
   against `QuboResultExporter.java`).
6. Print a one-line summary to stdout (nVars, exact PASS/FAIL, derivation ms,
   output path). Non-zero exit code on any failure, with the exception
   message on stderr — no stack trace dump by default.

**Not in scope:**
- No new Maven `exec`/`assembly` packaging beyond what's needed to run
  `QuboCli` (a simple `java -cp ... QuboCli` invocation via the built jar
  plus `lib/use-core-*.jar` on the classpath is sufficient; document the
  command in README, don't build a fat launcher).
- No change to `DeriveQuboAction`/GUI path — CLI is additive.
- No new decision-variable or config-format features.

## Acceptance criteria

- Running `QuboCli` against `examples/GarageTrucks` (model +
  `.cmd`) with no USE window open produces the same `qubo.json` Q-matrix
  (constant/linear/quadratic terms) as running "Derive QUBO Matrix"
  interactively on the same example.
- Same check for `examples/autoquboMaxClique`.
- Bad `--model` path / bad `--cmd` path / missing `qubo_config.json` each
  produce a clear one-line error on stderr and non-zero exit, not a stack
  trace.

## Dependencies

JAVA-001 (`QuboContext`/`QuboContextBuilder`), JAVA-002 (`QuboEngine`),
JAVA-003 (`QuboResultExporter`) — all already implemented, reused as-is.

## Open question

Exact `use-core` API for headless `.use` compilation + `.cmd` script
execution needs to be confirmed by reading `use-core-7.5.0` sources (the
`use` command-line launcher in the upstream `useocl/use` repo already does
this non-interactively — mirror its approach rather than reinventing it).
