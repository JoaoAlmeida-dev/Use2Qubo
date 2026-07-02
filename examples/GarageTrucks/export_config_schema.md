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
"decision_var_associations": ["RouteStop", "AssignedTo"]
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

**Example:** `["Route", "Node"]` enumerates all `(Route, Node)` link pairs
in `RouteStop`.

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

**Example:** `"index_attribute": "step"` on `RouteStop` produces Q matrix
entries indexed by `(route_id, step, node_id)`.

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
RouteStop.allInstances
  ->select(rs | rs.step < rs.routes.stops->size() - 1)
  ->collect(rs | rs.roadToNext().travelTime)
  ->sum()
```

This computes total travel time by summing `Road.travelTime` across all
consecutive `RouteStop` pairs, indexed by `step`.
`roadToNext()` is a helper operation defined on `RouteStop` in the `.use` model;
it locates the `Road` between `step` and `step + 1` on the same route.
The expression depends only on `RouteStop` links and `Road` weights — both
derivable from decision variables — so AutoQUBO can sample it symbolically.

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

Constraints that operate on `Real`-valued attributes (`Truck.currentLoad`,
`Truck.distanceTravelled`) cannot be directly encoded in the binary Q matrix.
They are verified post-solve against the decoded solution, not enforced during
annealing.
This is a known scope limitation; full binary encoding of capacity and fuel
constraints is future work.
