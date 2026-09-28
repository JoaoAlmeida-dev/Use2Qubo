-- ===========================================================
-- GarageTrucksTSP.cmd
-- Animation script for the GarageTrucksTSP USE model (position-indexed
-- redesign of GarbageTruckRouting.cmd's scenario -- see JAVA-021).
--
-- 3 bin stops (bin locations n2/n3/n4), depot fixed as start, disposal
-- fixed as end. TravelTime costs are the metric closure (shortest
-- path) of the original 7-road GarbageTruckRouting graph, treated as
-- undirected/bidirectional (a real city generally allows travel both ways
-- between connected points, even where the original directed toy graph
-- didn't model the reverse edge):
--   depot-n2=5, n2-n3=8, n3-n4=6, n4-n5=7, n5-disposal=4,
--   n2-disposal=20 (shortcut), n3-disposal=15 (shortcut)
-- Shortest paths among {depot,n2,n3,n4,disposal} (Dijkstra over the above,
-- undirected):
--   depot->n2=5, depot->n3=13, depot->n4=19
--   n2->n3=8, n2->n4=14, n3->n4=6 (symmetric both directions)
--   n2->disposal=20, n3->disposal=15, n4->disposal=11
-- Optimal order depot->n2->n3->n4->disposal = 5+8+6+11 = 30, matching the
-- original scenario's animated route cost exactly (same underlying graph).
--
-- Load with: open GarageTrucksTSP.cmd
-- ===========================================================

-- -----------------------------------------------------------
-- 1. Stops: depot, disposal (plain Stop), 3 bin stops (bin nodes)
-- -----------------------------------------------------------
!create depot : Stop
!set depot.stopId   := 1
!set depot.stopType := #Depot

!create disposal : Stop
!set disposal.stopId   := 6
!set disposal.stopType := #DisposalFacility

!create n2 : BinStop
!set n2.stopId   := 2
!set n2.stopType := #BinStop

!create n3 : BinStop
!set n3.stopId   := 3
!set n3.stopType := #BinStop

!create n4 : BinStop
!set n4.stopId   := 4
!set n4.stopType := #BinStop

-- -----------------------------------------------------------
-- 2. Route slots (3 middle slots between fixed depot/disposal endpoints)
-- -----------------------------------------------------------
!create pos1 : RouteSlot
!set pos1.idx := 1

!create pos2 : RouteSlot
!set pos2.idx := 2

!create pos3 : RouteSlot
!set pos3.idx := 3

-- -----------------------------------------------------------
-- 3. TravelTime: metric closure over the original road graph
-- -----------------------------------------------------------
!insert (depot, n2) into TravelTime
!set TravelTime.allInstances->any(tt | tt.fromStop = depot and tt.toStop = n2).cost := 5.0

!insert (depot, n3) into TravelTime
!set TravelTime.allInstances->any(tt | tt.fromStop = depot and tt.toStop = n3).cost := 13.0

!insert (depot, n4) into TravelTime
!set TravelTime.allInstances->any(tt | tt.fromStop = depot and tt.toStop = n4).cost := 19.0

!insert (n2, n3) into TravelTime
!set TravelTime.allInstances->any(tt | tt.fromStop = n2 and tt.toStop = n3).cost := 8.0

!insert (n3, n2) into TravelTime
!set TravelTime.allInstances->any(tt | tt.fromStop = n3 and tt.toStop = n2).cost := 8.0

!insert (n2, n4) into TravelTime
!set TravelTime.allInstances->any(tt | tt.fromStop = n2 and tt.toStop = n4).cost := 14.0

!insert (n4, n2) into TravelTime
!set TravelTime.allInstances->any(tt | tt.fromStop = n4 and tt.toStop = n2).cost := 14.0

!insert (n3, n4) into TravelTime
!set TravelTime.allInstances->any(tt | tt.fromStop = n3 and tt.toStop = n4).cost := 6.0

!insert (n4, n3) into TravelTime
!set TravelTime.allInstances->any(tt | tt.fromStop = n4 and tt.toStop = n3).cost := 6.0

!insert (n2, disposal) into TravelTime
!set TravelTime.allInstances->any(tt | tt.fromStop = n2 and tt.toStop = disposal).cost := 20.0

!insert (n3, disposal) into TravelTime
!set TravelTime.allInstances->any(tt | tt.fromStop = n3 and tt.toStop = disposal).cost := 15.0

!insert (n4, disposal) into TravelTime
!set TravelTime.allInstances->any(tt | tt.fromStop = n4 and tt.toStop = disposal).cost := 11.0

-- -----------------------------------------------------------
-- 4. CollectionRun singleton (objective host)
-- -----------------------------------------------------------
!create sched : CollectionRun

-- -----------------------------------------------------------
-- 5. Animated solution: depot -> n2 -> n3 -> n4 -> disposal (cost 30,
--    matches the original GarbageTruckRouting scenario's optimal route).
-- -----------------------------------------------------------
!insert (n2, pos1) into CollectsAt
!insert (n3, pos2) into CollectsAt
!insert (n4, pos3) into CollectsAt

-- -----------------------------------------------------------
-- 6. Check all constraints
-- -----------------------------------------------------------
check

-- Expected: all invariants true. sched.totalTravelTime() = 5+8+6+11 = 30.0
