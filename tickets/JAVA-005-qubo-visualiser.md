# JAVA-005 — QUBO Matrix Visualiser Dialog

## Goal

Replace the plain `JOptionPane` summary shown after QUBO derivation with a structured Swing dialog that displays the Q matrix as a colour-coded table, with variable labels as row/column headers and a stats bar at the top.

## Scope

New class `ui/QuboMatrixDialog`. Modify `DeriveQuboAction.done()` to open it instead of the current information message box. No changes to derivation logic.

## New Class: `ui/QuboMatrixDialog.java`

Extends `JDialog`. Constructor: `QuboMatrixDialog(Frame parent, QuboResult result, File outputFile)`.

### Layout

**Top panel (`FlowLayout`)** — stat labels:
- `nVars = N`
- `nSamples = N`
- `constant = N.NNN`
- `exact:` + badge label (`"YES"` green / `"NO"` red)

**Centre (`JScrollPane` wrapping `JTable`)** — Q matrix:
- `nVars × nVars` cells
- Diagonal cell `(i,i)` = `linear.getOrDefault(i, 0.0)`
- Upper-triangle cell `(i,j)` where `i < j` = `quadratic.getOrDefault(i+","+j, 0.0)`
- Lower-triangle cell `(i,j)` where `i > j` = mirrors `(j,i)` (read-only display only)
- Row and column headers = `varLabels.get(i)` (abbreviated to 20 chars + `…` if longer)
- Cell background: positive → `new Color(200, 220, 255)`, negative → `new Color(255, 200, 200)`, zero → white
- Table is non-editable (`isCellEditable` returns false)

**Bottom panel (`FlowLayout`, right-aligned)**:
- `"Save to File"` button — calls `QuboResultExporter.write(result, outputFile)` in a `SwingWorker`; shows error dialog on `IOException`; does not close the dialog
- `"Close"` button — `dispose()`

### Key implementation notes

- Use `DefaultTableModel` with `nVars` columns and `nVars` rows; populate in constructor.
- Custom `TableCellRenderer` applies background colour based on cell value.
- Column header names set via `JTableHeader` after populating; row header via a `JList` pinned to the left of the scroll pane (standard "row header" pattern).
- Preferred size: `Math.min(nVars * 80, 900) × 600` px; dialog is resizable.
- Modal: `super(parent, "QUBO Matrix — " + result.nVars + " variables", true)`.

## Changes to Existing Code

### `action/DeriveQuboAction.java` — `done()` method

Replace:
```java
JOptionPane.showMessageDialog(parent,
        "QUBO matrix exported to:\n" + outputFile.getAbsolutePath()
                + "\n\n" + result.toString(),
        "Derive QUBO", JOptionPane.INFORMATION_MESSAGE);
```

With:
```java
new QuboMatrixDialog(parent, result, outputFile).setVisible(true);
```

Add import: `org.tzi.use.plugin.use2qubo.ui.QuboMatrixDialog`.

## Acceptance Criteria

- Dialog opens after successful derivation; no plain `JOptionPane` shown.
- Q matrix table has exactly `nVars` rows and `nVars` columns.
- Diagonal cells display linear coefficients; upper-triangle cells display quadratic coefficients.
- Positive cells are light blue, negative cells are light red, zero cells are white.
- "Save to File" writes the output file without closing the dialog.
- "Close" disposes the dialog.
- `mvn compile` produces zero errors.

## Dependencies

JAVA-003 (`QuboResult`, `QuboResultExporter`). JAVA-004 (`DeriveQuboAction`). No new library dependencies.
