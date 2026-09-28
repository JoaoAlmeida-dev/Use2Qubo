import sys

# ===========================================================
# Huge scenario generator for GarageTrucksTSPHuge.
#
# NUM_LANES independent lanes, each with 3 waypoints (bin locations),
# sharing one depot and one disposal facility. Every lane reuses the same
# relative metric-closure distances as the base GarageTrucksTSP example
# (depot->w2=5, depot->w3=13, depot->w4=19, w2-w3=8, w2-w4=14, w3-w4=6,
# w2->disposal=20, w3->disposal=15, w4->disposal=11), so each lane's own
# optimum is the same order (w2,w3,w4) at cost 30, and the global optimum is
# NUM_LANES * 30.
#
# Total decision vars (VisitAt = Waypoint x Position, full cross product
# regardless of lane -- see GarageTrucksTSPHuge.use's header note) =
# (3 * NUM_LANES)^2 = 9 * NUM_LANES^2. Kept at NUM_LANES=4 (144 decision
# vars) to stay tractable against PolySampler's unpruned C(n,3)
# exactness-check enumeration.
# ===========================================================

NUM_LANES = 6

lines = []
def L(s=""):
    lines.append(s)

# Per-lane relative distances, reused identically from GarageTrucksTSP.cmd.
DEPOT_TO = {2: 5.0, 3: 13.0, 4: 19.0}          # depot -> w{k}
PAIR = {(2, 3): 8.0, (3, 2): 8.0, (2, 4): 14.0, (4, 2): 14.0, (3, 4): 6.0, (4, 3): 6.0}
TO_DISPOSAL = {2: 20.0, 3: 15.0, 4: 11.0}      # w{k} -> disposal

def wname(t, k):
    return f"w{t}_{k}"

def pname(t, i):
    return f"pos{t}_{i}"

L("-- ===========================================================")
L("-- GarageTrucksTSPHuge.cmd")
L(f"-- Animation script for GarageTrucksTSPHuge: {NUM_LANES} independent lanes,")
L("-- 3 waypoints each, shared depot/disposal. See generate_huge_cmd.py.")
L("--")
L(f"-- Total decision vars (VisitAt full cross product): 9 * {NUM_LANES}^2 = {9 * NUM_LANES * NUM_LANES}.")
L(f"-- Global optimum: {NUM_LANES} lanes x 30.0 (each lane's own optimal order) = {NUM_LANES * 30.0}.")
L("--")
L("-- Load with: open GarageTrucksTSPHuge.cmd")
L("-- ===========================================================")
L()

# -----------------------------------------------------------
# 1. Terminals: shared depot/disposal, NUM_LANES x 3 waypoints
# -----------------------------------------------------------
L("-- -----------------------------------------------------------")
L("-- 1. Terminals")
L("-- -----------------------------------------------------------")
L("!create depot : Terminal")
L("!set depot.terminalId   := 1")
L("!set depot.terminalType := #Depot")
L()
L("!create disposal : Terminal")
L("!set disposal.terminalId   := 2")
L("!set disposal.terminalType := #DisposalFacility")
L()

wp_id = 100
for t in range(1, NUM_LANES + 1):
    for k in (2, 3, 4):
        wp_id += 1
        name = wname(t, k)
        L(f"!create {name} : Waypoint")
        L(f"!set {name}.terminalId   := {wp_id}")
        L(f"!set {name}.terminalType := #Waypoint")
        L(f"!set {name}.lane         := {t}")
L()

# -----------------------------------------------------------
# 2. Positions: NUM_LANES x 3 (idx 1..3 per lane)
# -----------------------------------------------------------
L("-- -----------------------------------------------------------")
L("-- 2. Positions")
L("-- -----------------------------------------------------------")
for t in range(1, NUM_LANES + 1):
    for i in (1, 2, 3):
        name = pname(t, i)
        L(f"!create {name} : Position")
        L(f"!set {name}.idx  := {i}")
        L(f"!set {name}.lane := {t}")
