import sys

ROWS, COLS = 5, 10
def nid(r, c):  # 1-indexed row/col -> node id 1..50
    return (r - 1) * COLS + c

DEPOT = nid(1, 1)
DISPOSAL = nid(ROWS, COLS)

lines = []
def L(s=""):
    lines.append(s)

L("-- ===========================================================")
L("-- GarbageTruckRoutingV2.cmd")
L("-- Animation script for the GarbageTruckRouting USE model.")
L("--")
L(f"-- Scenario: {ROWS*COLS} nodes (1 depot, {ROWS*COLS-2} intersections, 1 disposal)")
L("--           laid out as a 5x10 directed grid (road = +1 right / +10 down),")
L("--           20 garbage bins, 3 trucks, all 3 assigned disjoint monotone")
L("--           staircase routes from depot (node 1) to disposal (node 50).")
L("-- Each route uses 9 horizontal + 4 vertical road segments (60 min total),")
L("-- matching the grid's only two travelTime values (4.0 horiz / 6.0 vert).")
L("-- RouteRoad(Route,Road) is the decision variable: each route's edges are")
L("-- selected directly below, so edgeCost() reads back exactly 60.0 with no")
L("-- shortcut-over-counting ambiguity (this grid has no shortcut edges).")
L("--")
L("-- Load with: open GarbageTruckRoutingV2.cmd")
L("-- ===========================================================")
L()
L("-- -----------------------------------------------------------")
L("-- 1. Create city nodes (5x10 grid, id = (row-1)*10 + col)")
L("-- -----------------------------------------------------------")
L("!create depot       : Node")
L(f"!set depot.nodeId   := {DEPOT}")
L("!set depot.nodeType := #Depot")
L()
for r in range(1, ROWS + 1):
    for c in range(1, COLS + 1):
        i = nid(r, c)
        if i in (DEPOT, DISPOSAL):
            continue
        L(f"!create n{i}       : Node")
        L(f"!set n{i}.nodeId   := {i}")
        L(f"!set n{i}.nodeType := #Intersection")
L()
L("!create disposal       : Node")
L(f"!set disposal.nodeId   := {DISPOSAL}")
L("!set disposal.nodeType := #DisposalFacility")
L()

def nodename(i):
    if i == DEPOT:
        return "depot"
    if i == DISPOSAL:
        return "disposal"
    return f"n{i}"

L("-- -----------------------------------------------------------")
L("-- 2. Create road network (directed edges with travel times)")
L("-- -----------------------------------------------------------")
L("-- Horizontal roads (same row, col -> col+1), travelTime 4.0")
for r in range(1, ROWS + 1):
    for c in range(1, COLS):
        a, b = nid(r, c), nid(r, c + 1)
        an, bn = nodename(a), nodename(b)
        L(f"!insert ({an}, {bn}) into Road")
        L(f"!set Road.allInstances->any(r | r.origin = {an} and r.destination = {bn}).travelTime := 4.0")
L()
L("-- Vertical roads (same col, row -> row+1), travelTime 6.0")
for r in range(1, ROWS):
    for c in range(1, COLS + 1):
        a, b = nid(r, c), nid(r + 1, c)
        an, bn = nodename(a), nodename(b)
        L(f"!insert ({an}, {bn}) into Road")
        L(f"!set Road.allInstances->any(r | r.origin = {an} and r.destination = {bn}).travelTime := 6.0")
L()

# Routes as (r,c) move sequences from (1,1) to (5,10)
def apply_moves(moves):
    r, c = 1, 1
    path = [(r, c)]
    for m in moves:
        if m == 'R':
            c += 1
        else:
            r += 1
        path.append((r, c))
    return [nid(r, c) for r, c in path]

route1_moves = "RRDRRDRRDRRDR"
route2_moves = "DRRDRRDRRDRRR"
route3_moves = "RDRDRDRDRRRRR"

route1 = apply_moves(route1_moves)
route2 = apply_moves(route2_moves)
route3 = apply_moves(route3_moves)

assert route1[0] == DEPOT and route1[-1] == DISPOSAL
assert route2[0] == DEPOT and route2[-1] == DISPOSAL
assert route3[0] == DEPOT and route3[-1] == DISPOSAL
assert route1_moves.count('R') == 9 and route1_moves.count('D') == 4
assert route2_moves.count('R') == 9 and route2_moves.count('D') == 4
assert route3_moves.count('R') == 9 and route3_moves.count('D') == 4

bin_ids_r1 = [2, 3, 14, 15, 26, 37, 39]
bin_ids_r2 = [11, 12, 23, 24, 35, 36, 48]
bin_ids_r3 = [13, 34, 45, 46, 47, 49]

for b in bin_ids_r1:
    assert b in route1, b
for b in bin_ids_r2:
    assert b in route2, b
for b in bin_ids_r3:
    assert b in route3, b

all_bin_ids = bin_ids_r1 + bin_ids_r2 + bin_ids_r3
assert len(all_bin_ids) == 20
assert len(set(all_bin_ids)) == 20

