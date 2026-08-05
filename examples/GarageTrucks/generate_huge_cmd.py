import sys

# ===========================================================
# Huge scenario generator: 10 trucks, 10 routes, 200 bins, 500 roads.
#
# Topology: one shared depot and one shared disposal facility, plus
# 10 independent "lanes" of 20 intersections each (one lane per
# truck/route). Lane t's route is the simple path
#   depot -> laneT_1 -> laneT_2 -> ... -> laneT_20 -> disposal
# (21 edges, travelTime 1.0 each -> edgeCost() = 21.0 per route).
# Each of the 20 lane nodes hosts exactly one garbage bin (200 bins
# total). Lanes share only the depot/disposal nodes, so routes never
# interfere with each other's flow-conservation invariant.
#
# The 210 "spine" edges above are padded with 290 unused decorative
# roads (29 per lane: 18 skip-two shortcuts + 11 depot shortcuts) to
# reach exactly 500 Road instances. Decorative roads are never
# inserted into RouteRoad, so they do not affect any route invariant;
# they only need positiveTravelTime > 0.
# ===========================================================

NUM_LANES = 10
LANE_LEN = 20  # bins/intersections per lane

lines = []
def L(s=""):
    lines.append(s)

DEPOT_ID = 1
DISPOSAL_ID = 2 + NUM_LANES * LANE_LEN  # 202

def lane_node_id(t, k):
    # t in 1..NUM_LANES, k in 1..LANE_LEN
    return 1 + (t - 1) * LANE_LEN + k  # 2 .. 201

def nodename(i):
    if i == DEPOT_ID:
        return "depot"
    if i == DISPOSAL_ID:
        return "disposal"
    return f"n{i}"

L("-- ===========================================================")
L("-- GarbageTruckRoutingHuge.cmd")
L("-- Animation script for the GarbageTruckRouting USE model.")
L("--")
L(f"-- Scenario: {2 + NUM_LANES * LANE_LEN} nodes (1 depot, {NUM_LANES * LANE_LEN} intersections, 1 disposal),")
L(f"--           200 garbage bins, {NUM_LANES} trucks, {NUM_LANES} routes, 500 roads.")
L("--")
L("-- Topology: 10 independent lanes of 20 intersections each, sharing only")
L("-- the depot and disposal nodes. Route t is the simple path")
L("-- depot -> laneT_1 -> ... -> laneT_20 -> disposal (21 edges, travelTime")
L("-- 1.0 each, edgeCost() = 21.0), so lanes never interact through the")
L("-- routeConnected flow-conservation invariant. Each lane node hosts one")
L("-- garbage bin (200 bins total, one per route). Truck t is AssignedTo")
L("-- route t only, so routeBinLoad()/edgeCost() per route are compared")
L("-- against that single truck's maxCapacity/fuelRange.")
L("--")
L("-- The 210 spine edges are padded with 290 unused decorative roads (29")
L("-- per lane: 18 skip-two shortcuts laneT_i->laneT_(i+2), plus 11 depot")
L("-- shortcuts depot->laneT_k for k=2..12) to reach exactly 500 Road")
L("-- instances. Decorative roads are never selected into RouteRoad, so")
L("-- they only need positiveTravelTime > 0 and do not affect feasibility.")
L("--")
L("-- Load with: open GarbageTruckRoutingHuge.cmd")
L("-- ===========================================================")
L()

# -----------------------------------------------------------
# 1. Create city nodes
# -----------------------------------------------------------
L("-- -----------------------------------------------------------")
L("-- 1. Create city nodes")
L("-- -----------------------------------------------------------")
L("!create depot       : Node")
L(f"!set depot.nodeId   := {DEPOT_ID}")
L("!set depot.nodeType := #Depot")
L()
for t in range(1, NUM_LANES + 1):
    for k in range(1, LANE_LEN + 1):
        i = lane_node_id(t, k)
        L(f"!create n{i}       : Node")
        L(f"!set n{i}.nodeId   := {i}")
        L(f"!set n{i}.nodeType := #Intersection")
L()
L("!create disposal       : Node")
L(f"!set disposal.nodeId   := {DISPOSAL_ID}")
L("!set disposal.nodeType := #DisposalFacility")
L()

# -----------------------------------------------------------
# 2. Create road network
# -----------------------------------------------------------
L("-- -----------------------------------------------------------")
L("-- 2. Create road network (directed edges with travel times)")
L("-- -----------------------------------------------------------")

road_edges = []  # (origin_name, dest_name, travelTime, is_spine)

for t in range(1, NUM_LANES + 1):
    lane = [lane_node_id(t, k) for k in range(1, LANE_LEN + 1)]
    # spine: depot -> lane[0] -> lane[1] -> ... -> lane[-1] -> disposal
    road_edges.append((nodename(DEPOT_ID), nodename(lane[0]), 1.0, True))
    for k in range(LANE_LEN - 1):
        road_edges.append((nodename(lane[k]), nodename(lane[k + 1]), 1.0, True))
    road_edges.append((nodename(lane[-1]), nodename(DISPOSAL_ID), 1.0, True))

    # decorative skip-two shortcuts (18 per lane), unused by any route
    for k in range(LANE_LEN - 2):
        road_edges.append((nodename(lane[k]), nodename(lane[k + 2]), 2.0, False))

    # decorative depot shortcuts (11 per lane), unused by any route
    for k in range(2, 13):  # k = 2..12 inclusive -> 11 shortcuts
        road_edges.append((nodename(DEPOT_ID), nodename(lane[k - 1]), 3.0, False))