L()

# -----------------------------------------------------------
# 3. TerminalDistance: metric closure, per lane (depot/disposal shared)
# -----------------------------------------------------------
L("-- -----------------------------------------------------------")
L("-- 3. TerminalDistance (per-lane metric closure, depot/disposal shared)")
L("-- -----------------------------------------------------------")

def insert_td(a, b, cost):
    L(f"!insert ({a}, {b}) into TerminalDistance")
    L(f"!set TerminalDistance.allInstances->any(t | t.fromTerminal = {a} and t.toTerminal = {b}).cost := {cost}")

for t in range(1, NUM_LANES + 1):
    for k, cost in DEPOT_TO.items():
        insert_td("depot", wname(t, k), cost)
    for (k1, k2), cost in PAIR.items():
        insert_td(wname(t, k1), wname(t, k2), cost)
    for k, cost in TO_DISPOSAL.items():
        insert_td(wname(t, k), "disposal", cost)
L()

# -----------------------------------------------------------
# 4. PositionConflict / WaypointConflict: within-lane pairs only
# -----------------------------------------------------------
L("-- -----------------------------------------------------------")
L("-- 4. PositionConflict / WaypointConflict (within-lane pairs only --")
L("--    cross-lane pairs are never populated, so they never constrain")
L("--    anything; a cross-lane VisitAt link is still a possible decision")
L("--    var, just one the lane-scoped coverage invariants correctly")
L("--    treat as not covering its own lane.)")
L("-- -----------------------------------------------------------")
waypoint_pairs = [(2, 3), (2, 4), (3, 4)]
position_pairs = [(1, 2), (1, 3), (2, 3)]
for t in range(1, NUM_LANES + 1):
    for (k1, k2) in waypoint_pairs:
        for i in (1, 2, 3):
            L(f"!insert ({wname(t, k1)}, {wname(t, k2)}, {pname(t, i)}) into PositionConflict")
    for (i1, i2) in position_pairs:
        for k in (2, 3, 4):
            L(f"!insert ({pname(t, i1)}, {pname(t, i2)}, {wname(t, k)}) into WaypointConflict")
L()

# -----------------------------------------------------------
# 5. Schedule singleton
# -----------------------------------------------------------
L("-- -----------------------------------------------------------")
L("-- 5. Schedule singleton (objective host)")
L("-- -----------------------------------------------------------")
L("!create sched : Schedule")
L()

# -----------------------------------------------------------
# 6. Animated solution: every lane visits w2 -> w3 -> w4 (each lane's own
#    optimum, cost 30 per lane).
# -----------------------------------------------------------
L("-- -----------------------------------------------------------")
L("-- 6. Animated solution: every lane at its own optimum (w2,w3,w4 order)")
L("-- -----------------------------------------------------------")
for t in range(1, NUM_LANES + 1):
    L(f"!insert ({wname(t, 2)}, {pname(t, 1)}) into VisitAt")
    L(f"!insert ({wname(t, 3)}, {pname(t, 2)}) into VisitAt")
    L(f"!insert ({wname(t, 4)}, {pname(t, 3)}) into VisitAt")
L()

L("-- -----------------------------------------------------------")
L("-- 7. Check all constraints")
L("-- -----------------------------------------------------------")
L("check")
L()
L(f"-- Expected: all invariants true. sched.totalCost() = {NUM_LANES} x 30.0 = {NUM_LANES * 30.0}.")

out = "\n".join(lines) + "\n"
with open(sys.argv[1], "w", newline="\n") as f:
    f.write(out)

print("lanes:", NUM_LANES)
print("waypoints:", NUM_LANES * 3)
print("positions:", NUM_LANES * 3)
print("decision vars (VisitAt full cross product):", (NUM_LANES * 3) ** 2)
print("expected total cost:", NUM_LANES * 30.0)
