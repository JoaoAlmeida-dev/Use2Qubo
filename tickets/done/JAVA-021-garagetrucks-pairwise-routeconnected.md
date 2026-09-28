# JAVA-021 — GarageTrucksPairwise/TSP: cut degree/ancillas via invariant redesign

**Status:** Done
**Priority:** Medium
**Depends on:** none (parallel line of investigation to JAVA-020, same lever)
**Context:** User asked to apply the MaxCliquePairwise lesson (JAVA-020: re-contexting a whole-candidate-set boolean predicate to a smaller-footprint per-instance context collapses degree/ancilla count) to the GarageTrucks routing example.

Diagnosis (this session, no code changes yet): `examples/GarageTrucks/qubo.json` reports 9 decision vars (7 `RouteRoad` + 2 `AssignedTo`), `polyDegree=7`, `nAncillaVars=238`, `exact=true`. `PenaltyEvaluator.evalOneInvariant` only reads invariants as boolean 0/1 (no magnitude), so the polynomial degree PolySampler needs for a task equals that task's decision-variable footprint.

Checked each invariant's footprint against the animated scenario (7 roads, fuelRange 100, bin loads small):
- `fuelWithinRange`/`capacityWithinRange`: footprint 7 (global sum over route) but numerically always-true in this scenario (max edgeCost 65 ≤ 100; max bin load 1.48 ≤ capacity) — constant-zero penalty, does not drive escalation.
- `binCovered`/`routeTouchesDepot`/`routeTouchesDisposal`: footprint ≤3 (few edges touch depot/disposal/bin nodes) — not the driver.
- `routeConnected` (`context Route`, single instance, `Node.allInstances->forAll(...)` internally reading `RouteRoad.allInstances` unfiltered by node): footprint = all 7 edges in one boolean conjunction. **This is the degree-7 driver**, structurally identical to old `cliqueProperty` (whole-candidate-set predicate vs. per-instance predicate).

