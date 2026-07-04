# qubo_config.json — Field Reference

Configuration file read by the `QuboEngine` plugin action "Derive QUBO Matrix".
It tells the engine which model elements are binary decision variables, how to
weight constraint penalties, and what OCL expression to optimise.

---

## `decision_var_associations`

**Type:** `string[]`

List of association (or association class) names whose link instances are
treated as binary decision variables.
Each name must match an association declared in the loaded `.use` model.

The engine iterates all instances of each listed association to enumerate the
binary variables `x_i ∈ {0, 1}` that populate the Q matrix.
An entry of `1` means the link exists in the solution; `0` means it does not.

**Example:**
```json
"decision_var_associations": ["RouteRoad", "AssignedTo", "FuelSlack"]
```

---

## `penalty_method`

**Type:** `string`

Penalty weight calculation method used by AutoQUBO to compute the scalar `α`
in the combined objective `E(x) = c(x) + α · g(x)`, where `c(x)` is the cost
function and `g(x)` is the aggregated constraint violation function.

`α` must be large enough that every feasible solution has lower energy than
every infeasible one.  Setting it too low yields infeasible solutions; too high
drowns the cost signal and degrades solution quality.

Supported values:

| Value | Method | Notes |
|---|---|---|
| `"verma-lewis"` | Verma & Lewis (2022) | Default. Produces the smallest valid `α`; minimises distortion of the cost landscape. Requires only the cost matrix `(c_{ij})`. |
| `"posiform-negaform"` | Sum of Posiform/Negaform terms | Valid; tends to produce larger `α` than `"verma-lewis"`. |
| `"manual"` | User-supplied value | Requires an additional `"penalty_weight": <number>` field. Not recommended unless the problem structure is well understood. |

Omitting this field causes the engine to fall back to AutoQUBO v1 behaviour
(manual penalty weight required by the user).

**Example:**
```json
"penalty_method": "verma-lewis"
```

---

## `decision_vars`

**Type:** `object[]`

Ordered list of decision variable descriptors.
Each entry refines one association from `decision_var_associations` with
metadata the engine needs to index Q matrix rows and columns.

Order matters: the Q matrix column/row indices are assigned in the order
variables appear here, then in the order their link instances are enumerated.

### `decision_vars[].type`

**Type:** `string` — `"link"` (only supported value currently)

Declares that the decision variable corresponds to the presence or absence of
an association link instance.
Future values may include `"attribute"` for binary-encoded integer attributes.

### `decision_vars[].association`

**Type:** `string`

Name of the association (or association class) in the `.use` model.
Must also appear in `decision_var_associations`.

### `decision_vars[].domain`

**Type:** `string[]` — exactly two elements

Names of the two participating classes, in the order `[source, target]`.
Used by the engine to enumerate link instances and label Q matrix axes.

**Example:** `["Route", "Road"]` enumerates all `(Route, Road)` link pairs
in `RouteRoad`.

### `decision_vars[].index_attribute`

**Type:** `string` (optional)

Name of an integer attribute on the association class that provides an explicit
positional index for each link instance.
Required when the association is an **association class** with a step or
position attribute.

Without this field the engine assigns indices by enumeration order, which is
non-deterministic across runs and incompatible with AutoQUBO v2 symbolic
sampling (which requires stable, instance-independent indices).

When present, the engine uses the attribute value as the column/row index
within the variable group, enabling symbolic Q matrix generation for entire
problem classes rather than per-instance sampling.

Not used by this example: neither `RouteRoad` nor `FuelSlack` carries an
attribute suitable as a positional index, and none is needed since neither
`edgeCost()` nor `fuelPenalty()` depend on link insertion order (only on
which links are selected).

---

## `objective`

**Type:** `object`

Defines the cost function `c(x)` evaluated by the USE OCL engine.
The expression must be computable from the decision variable links and model
attributes; it must **not** reference manually-set derived attributes such as
`Route.totalTravelTime`, as those values are unavailable during symbolic
sampling.

### `objective.expression`

**Type:** `string` (OCL expression)

OCL expression evaluated by the USE OCL evaluator against the live model
instance.
Must return a numeric value (`Integer` or `Real`).

The expression should reference decision variable associations directly so that
AutoQUBO can compute Q matrix coefficients by sampling.

**Current expression (GarbageTruckRouting):**
```ocl
Route.allInstances->collect(r | r.edgeCost())->sum()
+ 1000 * GarbageBin.allInstances->collect(b | b.coveragePenalty())->sum()
+ 1000 * Route.allInstances->collect(r | r.shapePenalty())->sum()
+ 1000 * Route.allInstances->collect(r | r.fuelPenalty())->sum()
```

