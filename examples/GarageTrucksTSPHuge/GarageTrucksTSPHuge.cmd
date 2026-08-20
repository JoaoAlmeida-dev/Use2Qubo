-- ===========================================================
-- GarageTrucksTSPHuge.cmd
-- Animation script for GarageTrucksTSPHuge: 6 independent lanes,
-- 3 waypoints each, shared depot/disposal. See generate_huge_cmd.py.
--
-- Total decision vars (VisitAt full cross product): 9 * 6^2 = 324.
-- Global optimum: 6 lanes x 30.0 (each lane's own optimal order) = 180.0.
--
-- Load with: open GarageTrucksTSPHuge.cmd
-- ===========================================================

-- -----------------------------------------------------------
-- 1. Terminals
-- -----------------------------------------------------------
!create depot : Terminal
!set depot.terminalId   := 1
!set depot.terminalType := #Depot

!create disposal : Terminal
!set disposal.terminalId   := 2
!set disposal.terminalType := #DisposalFacility

!create w1_2 : Waypoint
!set w1_2.terminalId   := 101
!set w1_2.terminalType := #Waypoint
!set w1_2.lane         := 1
!create w1_3 : Waypoint
!set w1_3.terminalId   := 102
!set w1_3.terminalType := #Waypoint
!set w1_3.lane         := 1
!create w1_4 : Waypoint
!set w1_4.terminalId   := 103
!set w1_4.terminalType := #Waypoint
!set w1_4.lane         := 1
!create w2_2 : Waypoint
!set w2_2.terminalId   := 104
!set w2_2.terminalType := #Waypoint
!set w2_2.lane         := 2
!create w2_3 : Waypoint
!set w2_3.terminalId   := 105
!set w2_3.terminalType := #Waypoint
!set w2_3.lane         := 2
!create w2_4 : Waypoint
!set w2_4.terminalId   := 106
!set w2_4.terminalType := #Waypoint
!set w2_4.lane         := 2
!create w3_2 : Waypoint
!set w3_2.terminalId   := 107
!set w3_2.terminalType := #Waypoint
!set w3_2.lane         := 3
!create w3_3 : Waypoint
!set w3_3.terminalId   := 108
!set w3_3.terminalType := #Waypoint
!set w3_3.lane         := 3
!create w3_4 : Waypoint
!set w3_4.terminalId   := 109
!set w3_4.terminalType := #Waypoint
!set w3_4.lane         := 3
!create w4_2 : Waypoint
!set w4_2.terminalId   := 110
!set w4_2.terminalType := #Waypoint
!set w4_2.lane         := 4
!create w4_3 : Waypoint
!set w4_3.terminalId   := 111
!set w4_3.terminalType := #Waypoint
!set w4_3.lane         := 4
!create w4_4 : Waypoint
!set w4_4.terminalId   := 112
!set w4_4.terminalType := #Waypoint
!set w4_4.lane         := 4
!create w5_2 : Waypoint
!set w5_2.terminalId   := 113
!set w5_2.terminalType := #Waypoint
!set w5_2.lane         := 5
!create w5_3 : Waypoint
!set w5_3.terminalId   := 114
!set w5_3.terminalType := #Waypoint
!set w5_3.lane         := 5
!create w5_4 : Waypoint
!set w5_4.terminalId   := 115
!set w5_4.terminalType := #Waypoint
!set w5_4.lane         := 5
!create w6_2 : Waypoint
!set w6_2.terminalId   := 116
!set w6_2.terminalType := #Waypoint
!set w6_2.lane         := 6
!create w6_3 : Waypoint
!set w6_3.terminalId   := 117
!set w6_3.terminalType := #Waypoint
!set w6_3.lane         := 6
!create w6_4 : Waypoint
!set w6_4.terminalId   := 118
!set w6_4.terminalType := #Waypoint
!set w6_4.lane         := 6

-- -----------------------------------------------------------
-- 2. Positions
-- -----------------------------------------------------------
!create pos1_1 : Position
!set pos1_1.idx  := 1
!set pos1_1.lane := 1
!create pos1_2 : Position
!set pos1_2.idx  := 2
!set pos1_2.lane := 1
!create pos1_3 : Position
!set pos1_3.idx  := 3
!set pos1_3.lane := 1
!create pos2_1 : Position
!set pos2_1.idx  := 1
!set pos2_1.lane := 2
!create pos2_2 : Position
!set pos2_2.idx  := 2
!set pos2_2.lane := 2
!create pos2_3 : Position
!set pos2_3.idx  := 3
!set pos2_3.lane := 2
!create pos3_1 : Position
!set pos3_1.idx  := 1
!set pos3_1.lane := 3
!create pos3_2 : Position
!set pos3_2.idx  := 2
!set pos3_2.lane := 3
!create pos3_3 : Position
!set pos3_3.idx  := 3
!set pos3_3.lane := 3
!create pos4_1 : Position
!set pos4_1.idx  := 1
!set pos4_1.lane := 4
!create pos4_2 : Position
!set pos4_2.idx  := 2
!set pos4_2.lane := 4
!create pos4_3 : Position
!set pos4_3.idx  := 3
!set pos4_3.lane := 4
!create pos5_1 : Position
!set pos5_1.idx  := 1
!set pos5_1.lane := 5
!create pos5_2 : Position
!set pos5_2.idx  := 2
!set pos5_2.lane := 5
!create pos5_3 : Position
!set pos5_3.idx  := 3
!set pos5_3.lane := 5
!create pos6_1 : Position
!set pos6_1.idx  := 1
!set pos6_1.lane := 6
!create pos6_2 : Position
!set pos6_2.idx  := 2
!set pos6_2.lane := 6
!create pos6_3 : Position
!set pos6_3.idx  := 3
!set pos6_3.lane := 6

-- -----------------------------------------------------------
-- 3. TerminalDistance (per-lane metric closure, depot/disposal shared)
-- -----------------------------------------------------------
!insert (depot, w1_2) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = depot and t.toTerminal = w1_2).cost := 5.0
!insert (depot, w1_3) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = depot and t.toTerminal = w1_3).cost := 13.0
!insert (depot, w1_4) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = depot and t.toTerminal = w1_4).cost := 19.0
!insert (w1_2, w1_3) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = w1_2 and t.toTerminal = w1_3).cost := 8.0
!insert (w1_3, w1_2) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = w1_3 and t.toTerminal = w1_2).cost := 8.0
!insert (w1_2, w1_4) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = w1_2 and t.toTerminal = w1_4).cost := 14.0
!insert (w1_4, w1_2) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = w1_4 and t.toTerminal = w1_2).cost := 14.0
!insert (w1_3, w1_4) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = w1_3 and t.toTerminal = w1_4).cost := 6.0
!insert (w1_4, w1_3) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = w1_4 and t.toTerminal = w1_3).cost := 6.0
!insert (w1_2, disposal) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = w1_2 and t.toTerminal = disposal).cost := 20.0
!insert (w1_3, disposal) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = w1_3 and t.toTerminal = disposal).cost := 15.0
!insert (w1_4, disposal) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = w1_4 and t.toTerminal = disposal).cost := 11.0
!insert (depot, w2_2) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = depot and t.toTerminal = w2_2).cost := 5.0
!insert (depot, w2_3) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = depot and t.toTerminal = w2_3).cost := 13.0
!insert (depot, w2_4) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = depot and t.toTerminal = w2_4).cost := 19.0
!insert (w2_2, w2_3) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = w2_2 and t.toTerminal = w2_3).cost := 8.0
!insert (w2_3, w2_2) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = w2_3 and t.toTerminal = w2_2).cost := 8.0
!insert (w2_2, w2_4) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = w2_2 and t.toTerminal = w2_4).cost := 14.0
!insert (w2_4, w2_2) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = w2_4 and t.toTerminal = w2_2).cost := 14.0
!insert (w2_3, w2_4) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = w2_3 and t.toTerminal = w2_4).cost := 6.0
!insert (w2_4, w2_3) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = w2_4 and t.toTerminal = w2_3).cost := 6.0
!insert (w2_2, disposal) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = w2_2 and t.toTerminal = disposal).cost := 20.0
!insert (w2_3, disposal) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = w2_3 and t.toTerminal = disposal).cost := 15.0
!insert (w2_4, disposal) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = w2_4 and t.toTerminal = disposal).cost := 11.0
!insert (depot, w3_2) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = depot and t.toTerminal = w3_2).cost := 5.0
!insert (depot, w3_3) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = depot and t.toTerminal = w3_3).cost := 13.0
!insert (depot, w3_4) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = depot and t.toTerminal = w3_4).cost := 19.0
!insert (w3_2, w3_3) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = w3_2 and t.toTerminal = w3_3).cost := 8.0
!insert (w3_3, w3_2) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = w3_3 and t.toTerminal = w3_2).cost := 8.0
!insert (w3_2, w3_4) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = w3_2 and t.toTerminal = w3_4).cost := 14.0
!insert (w3_4, w3_2) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = w3_4 and t.toTerminal = w3_2).cost := 14.0
!insert (w3_3, w3_4) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = w3_3 and t.toTerminal = w3_4).cost := 6.0
!insert (w3_4, w3_3) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = w3_4 and t.toTerminal = w3_3).cost := 6.0
!insert (w3_2, disposal) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = w3_2 and t.toTerminal = disposal).cost := 20.0
!insert (w3_3, disposal) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = w3_3 and t.toTerminal = disposal).cost := 15.0
!insert (w3_4, disposal) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = w3_4 and t.toTerminal = disposal).cost := 11.0
!insert (depot, w4_2) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = depot and t.toTerminal = w4_2).cost := 5.0
!insert (depot, w4_3) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = depot and t.toTerminal = w4_3).cost := 13.0
!insert (depot, w4_4) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = depot and t.toTerminal = w4_4).cost := 19.0
!insert (w4_2, w4_3) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = w4_2 and t.toTerminal = w4_3).cost := 8.0
!insert (w4_3, w4_2) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = w4_3 and t.toTerminal = w4_2).cost := 8.0
!insert (w4_2, w4_4) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = w4_2 and t.toTerminal = w4_4).cost := 14.0
!insert (w4_4, w4_2) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = w4_4 and t.toTerminal = w4_2).cost := 14.0
!insert (w4_3, w4_4) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = w4_3 and t.toTerminal = w4_4).cost := 6.0
!insert (w4_4, w4_3) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = w4_4 and t.toTerminal = w4_3).cost := 6.0
!insert (w4_2, disposal) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = w4_2 and t.toTerminal = disposal).cost := 20.0
!insert (w4_3, disposal) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = w4_3 and t.toTerminal = disposal).cost := 15.0
!insert (w4_4, disposal) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = w4_4 and t.toTerminal = disposal).cost := 11.0
!insert (depot, w5_2) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = depot and t.toTerminal = w5_2).cost := 5.0
!insert (depot, w5_3) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = depot and t.toTerminal = w5_3).cost := 13.0
!insert (depot, w5_4) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = depot and t.toTerminal = w5_4).cost := 19.0
!insert (w5_2, w5_3) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = w5_2 and t.toTerminal = w5_3).cost := 8.0
!insert (w5_3, w5_2) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = w5_3 and t.toTerminal = w5_2).cost := 8.0
!insert (w5_2, w5_4) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = w5_2 and t.toTerminal = w5_4).cost := 14.0
!insert (w5_4, w5_2) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = w5_4 and t.toTerminal = w5_2).cost := 14.0
!insert (w5_3, w5_4) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = w5_3 and t.toTerminal = w5_4).cost := 6.0
!insert (w5_4, w5_3) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = w5_4 and t.toTerminal = w5_3).cost := 6.0
!insert (w5_2, disposal) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = w5_2 and t.toTerminal = disposal).cost := 20.0
!insert (w5_3, disposal) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = w5_3 and t.toTerminal = disposal).cost := 15.0
!insert (w5_4, disposal) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = w5_4 and t.toTerminal = disposal).cost := 11.0
!insert (depot, w6_2) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = depot and t.toTerminal = w6_2).cost := 5.0
!insert (depot, w6_3) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = depot and t.toTerminal = w6_3).cost := 13.0
!insert (depot, w6_4) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = depot and t.toTerminal = w6_4).cost := 19.0
!insert (w6_2, w6_3) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = w6_2 and t.toTerminal = w6_3).cost := 8.0
!insert (w6_3, w6_2) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = w6_3 and t.toTerminal = w6_2).cost := 8.0
!insert (w6_2, w6_4) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = w6_2 and t.toTerminal = w6_4).cost := 14.0
!insert (w6_4, w6_2) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = w6_4 and t.toTerminal = w6_2).cost := 14.0
!insert (w6_3, w6_4) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = w6_3 and t.toTerminal = w6_4).cost := 6.0
!insert (w6_4, w6_3) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = w6_4 and t.toTerminal = w6_3).cost := 6.0
!insert (w6_2, disposal) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = w6_2 and t.toTerminal = disposal).cost := 20.0
!insert (w6_3, disposal) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = w6_3 and t.toTerminal = disposal).cost := 15.0
!insert (w6_4, disposal) into TerminalDistance
!set TerminalDistance.allInstances->any(t | t.fromTerminal = w6_4 and t.toTerminal = disposal).cost := 11.0

