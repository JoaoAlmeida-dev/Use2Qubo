# USE2QUBO Tutorial

USE OCL plugin: derive QUBO matrix from OCL invariants + config. Two ways to run: GUI (USE desktop) or headless CLI.

## 1. Prerequisites

- Java 11+, Maven 3.6+
- USE OCL 7.5.0 checkout (for `use-core`/`use-gui` JARs)

## 2. Build

```bash
git clone https://github.com/useocl/use.git
cd use
mvn package -pl use-core -pl use-gui
```

Copy resulting JARs (`use-core-7.5.0.jar`, `use-gui-7.5.0.jar`) into `lib/` here (or grab from `<USE_HOME>/lib/`). Then:

```bash
mvn clean package
```

Output: `target/use2qubo-1.0.0.jar`.

Or use `tools/build-plugin.ps1` (PowerShell wrapper).

## 3. Install into USE (GUI mode)

Copy `use2qubo-1.0.0.jar` → `<USE installation>/plugins/`. Restart USE. Auto-registers via `useplugin.xml`. New menu: **Plugins → Derive QUBO Matrix**, **Plugins → Edit QUBO Config**.

## 4. Model your problem

Need 2 files side by side:

1. **`model.use`** — classes, associations, OCL invariants + helper operations (normal USE model).
2. **`qubo_config.json`** — same dir as `.use` file. Declares:
   - which associations are binary decision vars `x_i ∈ {0,1}`
   - objective OCL expression `c(x)`
   - penalty method

Minimal example:

```json
{
  "decision_var_associations": ["RouteVisits", "AssignedTo"],
  "decision_vars": [
    {"type": "link", "association": "RouteVisits", "domain": ["Route", "Node"]},
    {"type": "link", "association": "AssignedTo",  "domain": ["Route", "Truck"]}
  ],
  "penalty_method": "verma-lewis",
  "objective": {
    "expression": "Route.allInstances->collect(r | r.edgeCost())->sum()",
    "minimise": true
  }
}
```

### Field notes (full ref: `examples/GarageTrucks/qubo_config_schema.md`)

- `decision_var_associations` — assoc names treated as binary vars. Must exist in `.use` model.
- `decision_vars[].type` — only `"link"` supported now.
- `decision_vars[].domain` — `[source, target]` class pair.
- `decision_vars[].index_attribute` (optional) — int attribute on assoc class giving stable index. Needed for association classes w/ ordering (e.g. route `step`). Without it: enumeration order, non-deterministic.
- `penalty_method` — `"verma-lewis"` (default, smallest valid α), `"posiform-negaform"`, or `"manual"` (needs extra `"penalty_weight": <number>`). Omit → old AutoQUBO v1 manual-weight fallback.
- `objective.expression` — OCL, must return numeric. **Must** only reference decision-var links + static attrs — no manually-set derived attrs (sampling can't see them).
- `objective.minimise` — bool; engine negates expr internally if `false`.

### Objective-writing rules (hard constraints)

1. Objective/penalty terms depending on **3+ decision vars at once** aren't degree-2 exact → engine's polynomial fit silently wrong on those points. Exactness check catches it (see §7), but better: keep terms pairwise.
2. Constraints that count/exist-check across many instances (route coverage, shape, etc.) → don't leave as boolean `inv`. Instead write named `.use` operation returning integer violation magnitude, sum with explicit weight in `objective.expression` (see `coveragePenalty()`/`shapePenalty()` pattern in GarageTrucks example).
3. Never read an association-class's own attribute (e.g. `step`) or an `ordered` role's insertion order inside objective/penalty ops — unpopulated during sampling.

## 5. Populate a scenario (GUI or CLI)

Write a `.cmd` script with SOIL statements (`!create`, `!insert`, `!set`) building one concrete object diagram, end with `check` to confirm invariants pass. See `examples/GarageTrucks/GarbageTruckRouting.cmd` for full pattern.

GUI: `File → Open` the `.use` model, then run the `.cmd` script from USE's shell (`open script.cmd` or paste commands).

## 6a. Derive QUBO — GUI mode

1. Load `.use` model + populate state (run `.cmd`).
2. **Plugins → Edit QUBO Config** — form editor for `qubo_config.json` (optional, can hand-edit JSON directly).
3. **Plugins → Derive QUBO Matrix** — runs `QuboEngine`, opens `QuboMatrixView` (colour-coded Q matrix, tabs: Matrix / Terms / Sampling / Exactness / Graph).
4. Exports `qubo.json` next to the model.

## 6b. Derive QUBO — headless CLI mode

No USE GUI needed. `QuboCli` entry point:

```bash
java -cp use2qubo-1.0.0.jar:use-core-7.5.0.jar:use-gui-7.5.0.jar \
  org.tzi.use.plugin.use2qubo.cli.QuboCli \
  --model model.use --cmd script.cmd \
  [--config qubo_config.json] [--out qubo.json]
```

(Windows: `;` instead of `:` in classpath.)

Defaults: `--config` → `qubo_config.json` next to model file; `--out` → `qubo.json` next to model file.

Exit codes: `0` = derived + exact, `3` = derived but exactness check failed, `2` = usage error, `1` = other error.

Example against shipped GarageTrucks scenario:

```bash
java -cp use2qubo-1.0.0.jar:<use-jars> org.tzi.use.plugin.use2qubo.cli.QuboCli \
  --model examples/GarageTrucks/GarbageTruckRouting.use \
  --cmd examples/GarageTrucks/GarbageTruckRouting.cmd
```

## 7. Read the output — `qubo.json`

Contains: `nVars`, constant/linear/quadratic coefficients (the `Q` matrix), `exact` flag, derivation time.

**`exact: false`** → q(x) fit diverges from real f(x) on held-out points. Root cause almost always rule 1 or 3 above (a term depends on 3+ vars, or reads an unpopulated attribute). Fix the OCL, not the plugin.

Feed `qubo.json` `Q` matrix into any QUBO solver (D-Wave, simulated annealing via `neal`/`dimod`, etc.) — plugin doesn't run the solver itself.

## 8. Worked examples

| Example | What it shows |
|---|---|
| `examples/GarageTrucks/` | Route/assignment decision vars, `index_attribute` for ordered stops, mixed automatic + manually-weighted penalty terms |
| `examples/autoquboMaxClique/` | Simple link-presence decision vars, classic combinatorial benchmark, no `index_attribute` needed |

Each has matching `.cmd` for quick smoke test after install.

## 9. Common pitfalls

- `qubo_config.json` not next to `.use` file → engine can't find it (or CLI `--config` not pointed right).
- Objective references manually-set attribute (`totalTravelTime`) instead of computing from links → sampling breaks silently, check `exact` flag.
- Boolean `inv` used for a many-instance count/coverage check → not degree-2, rewrite as violation-magnitude operation + weighted sum term.
- Forgot `index_attribute` on an ordered association class → non-deterministic indices, breaks reproducibility.
