# JAVA-008 — UI: Convert Popup Dialogs to Dockable Internal Windows

**Status:** Done  
**Priority:** Medium  
**Context:** Both QUBO dialogs are modal — they block the USE desktop while open and cannot be repositioned freely. USE already provides a dockable internal-window system via `JDesktopPane` + `ViewFrame`.

---

## Problem

- `QuboConfigDialog extends JDialog` — modal, blocks all interaction with USE while open
- `QuboMatrixDialog extends JDialog` — modal, blocks all interaction with USE while open
- Neither window can be repositioned within the USE desktop or left open while working in the main tool

---

## Solution

Plug into the existing USE MDI system:
- `MainWindow` holds a `JDesktopPane fDesk` with a `ViewManager` (custom desktop manager)
- `ViewFrame extends JInternalFrame` wraps any `View`-implementing panel
- `MainWindow.instance().addNewViewFrame(frame)` registers it — handles stagger-positioning automatically
- `View` interface requires only `void detachModel()`

No framework changes needed. Plugin actions already have access to `MainWindow.instance()`.

---

## Change 1 — QuboConfigView

**New file:** `ui/QuboConfigView.java`

- `extends JPanel implements View`
- Constructor: `QuboConfigView(File configFile)` (no `Frame parent` parameter)
- Move all UI construction from `QuboConfigDialog` constructor verbatim:
  - OCL objective text field
  - Minimise checkbox
  - Decision-variable association JList with Add/Remove buttons
  - Save and Close buttons
- "Close" button: `SwingUtilities.getAncestorOfClass(JInternalFrame.class, this).dispose()`
- `detachModel()`: no-op

**Delete after verification:** `ui/QuboConfigDialog.java`

---

## Change 2 — QuboMatrixView

**New file:** `ui/QuboMatrixView.java`

- `extends JPanel implements View`
- Constructor: `QuboMatrixView(QuboResult result)` (no `Frame parent` parameter)
- Move all UI construction from `QuboMatrixDialog` constructor verbatim:
  - Stats panel (nVars, nSamples, constant, exact badge)
  - Colour-coded JTable with row/column headers and tooltips
  - Save to File button (JFileChooser works fine inside JInternalFrame)
  - Close button
- "Close" button: same ancestor-dispose pattern
- `detachModel()`: no-op

**Delete after verification:** `ui/QuboMatrixDialog.java`

---

## Change 3 — Update Action Classes

**`action/EditQuboConfigAction.java`**

```java
// Before
new QuboConfigDialog(parent, configFile).setVisible(true);

// After
QuboConfigView view = new QuboConfigView(configFile);
ViewFrame frame = new ViewFrame("Edit QUBO Config", view, null);
MainWindow.instance().addNewViewFrame(frame);
frame.setVisible(true);
```

**`action/DeriveQuboAction.java`** (inside `SwingWorker.done()`)

```java
// Before
new QuboMatrixDialog(parent, result).setVisible(true);

// After
QuboMatrixView view = new QuboMatrixView(result);
ViewFrame frame = new ViewFrame("QUBO Matrix — " + result.nVars + " variables", view, null);
MainWindow.instance().addNewViewFrame(frame);
frame.setVisible(true);
```

---

## Notes

- `MainWindow.instance()` is non-null whenever a plugin action runs.
- Re-triggering the menu action creates a new window instance (consistent with current behaviour).
- `addNewViewFrame()` auto-staggers position so multiple open windows don't overlap.
- Pass `null` for `iconFilename` — USE handles this gracefully; add icon later if desired.
- `JFileChooser` inside an internal frame works without a parent frame reference.

---

## Files

| File | Action |
|------|--------|
| `ui/QuboConfigView.java` | Create |
| `ui/QuboMatrixView.java` | Create |
| `action/EditQuboConfigAction.java` | Update instantiation |
| `action/DeriveQuboAction.java` | Update instantiation |
| `ui/QuboConfigDialog.java` | Delete after verification |
| `ui/QuboMatrixDialog.java` | Delete after verification |

USE framework references (read-only, in `use-gui/`):
- `org.tzi.use.gui.main.ViewFrame`
- `org.tzi.use.gui.main.MainWindow` — `addNewViewFrame()`
- `org.tzi.use.gui.views.View`

---

## Verification

1. `mvn package` — build succeeds, no compile errors
2. Load USE with `GarbageTruckRouting.use` + instance
3. Trigger "Edit QUBO Config" → config window opens in USE desktop, draggable, non-modal
4. Trigger "Derive QUBO" → matrix window opens in desktop; both windows coexist simultaneously
5. Close buttons dismiss only the internal window, not the application
6. Save in config view persists the config JSON file correctly
7. "Save to File" in matrix view opens file chooser and exports JSON correctly
