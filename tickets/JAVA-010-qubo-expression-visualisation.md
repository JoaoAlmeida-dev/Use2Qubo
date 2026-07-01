# JAVA-010 — QUBO Expression Visualisation

**Status:** Done  
**Priority:** Medium  
**Depends on:** JAVA-009 (UI understandability improvements)  
**Context:** Matrix and Terms tabs show raw coefficients but give no structural view of variable interactions and no human-readable polynomial form of the QUBO expression.

---

## Problem

1. No visual representation of which variables are coupled and how strongly — the Q-matrix table reveals this only to users willing to scan every cell.
2. The full QUBO polynomial is never written out with resolved variable labels, making it hard to verify correctness or share with domain experts unfamiliar with index notation.

---

## Proposed Changes

### 1. QUBO Interaction Graph tab

Add a "Graph" tab to the existing `JTabbedPane` in `QuboMatrixView`.

**New file:** `ui/QuboGraphPanel.java` — `JPanel` subclass with custom `paintComponent(Graphics2D g)`. No new Maven dependencies; pure Java2D.

#### Layout

Circle layout: node `i` at angle `2π·i/n` on a circle of radius `min(width, height) / 2 - margin`. Drawn on every `paintComponent` call; resizes correctly.

#### Nodes (linear terms — diagonal Q entries)

- Filled circle; base radius 12 px scaled up by `|linCoeff_i| / maxLinear * 8` (range 12–20 px).
- Fill colour: cornflower blue `(100, 149, 237)` if coeff > 0; indian red `(205, 92, 92)` if coeff < 0; light gray `(200, 200, 200)` if ~0.
- Black 1 px border.
- Abbreviated label (`abbrev()`, same logic as matrix view) drawn below the node.

#### Edges (quadratic terms — off-diagonal Q entries)

- Line from node i centre to node j centre for every entry with `|Q_ij| > 1e-9`.
- Stroke width: `1.0 + |Q_ij| / maxQuad * 4.0` (range 1–5 px).
- Colour: cornflower blue α = 0.75 if positive; indian red α = 0.75 if negative.
- Drawn before nodes so nodes render on top.

#### Constant term

Text label in top-left corner: `c = {value:.4f}`.

#### Hover tooltips

`MouseMotionListener.mouseMoved`:

- Nearest node within 20 px: `setToolTipText(varLabels.get(i) + "\nlinear: " + coeff)`.
- Otherwise nearest edge: `setToolTipText(labelI + " × " + labelJ + "\nquadratic: " + coeff)`.
- Clear tooltip when nothing nearby.

#### Legend

Small `JPanel` docked at the bottom of `QuboGraphPanel` (inside its own `BorderLayout.SOUTH`):
- `■ positive (blue)` / `■ negative (red)` / node size = |linear|, edge width = |quadratic|.

#### Dense-graph notice

If `n > 50`, paint a translucent overlay with text: `"Graph may be dense — use Terms tab for details"`.

**Files:** `ui/QuboGraphPanel.java` (new), `ui/QuboMatrixView.java` (add tab)

---

### 2. QUBO Expression panel embedded in Matrix tab

Per-design decision: the polynomial text lives **inside the Matrix tab**, below the existing matrix + variable-index split pane. This avoids tab proliferation and keeps the matrix and its symbolic form co-located.

#### Layout change in `buildMatrixPanel(QuboResult)`

Current return type: `JSplitPane` (horizontal: matrix scroll | variable index).

New return type: `JSplitPane` (vertical):
- **Top:** existing horizontal `JSplitPane`, `resizeWeight = 0.7`.
- **Bottom:** expression panel (see below), `resizeWeight = 0.3`.

#### Expression panel

Header row (`JPanel`, `BorderLayout`):
- WEST: `JButton("Copy")` — copies full expression text to system clipboard via `Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null)`.
- CENTER: `JLabel("QUBO Expression")`.

Body: non-editable `JTextArea`, monospace font size 12, wrapped in `JScrollPane`.

Expression format (constant first, then linear by index, then quadratic by `i,j` key):

```
f(x) =   2.0000
       + 3.5000·x₀   [AssocA(t1,z1)]
       + 1.2000·x₁   [AssocA(t1,z2)]
       - 0.8000·x₂   [AssocA(t2,z1)]
       + 4.1000·x₀x₁ [AssocA(t1,z1) × AssocA(t1,z2)]
       ...
```

Sign-prefixed (`+` / `-`), aligned by decimal point. Omit zero terms (same as `linear` and `quadratic` maps already filter them).

**Method:** `buildExpressionPanel(QuboResult)` → `JPanel`, called from `buildMatrixPanel`.  
**Helper:** `buildExpressionText(QuboResult)` → `String`, builds the formatted text independently so the Copy button captures it without re-generating.

**File:** `ui/QuboMatrixView.java`

---

## Files Changed

| File | Change |
|---|---|
| `ui/QuboGraphPanel.java` | **New** — custom-painted QUBO interaction graph |
| `ui/QuboMatrixView.java` | Add "Graph" tab; embed expression panel + Copy button in Matrix tab via vertical `JSplitPane` |

No changes to `QuboResult`, `QuboEngine`, `DeriveQuboAction`, `QuboConfigView`, or `EditQuboConfigAction`.

---

## Verification

1. `mvn clean package` — no compile errors.
2. Load `GarbageTruckRouting.use` + `.cmd` in USE; run "Derive QUBO Matrix".
3. **Graph tab:** one node per variable; edges between variables with nonzero quadratic coupling; node colour/size matches sign and magnitude of linear coefficient; edge colour/thickness matches quadratic coefficient; hover shows label and coefficient value; constant shown in top-left corner.
4. **Matrix tab — Expression panel:** polynomial text present below the Q-matrix; all variable labels resolved; constant term first; nonzero linear and quadratic terms listed; Copy button copies text to clipboard.
5. **Expression correctness:** manually pick a row in the Terms tab and verify the same coefficient appears in the expression text.
6. **Regression:** Matrix tab (matrix + variable index), Terms tab, and Stats panel unchanged.