assert len(road_edges) == 500, len(road_edges)
spine_count = sum(1 for e in road_edges if e[3])
assert spine_count == NUM_LANES * (LANE_LEN + 1), spine_count

for (an, bn, tt, _) in road_edges:
    L(f"!insert ({an}, {bn}) into Road")
    L(f"!set Road.allInstances->any(r | r.origin = {an} and r.destination = {bn}).travelTime := {tt}")
L()

# -----------------------------------------------------------
# 3. Create garbage bins (200 total, one per lane node)
# -----------------------------------------------------------
L("-- -----------------------------------------------------------")
L("-- 3. Create garbage bins (200 total, one per lane intersection)")
L("-- -----------------------------------------------------------")

bin_of_lane_node = {}  # lane_node_id -> bin name
fills_by_lane = {}

bin_idx = 1
for t in range(1, NUM_LANES + 1):
    fills = []
    for k in range(1, LANE_LEN + 1):
        node_id = lane_node_id(t, k)
        name = f"bin{bin_idx}"
        # deterministic fill pattern in [0.10, 0.90], varies per lane/position
        fill = round(0.10 + 0.04 * ((t + k) % 20), 2)
        fills.append(fill)
        bin_of_lane_node[node_id] = name
        L(f"!create {name}       : GarbageBin")
        L(f"!set {name}.maxFill     := 1.0")
        L(f"!set {name}.currentFill := {fill}")
        bin_idx += 1
    fills_by_lane[t] = fills
L()
assert bin_idx - 1 == 200

L("-- Place bins at their lane intersection")
for node_id, name in bin_of_lane_node.items():
    L(f"!insert ({name}, {nodename(node_id)}) into LocatedAt")
L()

# -----------------------------------------------------------
# 4. Create trucks (10, one per lane/route)
# -----------------------------------------------------------
L("-- -----------------------------------------------------------")
L("-- 4. Create trucks (10, one per lane/route)")
L("-- -----------------------------------------------------------")
for t in range(1, NUM_LANES + 1):
    L(f"!create truck{t} : Truck")
    L(f"!set truck{t}.truckId     := {t}")
    L(f"!set truck{t}.fuelRange   := 50.0  -- edgeCost() per route is 21.0")
    L(f"!set truck{t}.maxCapacity := 20.0  -- >= any lane's 20-bin fill sum")
    L(f"!set truck{t}.currentLoad := 0.0")
    L()

# -----------------------------------------------------------
# 5. Create the 10 routes
# -----------------------------------------------------------
L("-- -----------------------------------------------------------")
L("-- 5. Create the 10 routes")
L("-- -----------------------------------------------------------")
for t in range(1, NUM_LANES + 1):
    lane = [lane_node_id(t, k) for k in range(1, LANE_LEN + 1)]
    path = [DEPOT_ID] + lane + [DISPOSAL_ID]
    L(f"!create route{t} : Route")
    L(f"!set route{t}.totalTravelTime := 21.0   -- 21 x 1.0 spine edges")
    L()
    L(f"-- Assign route{t} to truck{t}")
    L(f"!insert (route{t}, truck{t}) into AssignedTo")
    L()
    L(f"-- Route{t}: select the spine edges it uses (RouteRoad is the decision variable)")
    for i in range(len(path) - 1):
        an, bn = nodename(path[i]), nodename(path[i + 1])
        L(f"!insert (route{t}, Road.allInstances->any(r | r.origin = {an} and r.destination = {bn})) into RouteRoad")
    L()

# -----------------------------------------------------------
# 6. Simulate garbage collection along each route
# -----------------------------------------------------------
L("-- -----------------------------------------------------------")
L("-- 6. Simulate garbage collection along each route")
L("-- -----------------------------------------------------------")
for t in range(1, NUM_LANES + 1):
    L(f"-- Truck{t} collects all 20 bins on lane {t}")
    for k in range(1, LANE_LEN + 1):
        node_id = lane_node_id(t, k)
        name = bin_of_lane_node[node_id]
        L(f"!openter truck{t} collectGarbage({name})")
        L(f"!set truck{t}.currentLoad := truck{t}.currentLoad + {name}.currentFill")
        L(f"!set {name}.currentFill   := 0.0")
        L("!opexit")
    L()

# -----------------------------------------------------------
# 7. Check all constraints
# -----------------------------------------------------------
L("-- -----------------------------------------------------------")
L("-- 7. Check all constraints")
L("-- -----------------------------------------------------------")
L("check")
L()
L("-- Expected: all invariants true.")
for t in range(1, NUM_LANES + 1):
    total_fill = round(sum(fills_by_lane[t]), 2)
    L(f"-- truck{t}.currentLoad = {total_fill} m3  (<= maxCapacity 20.0)")
L("-- Each route: edgeCost() = 21.0 min; fuelPenalty-relevant check: 21.0 <= fuelRange 50.0")
L("-- All 200 bins have currentFill = 0 (collected)")
L("-- All 10 trucks assigned exactly one route; 500 Road instances total")
L("--   (210 spine edges used by RouteRoad + 290 unused decorative edges)")

out = "\n".join(lines) + "\n"
with open(sys.argv[1], "w", newline="\n") as f:
    f.write(out)

node_count = 2 + NUM_LANES * LANE_LEN
print("nodes total:", node_count)
print("roads total:", len(road_edges))
print("bins total:", bin_idx - 1)
print("trucks:", NUM_LANES)
print("routes:", NUM_LANES)
for t in range(1, NUM_LANES + 1):
    print(f"lane {t} bin fill sum:", round(sum(fills_by_lane[t]), 2))
