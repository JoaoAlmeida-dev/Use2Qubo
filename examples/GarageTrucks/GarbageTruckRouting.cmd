-- ===========================================================
-- GarbageTruckRouting.cmd
-- Animation script for the GarbageTruckRouting USE model.
--
-- Scenario: 7 nodes (1 depot, 4 intersections, 1 disposal),
--           3 garbage bins, 2 trucks.
-- Solution animated below: 1 active truck covers all 3 bins via 5 selected
-- RouteRoad edges (depot->n2->n3->n4->n5->disposal, 30 min total); 1 truck
-- remains unassigned (no route). RouteRoad(Route,Road) is the decision
-- variable: edgeCost() sums travelTime directly over selected edges, so
-- this scenario's road graph has no shortcut-over-counting ambiguity (the
-- two shortcuts n2->disposal/n3->disposal are simply not selected).
-- edgeCost() = 30 <= fuelRange 100, so fuelPenalty() = max(0, 30-100)^2 = 0,
-- i.e. this is a "gold" feasible instance for the fuel-range penalty too.
--
-- Load with: open GarbageTruckRouting.cmd
-- ===========================================================

-- -----------------------------------------------------------
-- 1. Create city nodes
-- -----------------------------------------------------------
!create depot       : Node
!set depot.nodeId   := 1
!set depot.nodeType := #Depot

!create n2       : Node
!set n2.nodeId   := 2
!set n2.nodeType := #Intersection

!create n3       : Node
!set n3.nodeId   := 3
!set n3.nodeType := #Intersection

!create n4       : Node
!set n4.nodeId   := 4
!set n4.nodeType := #Intersection

!create n5       : Node
!set n5.nodeId   := 5
!set n5.nodeType := #Intersection

!create disposal       : Node
!set disposal.nodeId   := 6
!set disposal.nodeType := #DisposalFacility

-- -----------------------------------------------------------
-- 2. Create road network (directed edges with travel times)
-- -----------------------------------------------------------
!insert (depot, n2) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n2).travelTime := 5.0

!insert (n2, n3) into Road
!set Road.allInstances->any(r | r.origin = n2 and r.destination = n3).travelTime := 8.0

!insert (n3, n4) into Road
!set Road.allInstances->any(r | r.origin = n3 and r.destination = n4).travelTime := 6.0

!insert (n4, n5) into Road
!set Road.allInstances->any(r | r.origin = n4 and r.destination = n5).travelTime := 7.0

!insert (n5, disposal) into Road
!set Road.allInstances->any(r | r.origin = n5 and r.destination = disposal).travelTime := 4.0

-- Direct shortcuts (not used by the animated route below)
!insert (n2, disposal) into Road
!set Road.allInstances->any(r | r.origin = n2 and r.destination = disposal).travelTime := 20.0

!insert (n3, disposal) into Road
!set Road.allInstances->any(r | r.origin = n3 and r.destination = disposal).travelTime := 15.0

-- -----------------------------------------------------------
-- 3. Create garbage bins
-- -----------------------------------------------------------
!create bin1       : GarbageBin
!set bin1.maxFill     := 1.0   -- 1000 L capacity
!set bin1.currentFill := 0.0   -- empty

!create bin2       : GarbageBin
!set bin2.maxFill     := 1.0   -- 1000 L capacity
!set bin2.currentFill := 0.98  -- 980 L, nearly full

!create bin3       : GarbageBin
!set bin3.maxFill     := 1.0   -- 1000 L capacity
!set bin3.currentFill := 0.5   -- 500 L

-- Place bins at intersections
!insert (bin1, n2) into LocatedAt
!insert (bin2, n3) into LocatedAt
!insert (bin3, n4) into LocatedAt

-- -----------------------------------------------------------
-- 4. Create trucks
-- -----------------------------------------------------------
!create truck1 : Truck
!set truck1.truckId     := 1
!set truck1.fuelRange   := 100.0  -- can drive 100 km
!set truck1.maxCapacity := 10.0   -- holds 10 m3
!set truck1.currentLoad := 0.0

!create truck2 : Truck
!set truck2.truckId     := 2
!set truck2.fuelRange   := 100.0
!set truck2.maxCapacity := 10.0
!set truck2.currentLoad := 0.0
-- truck2 gets no route below; not needed since all bins fit on truck1's route.

-- -----------------------------------------------------------
-- 5. Create the single active route
-- -----------------------------------------------------------
!create route1 : Route
!set route1.totalTravelTime := 30.0   -- 5+8+6+7+4 = 30 min

-- Assign route to truck1
!insert (route1, truck1) into AssignedTo

-- Select the edges this route uses (RouteRoad is the core decision variable)
!insert (route1, Road.allInstances->any(r | r.origin = depot and r.destination = n2)) into RouteRoad
!insert (route1, Road.allInstances->any(r | r.origin = n2 and r.destination = n3)) into RouteRoad
!insert (route1, Road.allInstances->any(r | r.origin = n3 and r.destination = n4)) into RouteRoad
!insert (route1, Road.allInstances->any(r | r.origin = n4 and r.destination = n5)) into RouteRoad
!insert (route1, Road.allInstances->any(r | r.origin = n5 and r.destination = disposal)) into RouteRoad

-- -----------------------------------------------------------
-- 6. Simulate garbage collection along the route
-- -----------------------------------------------------------

-- bin1 at n2 is already empty (currentFill = 0); collectGarbage's
-- binNeedsCollection precondition requires currentFill > 0, so it is not
-- called here -- an empty bin needs no pickup.

-- Truck visits bin2 at n3
!openter truck1 collectGarbage(bin2)
!set truck1.currentLoad := truck1.currentLoad + bin2.currentFill
!set bin2.currentFill   := 0.0
!opexit

-- Truck visits bin3 at n4
!openter truck1 collectGarbage(bin3)
!set truck1.currentLoad := truck1.currentLoad + bin3.currentFill
!set bin3.currentFill   := 0.0
!opexit

-- -----------------------------------------------------------
-- 7. Check all constraints
-- -----------------------------------------------------------
check

-- Expected: all invariants true.
-- truck1.currentLoad = 0.98 + 0.5 = 1.48 m3  (<= maxCapacity 10.0)
-- edgeCost() = 5+8+6+7+4 = 30.0 min; fuelPenalty() = max(0, 30-100)^2 = 0
-- All bins have currentFill = 0 (collected)
-- truck2 has no route