The first term (`Route::edgeCost()`) sums `travelTime` directly over every
`RouteRoad`-selected `Road` edge for that route — a genuine LINEAR
(degree-1) sum of decision variables. This replaces an earlier
`RouteStop(Route,Node)`-based "induced subgraph" formulation that inferred
edge use from both endpoint nodes being selected, which over-counted
shortcut edges once their endpoints were selected for other reasons.
Selecting edges directly (`RouteRoad(Route,Road)`) removes that bug
structurally rather than documenting it as a trade-off.

The second and third terms (`coveragePenalty()`, `shapePenalty()`) are
**manually-weighted penalty terms**, not automatic per-invariant penalties.
Both aggregate an `exists`/OR across every `Road` edge incident to a given
node, over the `RouteRoad` decision variable — a step function over
potentially more than two decision variables, which `QuboEngine` cannot
guarantee to represent exactly as a degree-2 polynomial (see its class
Javadoc). Unlike the previous `RouteStop(Route,Node)` encoding — where
coverage collapsed to reading a single decision variable per node in this
one-route scenario — a node under edge-based decision variables typically
has 2+ incident `Road`s, so this exactness collapse no longer applies even
here. This is a disclosed trade-off of the `RouteRoad` encoding, not hidden;
validate actual exactness by running the CLI/tests rather than assuming it.

The fourth term (`Route::fuelPenalty()`) enforces the truck's fuel-range
budget via a **slack-variable encoding** (Lucas 2014, "Ising formulations of
many NP problems", Sec 2.3), *not* a hinge `max(0, edgeCost() - fuelRange)`
— a hinge is a threshold function over a quadratic quantity and therefore
not itself degree-2 exact under any encoding. `FuelSlack(Route,SlackUnit)`
is a third decision variable: `SlackUnit` instances `bit0..bit6` carry
weights `1,2,4,...,64` (a 7-bit binary expansion covering 0..127, ≥ this
scenario's `fuelRange` of 100). `fuelPenalty()` computes
`gap = fuelRange - edgeCost() - (sum of selected bit weights)` and returns
`gap * gap`. Since `edgeCost()` is linear in the `RouteRoad` bits and the
slack sum is linear in the `FuelSlack` bits, `gap²` expands to only
pairwise products of decision variables — genuinely degree-2 exact, not an
approximation. `QuboEngine`'s pairwise sampling loops over all flattened
decision variables regardless of source association, so the
`RouteRoad`/`FuelSlack` cross-terms introduced by squaring `gap` are picked
up with no plugin code change.

The weight `1000` on the coverage/shape/fuel penalty terms was chosen
empirically for the previous (non-exact) objective and has **not** been
re-validated against this export — re-tune per scenario and validate by
deriving `qubo.json` and inspecting `QuboEngine`'s exactness diagnostics
(and, for coverage/shape specifically, by annealing the export), since it is
not derived automatically like the invariant penalty weight `B` computed by
`QuboEngine.computePenaltyWeight`. Note `fuelPenalty()` is already a squared
magnitude, so its weight may need separate tuning from the other three terms
rather than sharing the same `1000`.
The expression depends only on `RouteRoad`/`AssignedTo`/`FuelSlack` links and
static model attributes — all derivable from decision variables — so
AutoQUBO can sample it symbolically.

**Model-defined helpers:** operations declared on model classes in the `.use` file
can be called from the objective expression.
AutoQUBO evaluates them through the USE OCL engine at sampling time, so they are
transparent to the QUBO derivation as long as they ultimately reference only
decision variable associations and static model attributes.
Encapsulating navigation sub-expressions in `.use` operations reduces objective
length and makes the cost structure easier to audit.

**What not to write:**
```ocl
-- BAD: totalTravelTime is set manually; AutoQUBO cannot sample it.
Route.allInstances->collect(r | r.totalTravelTime)->sum()
```

### `objective.minimise`

**Type:** `boolean`

`true` — minimise the objective (e.g. shortest total travel time).
`false` — maximise the objective (e.g. maximum coverage).

The engine negates the expression before constructing the Q matrix when
`false`, since QUBO solvers always minimise `x^T Q x`.

**Example:**
```json
"objective": {
  "expression": "...",
  "minimise": true
}
```

---

## Constraints and penalty encoding

OCL invariants in the `.use` model are **not** automatically read from this
file.
The engine encodes constraints as QUBO penalty terms separately, using the
`penalty_method` weight to scale them.

Constraints that operate directly on `Real`-valued attributes (e.g.
`Truck.currentLoad` vs `maxCapacity`) cannot be directly encoded in the
binary Q matrix and are instead verified post-solve against the decoded
solution, not enforced during annealing; this is a known scope limitation
for truck capacity. Fuel-range feasibility, in contrast, *is* encoded
directly in the objective via the slack-variable technique described above
(`Route::fuelPenalty()`), since a linear quantity (`edgeCost()`) compared
against a constant threshold can be turned into a genuinely degree-2-exact
penalty with slack bits; the same technique could in principle be applied
to `Truck.currentLoad <= maxCapacity`, left as future work.