fills_r1 = [0.0, 0.98, 0.40, 0.45, 0.50, 0.55, 0.60]
fills_r2 = [0.42, 0.48, 0.52, 0.58, 0.41, 0.47, 0.53]
fills_r3 = [0.44, 0.49, 0.51, 0.56, 0.59, 0.43]
all_fills = fills_r1 + fills_r2 + fills_r3
assert len(all_fills) == 20

L("-- -----------------------------------------------------------")
L("-- 3. Create garbage bins (20 total, 1000 L / 1.0 m3 capacity each)")
L("-- -----------------------------------------------------------")
bin_names = []
bin_of_node = {}
idx = 1
for node_id, fill in zip(all_bin_ids, all_fills):
    name = f"bin{idx}"
    bin_names.append(name)
    bin_of_node[node_id] = name
    L(f"!create {name}       : GarbageBin")
    L(f"!set {name}.maxFill     := 1.0")
    L(f"!set {name}.currentFill := {fill}")
    idx += 1
L()
L("-- Place bins at intersections along the three routes")
for node_id, fill in zip(all_bin_ids, all_fills):
    name = bin_of_node[node_id]
    L(f"!insert ({name}, {nodename(node_id)}) into LocatedAt")
L()

L("-- -----------------------------------------------------------")
L("-- 4. Create trucks (3, each assigned one route)")
L("-- -----------------------------------------------------------")
for t in (1, 2, 3):
    L(f"!create truck{t} : Truck")
    L(f"!set truck{t}.truckId     := {t}")
    L(f"!set truck{t}.fuelRange   := 100.0  -- can drive 100 km")
    L(f"!set truck{t}.maxCapacity := 15.0   -- holds 15 m3")
    L(f"!set truck{t}.currentLoad := 0.0")
    L()

L("-- -----------------------------------------------------------")
L("-- 5. Create the three routes")
L("-- -----------------------------------------------------------")
routes = {1: route1, 2: route2, 3: route3}
route_bins = {1: bin_ids_r1, 2: bin_ids_r2, 3: bin_ids_r3}

def edge_travel_time(a, b):
    # a,b are (row,col) grid coordinates one step apart
    return 4.0 if a[0] == b[0] else 6.0

for t, path in routes.items():
    L(f"!create route{t} : Route")
    L(f"!set route{t}.totalTravelTime := 60.0   -- 9x4.0 + 4x6.0 = 60 min")
    L()
    L(f"-- Assign route{t} to truck{t}")
    L(f"!insert (route{t}, truck{t}) into AssignedTo")
    L()
    L(f"-- Route{t}: select the edges it uses (RouteRoad is the decision variable)")
    for i in range(len(path) - 1):
        an, bn = nodename(path[i]), nodename(path[i + 1])
        L(f"!insert (route{t}, Road.allInstances->any(r | r.origin = {an} and r.destination = {bn})) into RouteRoad")
    L()

L("-- -----------------------------------------------------------")
L("-- 6. Simulate garbage collection along each route")
L("-- -----------------------------------------------------------")
for t, path in routes.items():
    L(f"-- Truck{t} collects its assigned bins")
    for node_id in route_bins[t]:
        name = bin_of_node[node_id]
        fill = all_fills[all_bin_ids.index(node_id)]
        if fill == 0.0:
            L(f"-- {name} is already empty (currentFill = 0); collectGarbage's")
            L(f"-- binNeedsCollection precondition requires currentFill > 0, so skip it.")
            continue
        L(f"!openter truck{t} collectGarbage({name})")
        L(f"!set truck{t}.currentLoad := truck{t}.currentLoad + {name}.currentFill")
        L(f"!set {name}.currentFill   := 0.0")
        L("!opexit")
    L()

L("-- -----------------------------------------------------------")
L("-- 7. Check all constraints")
L("-- -----------------------------------------------------------")
L("check")
L()
L("-- Expected: all invariants true.")
for t, path in routes.items():
    total_fill = sum(all_fills[all_bin_ids.index(n)] for n in route_bins[t])
    L(f"-- truck{t}.currentLoad = {total_fill:.2f} m3  (<= maxCapacity 15.0)")
L("-- Each route: edgeCost() = 60.0 min; fuelPenalty() = max(0, 60-100)^2 = 0")
L("-- All 20 bins have currentFill = 0 (collected, except the one already empty)")
L("-- All 3 trucks assigned a route")

out = "\n".join(lines) + "\n"
with open(sys.argv[1], "w", newline="\n") as f:
    f.write(out)

print("edges:", sum(1 for l in lines if l.startswith("!insert (") and "into Road" in l))
print("nodes total:", ROWS * COLS)
print("bins:", len(all_bin_ids))
print("route1 total time check:", route1_moves.count('R')*4 + route1_moves.count('D')*6)
print("route2 total time check:", route2_moves.count('R')*4 + route2_moves.count('D')*6)
print("route3 total time check:", route3_moves.count('R')*4 + route3_moves.count('D')*6)