-- -----------------------------------------------------------
-- 4. PositionConflict / WaypointConflict (within-lane pairs only --
--    cross-lane pairs are never populated, so they never constrain
--    anything; a cross-lane VisitAt link is still a possible decision
--    var, just one the lane-scoped coverage invariants correctly
--    treat as not covering its own lane.)
-- -----------------------------------------------------------
!insert (w1_2, w1_3, pos1_1) into PositionConflict
!insert (w1_2, w1_3, pos1_2) into PositionConflict
!insert (w1_2, w1_3, pos1_3) into PositionConflict
!insert (w1_2, w1_4, pos1_1) into PositionConflict
!insert (w1_2, w1_4, pos1_2) into PositionConflict
!insert (w1_2, w1_4, pos1_3) into PositionConflict
!insert (w1_3, w1_4, pos1_1) into PositionConflict
!insert (w1_3, w1_4, pos1_2) into PositionConflict
!insert (w1_3, w1_4, pos1_3) into PositionConflict
!insert (pos1_1, pos1_2, w1_2) into WaypointConflict
!insert (pos1_1, pos1_2, w1_3) into WaypointConflict
!insert (pos1_1, pos1_2, w1_4) into WaypointConflict
!insert (pos1_1, pos1_3, w1_2) into WaypointConflict
!insert (pos1_1, pos1_3, w1_3) into WaypointConflict
!insert (pos1_1, pos1_3, w1_4) into WaypointConflict
!insert (pos1_2, pos1_3, w1_2) into WaypointConflict
!insert (pos1_2, pos1_3, w1_3) into WaypointConflict
!insert (pos1_2, pos1_3, w1_4) into WaypointConflict
!insert (w2_2, w2_3, pos2_1) into PositionConflict
!insert (w2_2, w2_3, pos2_2) into PositionConflict
!insert (w2_2, w2_3, pos2_3) into PositionConflict
!insert (w2_2, w2_4, pos2_1) into PositionConflict
!insert (w2_2, w2_4, pos2_2) into PositionConflict
!insert (w2_2, w2_4, pos2_3) into PositionConflict
!insert (w2_3, w2_4, pos2_1) into PositionConflict
!insert (w2_3, w2_4, pos2_2) into PositionConflict
!insert (w2_3, w2_4, pos2_3) into PositionConflict
!insert (pos2_1, pos2_2, w2_2) into WaypointConflict
!insert (pos2_1, pos2_2, w2_3) into WaypointConflict
!insert (pos2_1, pos2_2, w2_4) into WaypointConflict
!insert (pos2_1, pos2_3, w2_2) into WaypointConflict
!insert (pos2_1, pos2_3, w2_3) into WaypointConflict
!insert (pos2_1, pos2_3, w2_4) into WaypointConflict
!insert (pos2_2, pos2_3, w2_2) into WaypointConflict
!insert (pos2_2, pos2_3, w2_3) into WaypointConflict
!insert (pos2_2, pos2_3, w2_4) into WaypointConflict
!insert (w3_2, w3_3, pos3_1) into PositionConflict
!insert (w3_2, w3_3, pos3_2) into PositionConflict
!insert (w3_2, w3_3, pos3_3) into PositionConflict
!insert (w3_2, w3_4, pos3_1) into PositionConflict
!insert (w3_2, w3_4, pos3_2) into PositionConflict
!insert (w3_2, w3_4, pos3_3) into PositionConflict
!insert (w3_3, w3_4, pos3_1) into PositionConflict
!insert (w3_3, w3_4, pos3_2) into PositionConflict
!insert (w3_3, w3_4, pos3_3) into PositionConflict
!insert (pos3_1, pos3_2, w3_2) into WaypointConflict
!insert (pos3_1, pos3_2, w3_3) into WaypointConflict
!insert (pos3_1, pos3_2, w3_4) into WaypointConflict
!insert (pos3_1, pos3_3, w3_2) into WaypointConflict
!insert (pos3_1, pos3_3, w3_3) into WaypointConflict
!insert (pos3_1, pos3_3, w3_4) into WaypointConflict
!insert (pos3_2, pos3_3, w3_2) into WaypointConflict
!insert (pos3_2, pos3_3, w3_3) into WaypointConflict
!insert (pos3_2, pos3_3, w3_4) into WaypointConflict
!insert (w4_2, w4_3, pos4_1) into PositionConflict
!insert (w4_2, w4_3, pos4_2) into PositionConflict
!insert (w4_2, w4_3, pos4_3) into PositionConflict
!insert (w4_2, w4_4, pos4_1) into PositionConflict
!insert (w4_2, w4_4, pos4_2) into PositionConflict
!insert (w4_2, w4_4, pos4_3) into PositionConflict
!insert (w4_3, w4_4, pos4_1) into PositionConflict
!insert (w4_3, w4_4, pos4_2) into PositionConflict
!insert (w4_3, w4_4, pos4_3) into PositionConflict
!insert (pos4_1, pos4_2, w4_2) into WaypointConflict
!insert (pos4_1, pos4_2, w4_3) into WaypointConflict
!insert (pos4_1, pos4_2, w4_4) into WaypointConflict
!insert (pos4_1, pos4_3, w4_2) into WaypointConflict
!insert (pos4_1, pos4_3, w4_3) into WaypointConflict
!insert (pos4_1, pos4_3, w4_4) into WaypointConflict
!insert (pos4_2, pos4_3, w4_2) into WaypointConflict
!insert (pos4_2, pos4_3, w4_3) into WaypointConflict
!insert (pos4_2, pos4_3, w4_4) into WaypointConflict
!insert (w5_2, w5_3, pos5_1) into PositionConflict
!insert (w5_2, w5_3, pos5_2) into PositionConflict
!insert (w5_2, w5_3, pos5_3) into PositionConflict
!insert (w5_2, w5_4, pos5_1) into PositionConflict
!insert (w5_2, w5_4, pos5_2) into PositionConflict
!insert (w5_2, w5_4, pos5_3) into PositionConflict
!insert (w5_3, w5_4, pos5_1) into PositionConflict
!insert (w5_3, w5_4, pos5_2) into PositionConflict
!insert (w5_3, w5_4, pos5_3) into PositionConflict
!insert (pos5_1, pos5_2, w5_2) into WaypointConflict
!insert (pos5_1, pos5_2, w5_3) into WaypointConflict
!insert (pos5_1, pos5_2, w5_4) into WaypointConflict
!insert (pos5_1, pos5_3, w5_2) into WaypointConflict
!insert (pos5_1, pos5_3, w5_3) into WaypointConflict
!insert (pos5_1, pos5_3, w5_4) into WaypointConflict
!insert (pos5_2, pos5_3, w5_2) into WaypointConflict
!insert (pos5_2, pos5_3, w5_3) into WaypointConflict
!insert (pos5_2, pos5_3, w5_4) into WaypointConflict
!insert (w6_2, w6_3, pos6_1) into PositionConflict
!insert (w6_2, w6_3, pos6_2) into PositionConflict
!insert (w6_2, w6_3, pos6_3) into PositionConflict
!insert (w6_2, w6_4, pos6_1) into PositionConflict
!insert (w6_2, w6_4, pos6_2) into PositionConflict
!insert (w6_2, w6_4, pos6_3) into PositionConflict
!insert (w6_3, w6_4, pos6_1) into PositionConflict
!insert (w6_3, w6_4, pos6_2) into PositionConflict
!insert (w6_3, w6_4, pos6_3) into PositionConflict
!insert (pos6_1, pos6_2, w6_2) into WaypointConflict
!insert (pos6_1, pos6_2, w6_3) into WaypointConflict
!insert (pos6_1, pos6_2, w6_4) into WaypointConflict
!insert (pos6_1, pos6_3, w6_2) into WaypointConflict
!insert (pos6_1, pos6_3, w6_3) into WaypointConflict
!insert (pos6_1, pos6_3, w6_4) into WaypointConflict
!insert (pos6_2, pos6_3, w6_2) into WaypointConflict
!insert (pos6_2, pos6_3, w6_3) into WaypointConflict
!insert (pos6_2, pos6_3, w6_4) into WaypointConflict

