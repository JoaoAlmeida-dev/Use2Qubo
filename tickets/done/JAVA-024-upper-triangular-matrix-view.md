# JAVA-024 — Show the Q matrix as upper triangular in the Matrix tab

**Status:** Done
**Priority:** Medium
**Depends on:** none
**Context:** `MatrixTabPanel` builds its table by mirroring every quadratic coefficient into the lower triangle at full value (`data[i][j] = quadratic.get(j + "," + i)` for `i > j`). `QuboResult.quadratic` stores each coupling once under `"i,j"` with `i < j`, and `qubo.json`, the expression panel and the paper all use the upper-triangular convention. The mirrored table is neither that Q nor a valid symmetric Q (which would need `Q_ij/2` in both halves): read as `xᵀQx` it double-counts every coupling (e.g. depot/disposal 22 in the README screenshot would contribute 44). No ticket or doc records a reason for the mirroring.

## Scope

- `ui/tabs/MatrixTabPanel.java`:
  - Table data: diagonal = linear, `i < j` = quadratic `"i,j"`, `i > j` = `null` (no value).
  - `ColoredCellRenderer`: `null` cell → blank text, neutral grey background (below-diagonal), still shows the selection border.
  - `highlightCell(i, j)` (called from the Sampling tab with `derivedI/derivedJ`): normalise to `(min, max)` so it always lands on the upper cell.
  - Selecting a below-diagonal cell redirects the selection to its mirrored upper cell, so clicks never land on an empty cell.
- `ui/tabs/README.md`: Matrix row says the table is upper triangular.
- No change to engine, `QuboResult`, `qubo.json` export or `ExpressionPanel`.

## Files Changed

| File | Change |
|---|---|
| `src/main/java/org/tzi/use/plugin/use2qubo/ui/tabs/MatrixTabPanel.java` | upper-triangular table, grey empty lower half, normalised highlight/selection |
| `src/main/java/org/tzi/use/plugin/use2qubo/ui/tabs/README.md` | describe upper-triangular Matrix tab |

## Acceptance criteria / Verification

- `mvn -q clean verify` passes (existing tests unchanged; UI-only change).
- Manual (user): Derive QUBO on an example, Matrix tab shows values on and above the diagonal only, lower half grey and blank; Sampling tab row click highlights the upper cell; clicking a grey cell selects its upper mirror.

## Commit

Once done, `git commit` the changes with a commit message of 15 words max.
