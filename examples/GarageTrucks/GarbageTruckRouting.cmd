-- ===========================================================
-- GarbageTruckRouting.cmd
-- Animation script for the GarbageTruckRouting USE model.
--
-- Scenario: 7 nodes (1 depot, 4 intersections, 1 disposal),
--           3 garbage bins, 2 trucks.
-- Optimal solution: 1 active truck covers all 3 bins;
--                   1 truck remains inactive.
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

-- Direct shortcuts
!insert (n2, disposal) into Road
!set Road.allInstances->any(r | r.origin = n2 and r.destination = disposal).travelTime := 20.0

!insert (n3, disposal) into Road
!set Road.allInstances->any(r | r.origin = n3 and r.destination = disposal).travelTime := 15.0

-- -----------------------------------------------------------
-- 3. Create garbage bins
-- -----------------------------------------------------------
!create bin1       : GarbageBin
!set bin1.maxFill     := 2.0   -- 2 m3 capacity
!set bin1.currentFill := 1.5   -- 75% full

!create bin2       : GarbageBin
!set bin2.maxFill     := 2.0
!set bin2.currentFill := 0.8   -- 40% full

!create bin3       : GarbageBin
!set bin3.maxFill     := 3.0   -- larger bin
!set bin3.currentFill := 2.0   -- 67% full

-- Place bins at intersections
!insert (bin1, n2) into LocatedAt
!insert (bin2, n3) into LocatedAt
!insert (bin3, n4) into LocatedAt

-- -----------------------------------------------------------
-- 4. Create trucks
-- -----------------------------------------------------------
!create truck1 : Truck
!set truck1.truckId           := 1
!set truck1.fuelRange         := 100.0  -- can drive 100 km
!set truck1.maxCapacity       := 10.0   -- holds 10 m3
!set truck1.currentLoad       := 0.0
!set truck1.distanceTravelled := 0.0
!set truck1.active            := true

!create truck2 : Truck
!set truck2.truckId           := 2
!set truck2.fuelRange         := 100.0
!set truck2.maxCapacity       := 10.0
!set truck2.currentLoad       := 0.0
!set truck2.distanceTravelled := 0.0
!set truck2.active            := false  -- not needed; all bins fit in truck1

-- -----------------------------------------------------------
-- 5. Create the single active route
-- -----------------------------------------------------------
!create route1 : Route
!set route1.totalTravelTime := 30.0   -- 5+8+6+7+4 = 30 min

-- Assign route to truck1
!insert (route1, truck1) into AssignedTo

-- Define ordered stop sequence with explicit step indices (0-indexed).
-- step is the QUBO variable index; AutoQUBO uses it to build symbolic
-- Q matrix entries per (Route, step, Node) triple.
!insert (route1, depot) into RouteStop
!set RouteStop.allInstances->any(rs | rs.routes = route1 and rs.stops = depot).step := 0

!insert (route1, n2) into RouteStop
!set RouteStop.allInstances->any(rs | rs.routes = route1 and rs.stops = n2).step := 1

!insert (route1, n3) into RouteStop
!set RouteStop.allInstances->any(rs | rs.routes = route1 and rs.stops = n3).step := 2

!insert (route1, n4) into RouteStop
!set RouteStop.allInstances->any(rs | rs.routes = route1 and rs.stops = n4).step := 3

!insert (route1, n5) into RouteStop
!set RouteStop.allInstances->any(rs | rs.routes = route1 and rs.stops = n5).step := 4

!insert (route1, disposal) into RouteStop
!set RouteStop.allInstances->any(rs | rs.routes = route1 and rs.stops = disposal).step := 5

-- -----------------------------------------------------------
-- 6. Simulate garbage collection along the route
-- -----------------------------------------------------------

-- Truck visits bin1 at n2
!openter truck1 collectGarbage(bin1)
!set truck1.currentLoad := truck1.currentLoad + bin1.currentFill
!set bin1.currentFill   := 0.0
!opexit

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

-- Update truck distance (5+8+6+7+4 = 30 min, set manually here).
-- In a solver run, distanceTravelled is a Real-valued post-solve attribute
-- derived from the selected RouteStop links; it is not encoded in the QUBO Q matrix.
!set truck1.distanceTravelled := 30.0

-- -----------------------------------------------------------
-- 7. Check all constraints
-- -----------------------------------------------------------
check

-- Expected: all invariants true.
-- truck1.currentLoad = 1.5 + 0.8 + 2.0 = 4.3 m3  (<= maxCapacity 10.0)
-- truck1.distanceTravelled = 30.0 km              (<= fuelRange 100.0)
-- All bins have currentFill = 0 (collected)
-- truck2 is inactive and has no route