-- -----------------------------------------------------------
-- 5. Schedule singleton (objective host)
-- -----------------------------------------------------------
!create sched : Schedule

-- -----------------------------------------------------------
-- 6. Animated solution: every lane at its own optimum (w2,w3,w4 order)
-- -----------------------------------------------------------
!insert (w1_2, pos1_1) into VisitAt
!insert (w1_3, pos1_2) into VisitAt
!insert (w1_4, pos1_3) into VisitAt
!insert (w2_2, pos2_1) into VisitAt
!insert (w2_3, pos2_2) into VisitAt
!insert (w2_4, pos2_3) into VisitAt
!insert (w3_2, pos3_1) into VisitAt
!insert (w3_3, pos3_2) into VisitAt
!insert (w3_4, pos3_3) into VisitAt
!insert (w4_2, pos4_1) into VisitAt
!insert (w4_3, pos4_2) into VisitAt
!insert (w4_4, pos4_3) into VisitAt
!insert (w5_2, pos5_1) into VisitAt
!insert (w5_3, pos5_2) into VisitAt
!insert (w5_4, pos5_3) into VisitAt
!insert (w6_2, pos6_1) into VisitAt
!insert (w6_3, pos6_2) into VisitAt
!insert (w6_4, pos6_3) into VisitAt

-- -----------------------------------------------------------
-- 7. Check all constraints
-- -----------------------------------------------------------
check

-- Expected: all invariants true. sched.totalCost() = 6 x 30.0 = 180.0.
