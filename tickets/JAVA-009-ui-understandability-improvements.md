# JAVA-009 — UI Understandability & Visualisation Improvements

**Status:** Done  
**Priority:** Medium  
**Depends on:** JAVA-008 (Dialog → View migration)  
**Context:** Plugin works but gives poor feedback during QUBO derivation and the matrix view lacks visual context.

---

## Problem

1. QUBO derivation fires with zero progress feedback — UI appears frozen for large models.
2. `QuboMatrixView` uses flat colour rendering with no legend, no magnitude scaling, no variable index panel, no terms breakdown, and does not show penalty weight B or derivation time.
3. `QuboConfigView` has no OCL validation, no help text, and does not show the target config file path before saving.

---

## Proposed Changes

### 1. Progress dialog during QUBO derivation

`SwingWorker<QuboResult, Void>` → `SwingWorker<QuboResult, String>`.

Show modal `JDialog` with indeterminate `JProgressBar`, step label, and Cancel button.

`QuboEngine.derive` accepts a `Consumer<String>` progress callback; publishes:
- `"Building variable index…"`
- `"Compiling objective OCL…"`
- `"Sampling: constant term"`
- `"Sampling: linear terms (i/n)…"` (per i)
- `"Sampling: quadratic pairs (k/total)…"` (per pair)
- `"Running exactness check…"`

**Files:** `DeriveQuboAction.java`, `QuboEngine.java`

---

### 2. Magnitude-scaled colour rendering

Replace flat colour constants `COLOR_POSITIVE = (200, 220, 255)` and `COLOR_NEGATIVE = (255, 200, 200)` with intensity-scaled rendering:

- `maxAbs = max |Q_ij|` across all nonzero entries
- `alpha = |v| / maxAbs` clamped to `[0.05, 1.0]`
- Positive: WHITE → cornflower blue `(100, 149, 237)` — interpolated by alpha
- Negative: WHITE → indian red `(205, 92, 92)` — interpolated by alpha
- Zero: `Color.WHITE` (unchanged)

`COLOR_POSITIVE` and `COLOR_NEGATIVE` constants are removed; colour is computed per cell in `getTableCellRendererComponent`.

**File:** `QuboMatrixView.java` → `ColoredCellRenderer`

---

### 3. Colour legend + enriched stats panel

Add to `buildStatsPanel`:
- Coloured swatches: `[■ negative]` `[■ positive]`
- `nnz = X` (nonzero count)
- `density = X.X%` (nnz / n²)
- `B = X.X` (penalty weight = nVars + 1)
- `time = Xms` (derivation time)

`QuboResult` needs two new fields: `double penaltyWeight` and `long derivationMs`.

- `penaltyWeight` = `nVars + 1`, computed inside `QuboEngine.derive()` and passed to `QuboResult` constructor.
- `derivationMs` measured in `DeriveQuboAction` around the `QuboEngine.derive()` call; passed into `QuboResult` constructor.

**Files:** `QuboMatrixView.java`, `QuboResult.java`, `DeriveQuboAction.java`

---

### 4. Variable index side panel

`JSplitPane` (horizontal) wrapping existing scroll pane in `buildMatrixPanel()`:
- Left: existing Q-matrix scroll pane
- Right: `JTable` (columns: `Idx`, `Variable`) listing all `varLabels`

Width ~250px; collapsible via split divider.

**File:** `QuboMatrixView.java` → `buildMatrixPanel()`

---

### 5. OCL validation in config view

"Validate OCL" button next to `objectiveField`. On click:
- Compile via `OCLCompiler.compileExpression` against loaded model
- Show inline `JLabel` (green ✓ or red ✗ + error text) below the field

Requires passing `MModel` (or `Session`) into `QuboConfigView` constructor:  
`QuboConfigView(File configFile, MModel model)`

`EditQuboConfigAction` already has `pluginAction.getSession()` → `ISystem.model()`.

**Files:** `QuboConfigView.java`, `EditQuboConfigAction.java`

---

### 6. Help tooltips + file path in config view

`setToolTipText` on each field:
- Objective field: OCL expression example
- Minimise checkbox: minimise vs. maximise explanation
- Association list: what decision-var associations are and how they affect the export
- Add button: note that name must match `.use` model exactly

Show target file path in read-only `JTextField` at top of view.

**File:** `QuboConfigView.java`

---

### 7. QUBO terms breakdown panel

Add `buildTermsPanel(QuboResult result)` to `QuboMatrixView`.

Flat `JTable` listing every QUBO term (constant + all nonzero linear + all nonzero quadratic), with columns:

| Column | Content |
|---|---|
| Type | `constant` / `linear` / `quadratic` |
| i | variable index (`—` for constant) |
| j | second variable index (`—` for non-quadratic) |
| Label(s) | resolved from `result.varLabels`; quadratic shows `label_i × label_j` |
| Coefficient | raw `double` value |

Row count: `1 + nVars + |quadratic|`. Default sort: `|Coefficient|` descending.

UI placement: `JTabbedPane` replacing the current CENTER panel — tab "Matrix" holds existing `buildMatrixPanel()` scroll pane; tab "Terms" holds this table. Variable index side panel (§4) stays inside the "Matrix" tab.

No changes to `QuboEngine` or `QuboResult` — data sourced entirely from existing fields (`constant`, `linear`, `quadratic`, `varLabels`).

**File:** `QuboMatrixView.java`

---

## Verification

1. `mvn clean package` — no compile errors.
2. Load `GarbageTruckRouting.use` + `.cmd` in USE.
3. Edit QUBO Config: check tooltips, OCL validate button, file path display.
4. Derive QUBO Matrix: progress view shows step updates; closes on completion.
5. Matrix view — Matrix tab: legend, magnitude-scaled colours, variable index side panel.
6. Matrix view — Terms tab: all QUBO terms listed with resolved labels, sorted by magnitude.
7. Stats panel: nnz, density, B, time values present and correct.
8. Large model (>20 vars): quadratic pair progress updates correctly.