Hypothesis: re-contexting `routeConnected` from `Route` (1 instance, footprint 7) to `Node` (6 instances, footprint = edges incident to that node, max 3 in this graph) should drop max degree to ~3 (not guaranteed all the way to native 2, since flow-conservation equality over 3 vars isn't guaranteed pairwise-reducible the way clique-conflict was) and crash the ancilla count.

## Scope

1. New example directory `examples/GarageTrucksPairwise/` (leaves `examples/GarageTrucks/` untouched):
   - `GarageTrucksPairwise.use`: copy of `GarbageTruckRouting.use`, replacing the single `context Route inv routeConnected` with `context Node inv routeConnectedAtNode` — same flow-conservation logic (`selOut - selIn = target`), computed per node instead of via `Node.allInstances->forAll` inside one Route-context invariant.
   - `GarageTrucksPairwise.cmd`: same scenario population as `GarbageTruckRouting.cmd` (copy, adjust model name).
   - `qubo_config.json`: same `decision_var_associations`/`decision_vars`/`objective` as `GarageTrucks/qubo_config.json`; lower `max_degree` (e.g. 5) since no term should need the full 7.
2. Build the plugin jar if `target/use2qubo-1.0.0.jar` is stale; run `QuboCli` against the new example.
3. Record `nVars`, `polyDegree`, `nAncillaVars`, `exact` from the new `qubo.json`; compare against `GarageTrucks/qubo.json` (9 vars, degree 7, 238 ancillas, exact=true).
4. Hand-verify the animated scenario's feasible solution still evaluates to the expected penalty (0, since all invariants pass `check`) under the new encoding.
5. Out of scope: touching `examples/GarageTrucks/` itself; paper sections (this is exploratory — only fold into the paper once numbers are verified and the user decides it's worth citing); fuel/capacity range invariants (diagnosed as not the driver, left as-is).

## Files Changed

| File | Change |
|---|---|
| `examples/GarageTrucksPairwise/GarageTrucksPairwise.use` | new |
| `examples/GarageTrucksPairwise/GarageTrucksPairwise.cmd` | new |
| `examples/GarageTrucksPairwise/qubo_config.json` | new |
| `examples/GarageTrucksPairwise/qubo.json` | new, generated |

## Acceptance criteria / Verification

- New `qubo.json` inspected for `exact`, `nVars`, `polyDegree`, `nAncillaVars`.
- Comparison against original GarageTrucks numbers stated plainly (improvement or not).
- `examples/GarageTrucks/qubo.json` unchanged.

## Attempt 1 result (superseded): GarageTrucksPairwise, routeConnected re-context alone

Re-contexting `routeConnected` from `Route` to `Node` alone made things **worse**: `qubo.json` came out `nVars=256, polyDegree=9, nAncillaVars=247, exact=true`, versus the original `nVars=247, polyDegree=7, nAncillaVars=238`. Root cause found: `fuelWithinRange`/`capacityWithinRange` read `self.truck->collect(...)->sum()` as their RHS threshold, and `self.truck` comes from the `AssignedTo` decision variable — when a route is unassigned that threshold collapses to 0, coupling every edge variable to every truck-assignment variable in one task (footprint 9, not 7). `routeConnected` was never the ceiling. Files kept on disk (`examples/GarageTrucksPairwise/`) as a documented negative result; not deleted.

## Attempt 2 (adopted): GarageTrucksTSP, full position-indexed (Lucas/TSP-style) redesign

Per user direction, went with the broader redesign option instead of patching the pairwise attempt further: new example `examples/GarageTrucksTSP/` replaces edge selection (`RouteRoad`) with a one-hot permutation decision variable `VisitAt(Waypoint,Position)` over the 3 bin waypoints and 3 middle positions (depot fixed as start, disposal fixed as end — neither is a decision variable). Design decisions (confirmed with user before implementation):

- **Scope**: minimal ablation — capacity/fuel-range invariants dropped entirely for this variant (they would reintroduce the exact `self.truck`-coupling blowup found in Attempt 1). Isolates the position-encoding lever alone.
- **Missing edges**: metric closure (precomputed shortest-path cost) over the original 7-road graph, treated as undirected/bidirectional, stored as fixed `TerminalDistance` data — not Big-M penalties, since the user noted a real city graph is sparse in direct pairs but always has *some* path between any two points.
- **Endpoints**: depot/disposal fixed by construction (not decision variables); only the 3 intermediate waypoints get position one-hot vars (3×3=9 decision vars, same count as the original 9).
- **One-hot encoding split**: "at-most-one" side done via fixed `PositionConflict`/`WaypointConflict` associationclasses enumerating conflicting (waypoint,waypoint,position)/(position,position,waypoint) triples — same pairwise lever as `MaxCliquePairwise`'s `NonAdjacent`, native degree 2. "At-least-one" (coverage) side left as `exists`-based per-position/per-waypoint invariants (footprint 3, not natively pairwise) — only 6 such instances total.
- Full coverage/connectivity now falls out **structurally** from a valid permutation; `routeConnected`/`binCovered`/`routeTouchesDepot`/`routeTouchesDisposal` have no equivalent in this design — nothing left to check once one-hot holds.

### Verified numbers

`examples/GarageTrucksTSP/qubo.json`: `nVars=15` (9 decision + 6 ancilla), `polyDegree=3`, `exact=true` (exhaustive, 512/512 matched), `nAncillaVars=6`, Verma-Lewis-style `quadratizationPenalty (B)=2730.0`.

Versus original `GarageTrucks`: `nVars=247` (9 decision + 238 ancilla), `polyDegree=7`, `exact=true`.

Versus Attempt 1 `GarageTrucksPairwise`: `nVars=256`, `polyDegree=9`, `nAncillaVars=247`.

Hand-verified (`python`, reading `qubo.json` linear/quadratic/constant terms directly):
- True optimum (`n2@pos1, n3@pos2, n4@pos3`, matching the original scenario's route depot→n2→n3→n4→disposal): energy = **30.0**, exactly matching the true path cost (5+8+6+11) with zero penalty. All 6 ancillas evaluate to 0 at this point (their constituent variable pairs are never both active).
- Infeasible point (`n2@pos1, n3@pos1` conflict, `pos2` uncovered): energy = **2849.0**, correctly penalized far above the optimum.

Annealer sanity check (`neal`/`dimod`, 1000 reads, seed 42, via `articles/qmod_2026/experiments/.venv`'s Python — the system Python lacked `neal`): best sample energy **30.0**, active variables `VisitAt(n2,pos1)`, `VisitAt(n3,pos2)`, `VisitAt(n4,pos3)` — the true optimum found immediately, matching the hand-verified value exactly. Energy stats over 1000 reads: min 30.0, max 794.0, mean 148.06. The script's own `Feasible` flag reads `False` (its `REQUIRED_FOR_FEASIBILITY` check is hardcoded to `GarageTrucks` stop labels, not this variant's `VisitAt(...)` labels — same known limitation noted for `MaxCliquePairwise` in JAVA-020); ignore it here, judge by the active-variable set against the known optimum instead. Result: `simulatedAnnealing/results/garagetrucks_tsp_annealing_result.json`.

## Rename (post-adoption): domain-flavoured identifiers in GarageTrucksTSP

Attempt 2's identifiers above (`Terminal`, `Waypoint`, `Position`, `VisitAt`, `TerminalDistance`, `PositionConflict`, `WaypointConflict`, `Schedule`, `totalCost()`) were generic TSP-textbook names. Renamed throughout `examples/GarageTrucksTSP/` (`.use`, `.cmd`, `qubo_config.json`) to domain-flavoured names, structural roles unchanged — pure rename, no semantic change:

| Old | New |
|---|---|
| `Terminal` | `Stop` |
| `Waypoint` | `BinStop` |
| `Position` | `RouteSlot` |
| `VisitAt(Waypoint,Position)` | `CollectsAt(BinStop,RouteSlot)` |
| `TerminalDistance` | `TravelTime` |
| `PositionConflict` | `SlotClash` |
| `WaypointConflict` | `BinClash` |
| `Schedule` | `CollectionRun` |
| `totalCost()` | `totalTravelTime()` |

Roles renamed to match: `TravelTime.fromTerminal/toTerminal` → `fromStop/toStop`; `CollectsAt.waypoint/position` → `binStop/routeSlot`; `SlotClash.wa/wb/pos` → `ba/bb/slot`; `BinClash.pa/pb/wp` → `sa/sb/bp`. Enum `TerminalType`/literal `#Waypoint` → `StopType`/`#BinStop`. Instance names in the `.cmd` (`n2`, `n3`, `n4`, `depot`, `disposal`, `pos1..3`, `sched`) left unchanged — scenario data, not part of the requested mapping.

Re-ran `QuboCli` against the renamed files: `nVars=15`, `polyDegree=3`, `exact=true` (exhaustive, 512/512), `nAncillaVars=6`, `constant=270.0` — identical to the pre-rename numbers above, confirming the rename didn't change behaviour. All examples/mentions of the old names above (Attempt 2 section) describe the state *at that point in time*; current files on disk use the new names.

`examples/GarageTrucksTSPHuge/` (added after the rename, multi-lane scale-up) still uses the pre-rename names (`Waypoint`, `Position`, `VisitAt`, etc.) — out of scope for this rename, which only covered `GarageTrucksTSP/` per the request.

## Huge variant: GarageTrucksTSPHuge (multi-lane scale-up)

New `examples/GarageTrucksTSPHuge/` (kept pre-rename names — see rename section above), mirroring how `GarbageTruckRoutingHuge.cmd` scales the original edge-selection model: many independent lanes sharing one depot/disposal. Generator script `generate_huge_cmd.py` (parameterised by `NUM_LANES`, currently 6).

**Architectural finding**: unlike the original model, this design can't scale a single permutation arbitrarily — `Position`/`Waypoint`-`allInstances`-based coverage invariants (`atLeastOneWaypointHere`/`atLeastOnePositionHere`) have footprint equal to the *total* waypoint/position count unless explicitly lane-scoped by a `lane:Integer` attribute filter, which `GarageTrucksTSPHuge.use` adds (load-bearing, unlike the single-lane base model). With that filter, per-lane footprint stays fixed at 3 regardless of lane count — but total decision vars (`VisitAt` = `Waypoint`×`Position`, full cross product per `VarIndexBuilder`, no lane-aware restriction available in the framework) still grows as `9 × lanes²`, same quadratic-in-scale shape the original `RouteRoad(Route,Road)` huge example has (`routes × roads`, both of which scale with lane count there too). `PolySampler` has no sparsity pruning — it enumerates literally every `C(n,d)` combination regardless of which are structurally relevant — so this quadratic wall directly caps how large a "huge" variant can stay tractable.

**Measured scaling** (all degree 3, all `exact=true`):

| Lanes | nVars (decision+ancilla) | Runtime |
|---|---|---|
| 1 (base `GarageTrucksTSP`) | 15 (9+6) | <1s |
| 4 | 168 (144+24) | 36.5s |
| 6 | 360 (324+36) | 712s (~11.9 min) |

Settled on 6 lanes as the shipped huge variant — meaningfully bigger (24× the base case's decision vars) while staying under 12 minutes. `exactnessMethod=sampled` (20/20 matched), not exhaustive, since `nVars=324` exceeds `QuboConstants.EXACTNESS_EXHAUSTIVE_MAX_N` (20) — a spot-check, not a proof, same caveat as any large export.

Hand-verified: all-lanes-optimal point (every lane at its own `w2→w3→w4` order) evaluates to energy **180.0**, exactly `6 × 30.0` (each lane's independent optimum, computed the same way as the base case). Confirms the multi-lane construction is correct, not just fast.

## Simplification (post-rename): drop SlotClash/BinClash, use ->size()=1

Replaced the four-invariant one-hot encoding (`atMostOneBinStopPerSlot`/`atMostOneSlotPerBinStop` via the fixed `SlotClash`/`BinClash` pairwise-conflict associationclasses, plus `atLeastOneBinStopHere`/`atLeastOneSlotHere`) with two `exactlyOneBinStopHere`/`exactlyOneSlotHere` invariants using a direct `->size() = 1` cardinality check. Removed `SlotClash`/`BinClash` associationclasses from `.use` and their 18 `!insert` population lines from `.cmd`. `qubo_config.json` untouched (they were never decision variables, just fixed conflict data — confirmed no references before editing).

**Result: no regression.** Re-ran `QuboCli`: `nVars=15`, `polyDegree=3`, `exact=true` (exhaustive, 512/512), `nAncillaVars=6` — identical to the four-invariant version. Only `quadratizationPenalty (B)` shifted (2730.0 → 3270.0, expected, the objective/penalty combination changed slightly), `constant=270.0` unchanged. Hand-verified: true optimum still 30.0, an infeasible conflict point still correctly penalized (3389.0).

This was flagged going in as a real structural tradeoff (fewer classes/invariants vs. a previously-guaranteed-native-degree-2 "at most one" side) rather than an assumed win — landed as a pure simplification with no cost at this problem size. Worth re-checking if this encoding is ever pushed to `GarageTrucksTSPHuge` scale, since `->size()=1`'s exact required degree isn't structurally guaranteed to stay at the old at-least-one side's ceiling the way the pairwise `SlotClash`/`BinClash` side was guaranteed native degree 2 — not verified at scale here, `GarageTrucksTSPHuge` still uses the pre-simplification four-invariant encoding.

## Commit

Once done, `git commit` the changes with a commit message of 15 words max.
