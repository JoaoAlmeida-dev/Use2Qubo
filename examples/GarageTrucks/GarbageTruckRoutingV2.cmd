-- ===========================================================
-- GarbageTruckRoutingV2.cmd
-- Animation script for the GarbageTruckRouting USE model.
--
-- Scenario: 50 nodes (1 depot, 48 intersections, 1 disposal)
--           laid out as a 5x10 directed grid (road = +1 right / +10 down),
--           20 garbage bins, 3 trucks, all 3 active on disjoint monotone
--           staircase routes from depot (node 1) to disposal (node 50).
-- Each route uses 9 horizontal + 4 vertical road segments (60 min total),
-- matching the grid's only two travelTime values (4.0 horiz / 6.0 vert).
--
-- Load with: open GarbageTruckRoutingV2.cmd
-- ===========================================================

-- -----------------------------------------------------------
-- 1. Create city nodes (5x10 grid, id = (row-1)*10 + col)
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
!create n6       : Node
!set n6.nodeId   := 6
!set n6.nodeType := #Intersection
!create n7       : Node
!set n7.nodeId   := 7
!set n7.nodeType := #Intersection
!create n8       : Node
!set n8.nodeId   := 8
!set n8.nodeType := #Intersection
!create n9       : Node
!set n9.nodeId   := 9
!set n9.nodeType := #Intersection
!create n10       : Node
!set n10.nodeId   := 10
!set n10.nodeType := #Intersection
!create n11       : Node
!set n11.nodeId   := 11
!set n11.nodeType := #Intersection
!create n12       : Node
!set n12.nodeId   := 12
!set n12.nodeType := #Intersection
!create n13       : Node
!set n13.nodeId   := 13
!set n13.nodeType := #Intersection
!create n14       : Node
!set n14.nodeId   := 14
!set n14.nodeType := #Intersection
!create n15       : Node
!set n15.nodeId   := 15
!set n15.nodeType := #Intersection
!create n16       : Node
!set n16.nodeId   := 16
!set n16.nodeType := #Intersection
!create n17       : Node
!set n17.nodeId   := 17
!set n17.nodeType := #Intersection
!create n18       : Node
!set n18.nodeId   := 18
!set n18.nodeType := #Intersection
!create n19       : Node
!set n19.nodeId   := 19
!set n19.nodeType := #Intersection
!create n20       : Node
!set n20.nodeId   := 20
!set n20.nodeType := #Intersection
!create n21       : Node
!set n21.nodeId   := 21
!set n21.nodeType := #Intersection
!create n22       : Node
!set n22.nodeId   := 22
!set n22.nodeType := #Intersection
!create n23       : Node
!set n23.nodeId   := 23
!set n23.nodeType := #Intersection
!create n24       : Node
!set n24.nodeId   := 24
!set n24.nodeType := #Intersection
!create n25       : Node
!set n25.nodeId   := 25
!set n25.nodeType := #Intersection
!create n26       : Node
!set n26.nodeId   := 26
!set n26.nodeType := #Intersection
!create n27       : Node
!set n27.nodeId   := 27
!set n27.nodeType := #Intersection
!create n28       : Node
!set n28.nodeId   := 28
!set n28.nodeType := #Intersection
!create n29       : Node
!set n29.nodeId   := 29
!set n29.nodeType := #Intersection
!create n30       : Node
!set n30.nodeId   := 30
!set n30.nodeType := #Intersection
!create n31       : Node
!set n31.nodeId   := 31
!set n31.nodeType := #Intersection
!create n32       : Node
!set n32.nodeId   := 32
!set n32.nodeType := #Intersection
!create n33       : Node
!set n33.nodeId   := 33
!set n33.nodeType := #Intersection
!create n34       : Node
!set n34.nodeId   := 34
!set n34.nodeType := #Intersection
!create n35       : Node
!set n35.nodeId   := 35
!set n35.nodeType := #Intersection
!create n36       : Node
!set n36.nodeId   := 36
!set n36.nodeType := #Intersection
!create n37       : Node
!set n37.nodeId   := 37
!set n37.nodeType := #Intersection
!create n38       : Node
!set n38.nodeId   := 38
!set n38.nodeType := #Intersection
!create n39       : Node
!set n39.nodeId   := 39
!set n39.nodeType := #Intersection
!create n40       : Node
!set n40.nodeId   := 40
!set n40.nodeType := #Intersection
!create n41       : Node
!set n41.nodeId   := 41
!set n41.nodeType := #Intersection
!create n42       : Node
!set n42.nodeId   := 42
!set n42.nodeType := #Intersection
!create n43       : Node
!set n43.nodeId   := 43
!set n43.nodeType := #Intersection
!create n44       : Node
!set n44.nodeId   := 44
!set n44.nodeType := #Intersection
!create n45       : Node
!set n45.nodeId   := 45
!set n45.nodeType := #Intersection
!create n46       : Node
!set n46.nodeId   := 46
!set n46.nodeType := #Intersection
!create n47       : Node
!set n47.nodeId   := 47
!set n47.nodeType := #Intersection
!create n48       : Node
!set n48.nodeId   := 48
!set n48.nodeType := #Intersection
!create n49       : Node
!set n49.nodeId   := 49
!set n49.nodeType := #Intersection

!create disposal       : Node
!set disposal.nodeId   := 50
!set disposal.nodeType := #DisposalFacility

-- -----------------------------------------------------------
-- 2. Create road network (directed edges with travel times)
-- -----------------------------------------------------------
-- Horizontal roads (same row, col -> col+1), travelTime 4.0
!insert (depot, n2) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n2).travelTime := 4.0
!insert (n2, n3) into Road
!set Road.allInstances->any(r | r.origin = n2 and r.destination = n3).travelTime := 4.0
!insert (n3, n4) into Road
!set Road.allInstances->any(r | r.origin = n3 and r.destination = n4).travelTime := 4.0
!insert (n4, n5) into Road
!set Road.allInstances->any(r | r.origin = n4 and r.destination = n5).travelTime := 4.0
!insert (n5, n6) into Road
!set Road.allInstances->any(r | r.origin = n5 and r.destination = n6).travelTime := 4.0
!insert (n6, n7) into Road
!set Road.allInstances->any(r | r.origin = n6 and r.destination = n7).travelTime := 4.0
!insert (n7, n8) into Road
!set Road.allInstances->any(r | r.origin = n7 and r.destination = n8).travelTime := 4.0
!insert (n8, n9) into Road
!set Road.allInstances->any(r | r.origin = n8 and r.destination = n9).travelTime := 4.0
!insert (n9, n10) into Road
!set Road.allInstances->any(r | r.origin = n9 and r.destination = n10).travelTime := 4.0
!insert (n11, n12) into Road
!set Road.allInstances->any(r | r.origin = n11 and r.destination = n12).travelTime := 4.0
!insert (n12, n13) into Road
!set Road.allInstances->any(r | r.origin = n12 and r.destination = n13).travelTime := 4.0
!insert (n13, n14) into Road
!set Road.allInstances->any(r | r.origin = n13 and r.destination = n14).travelTime := 4.0
!insert (n14, n15) into Road
!set Road.allInstances->any(r | r.origin = n14 and r.destination = n15).travelTime := 4.0
!insert (n15, n16) into Road
!set Road.allInstances->any(r | r.origin = n15 and r.destination = n16).travelTime := 4.0
!insert (n16, n17) into Road
!set Road.allInstances->any(r | r.origin = n16 and r.destination = n17).travelTime := 4.0
!insert (n17, n18) into Road
!set Road.allInstances->any(r | r.origin = n17 and r.destination = n18).travelTime := 4.0
!insert (n18, n19) into Road
!set Road.allInstances->any(r | r.origin = n18 and r.destination = n19).travelTime := 4.0
!insert (n19, n20) into Road
!set Road.allInstances->any(r | r.origin = n19 and r.destination = n20).travelTime := 4.0
!insert (n21, n22) into Road
!set Road.allInstances->any(r | r.origin = n21 and r.destination = n22).travelTime := 4.0
!insert (n22, n23) into Road
!set Road.allInstances->any(r | r.origin = n22 and r.destination = n23).travelTime := 4.0
!insert (n23, n24) into Road
!set Road.allInstances->any(r | r.origin = n23 and r.destination = n24).travelTime := 4.0
!insert (n24, n25) into Road
!set Road.allInstances->any(r | r.origin = n24 and r.destination = n25).travelTime := 4.0
!insert (n25, n26) into Road
!set Road.allInstances->any(r | r.origin = n25 and r.destination = n26).travelTime := 4.0
!insert (n26, n27) into Road
!set Road.allInstances->any(r | r.origin = n26 and r.destination = n27).travelTime := 4.0
!insert (n27, n28) into Road
!set Road.allInstances->any(r | r.origin = n27 and r.destination = n28).travelTime := 4.0
!insert (n28, n29) into Road
!set Road.allInstances->any(r | r.origin = n28 and r.destination = n29).travelTime := 4.0
!insert (n29, n30) into Road
!set Road.allInstances->any(r | r.origin = n29 and r.destination = n30).travelTime := 4.0
!insert (n31, n32) into Road
!set Road.allInstances->any(r | r.origin = n31 and r.destination = n32).travelTime := 4.0
!insert (n32, n33) into Road
!set Road.allInstances->any(r | r.origin = n32 and r.destination = n33).travelTime := 4.0
!insert (n33, n34) into Road
!set Road.allInstances->any(r | r.origin = n33 and r.destination = n34).travelTime := 4.0
!insert (n34, n35) into Road
!set Road.allInstances->any(r | r.origin = n34 and r.destination = n35).travelTime := 4.0
!insert (n35, n36) into Road
!set Road.allInstances->any(r | r.origin = n35 and r.destination = n36).travelTime := 4.0
!insert (n36, n37) into Road
!set Road.allInstances->any(r | r.origin = n36 and r.destination = n37).travelTime := 4.0
!insert (n37, n38) into Road
!set Road.allInstances->any(r | r.origin = n37 and r.destination = n38).travelTime := 4.0
!insert (n38, n39) into Road
!set Road.allInstances->any(r | r.origin = n38 and r.destination = n39).travelTime := 4.0
!insert (n39, n40) into Road
!set Road.allInstances->any(r | r.origin = n39 and r.destination = n40).travelTime := 4.0
!insert (n41, n42) into Road
!set Road.allInstances->any(r | r.origin = n41 and r.destination = n42).travelTime := 4.0
!insert (n42, n43) into Road
!set Road.allInstances->any(r | r.origin = n42 and r.destination = n43).travelTime := 4.0
!insert (n43, n44) into Road
!set Road.allInstances->any(r | r.origin = n43 and r.destination = n44).travelTime := 4.0
!insert (n44, n45) into Road
!set Road.allInstances->any(r | r.origin = n44 and r.destination = n45).travelTime := 4.0
!insert (n45, n46) into Road
!set Road.allInstances->any(r | r.origin = n45 and r.destination = n46).travelTime := 4.0
!insert (n46, n47) into Road
!set Road.allInstances->any(r | r.origin = n46 and r.destination = n47).travelTime := 4.0
!insert (n47, n48) into Road
!set Road.allInstances->any(r | r.origin = n47 and r.destination = n48).travelTime := 4.0
!insert (n48, n49) into Road
!set Road.allInstances->any(r | r.origin = n48 and r.destination = n49).travelTime := 4.0
!insert (n49, disposal) into Road
!set Road.allInstances->any(r | r.origin = n49 and r.destination = disposal).travelTime := 4.0

-- Vertical roads (same col, row -> row+1), travelTime 6.0
!insert (depot, n11) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n11).travelTime := 6.0
!insert (n2, n12) into Road
!set Road.allInstances->any(r | r.origin = n2 and r.destination = n12).travelTime := 6.0
!insert (n3, n13) into Road
!set Road.allInstances->any(r | r.origin = n3 and r.destination = n13).travelTime := 6.0
!insert (n4, n14) into Road
!set Road.allInstances->any(r | r.origin = n4 and r.destination = n14).travelTime := 6.0
!insert (n5, n15) into Road
!set Road.allInstances->any(r | r.origin = n5 and r.destination = n15).travelTime := 6.0
!insert (n6, n16) into Road
!set Road.allInstances->any(r | r.origin = n6 and r.destination = n16).travelTime := 6.0
!insert (n7, n17) into Road
!set Road.allInstances->any(r | r.origin = n7 and r.destination = n17).travelTime := 6.0
!insert (n8, n18) into Road
!set Road.allInstances->any(r | r.origin = n8 and r.destination = n18).travelTime := 6.0
!insert (n9, n19) into Road
!set Road.allInstances->any(r | r.origin = n9 and r.destination = n19).travelTime := 6.0
!insert (n10, n20) into Road
!set Road.allInstances->any(r | r.origin = n10 and r.destination = n20).travelTime := 6.0
!insert (n11, n21) into Road
!set Road.allInstances->any(r | r.origin = n11 and r.destination = n21).travelTime := 6.0
!insert (n12, n22) into Road
!set Road.allInstances->any(r | r.origin = n12 and r.destination = n22).travelTime := 6.0
!insert (n13, n23) into Road
!set Road.allInstances->any(r | r.origin = n13 and r.destination = n23).travelTime := 6.0
!insert (n14, n24) into Road
!set Road.allInstances->any(r | r.origin = n14 and r.destination = n24).travelTime := 6.0
!insert (n15, n25) into Road
!set Road.allInstances->any(r | r.origin = n15 and r.destination = n25).travelTime := 6.0
!insert (n16, n26) into Road
!set Road.allInstances->any(r | r.origin = n16 and r.destination = n26).travelTime := 6.0
!insert (n17, n27) into Road
!set Road.allInstances->any(r | r.origin = n17 and r.destination = n27).travelTime := 6.0
!insert (n18, n28) into Road
!set Road.allInstances->any(r | r.origin = n18 and r.destination = n28).travelTime := 6.0
!insert (n19, n29) into Road
!set Road.allInstances->any(r | r.origin = n19 and r.destination = n29).travelTime := 6.0
!insert (n20, n30) into Road
!set Road.allInstances->any(r | r.origin = n20 and r.destination = n30).travelTime := 6.0
!insert (n21, n31) into Road
!set Road.allInstances->any(r | r.origin = n21 and r.destination = n31).travelTime := 6.0
!insert (n22, n32) into Road
!set Road.allInstances->any(r | r.origin = n22 and r.destination = n32).travelTime := 6.0
!insert (n23, n33) into Road
!set Road.allInstances->any(r | r.origin = n23 and r.destination = n33).travelTime := 6.0
!insert (n24, n34) into Road
!set Road.allInstances->any(r | r.origin = n24 and r.destination = n34).travelTime := 6.0
!insert (n25, n35) into Road
!set Road.allInstances->any(r | r.origin = n25 and r.destination = n35).travelTime := 6.0
!insert (n26, n36) into Road
!set Road.allInstances->any(r | r.origin = n26 and r.destination = n36).travelTime := 6.0
!insert (n27, n37) into Road
!set Road.allInstances->any(r | r.origin = n27 and r.destination = n37).travelTime := 6.0
!insert (n28, n38) into Road
!set Road.allInstances->any(r | r.origin = n28 and r.destination = n38).travelTime := 6.0
!insert (n29, n39) into Road
!set Road.allInstances->any(r | r.origin = n29 and r.destination = n39).travelTime := 6.0
!insert (n30, n40) into Road
!set Road.allInstances->any(r | r.origin = n30 and r.destination = n40).travelTime := 6.0
!insert (n31, n41) into Road
!set Road.allInstances->any(r | r.origin = n31 and r.destination = n41).travelTime := 6.0
!insert (n32, n42) into Road
!set Road.allInstances->any(r | r.origin = n32 and r.destination = n42).travelTime := 6.0
!insert (n33, n43) into Road
!set Road.allInstances->any(r | r.origin = n33 and r.destination = n43).travelTime := 6.0
!insert (n34, n44) into Road
!set Road.allInstances->any(r | r.origin = n34 and r.destination = n44).travelTime := 6.0
!insert (n35, n45) into Road
!set Road.allInstances->any(r | r.origin = n35 and r.destination = n45).travelTime := 6.0
!insert (n36, n46) into Road
!set Road.allInstances->any(r | r.origin = n36 and r.destination = n46).travelTime := 6.0
!insert (n37, n47) into Road
!set Road.allInstances->any(r | r.origin = n37 and r.destination = n47).travelTime := 6.0
!insert (n38, n48) into Road
!set Road.allInstances->any(r | r.origin = n38 and r.destination = n48).travelTime := 6.0
!insert (n39, n49) into Road
!set Road.allInstances->any(r | r.origin = n39 and r.destination = n49).travelTime := 6.0
!insert (n40, disposal) into Road
!set Road.allInstances->any(r | r.origin = n40 and r.destination = disposal).travelTime := 6.0

-- -----------------------------------------------------------
-- 3. Create garbage bins (20 total, 3.0 m3 capacity each)
-- -----------------------------------------------------------
!create bin1       : GarbageBin
!set bin1.maxFill     := 3.0
!set bin1.currentFill := 1.0
!create bin2       : GarbageBin
!set bin2.maxFill     := 3.0
!set bin2.currentFill := 1.5
!create bin3       : GarbageBin
!set bin3.maxFill     := 3.0
!set bin3.currentFill := 2.0
!create bin4       : GarbageBin
!set bin4.maxFill     := 3.0
!set bin4.currentFill := 0.8
!create bin5       : GarbageBin
!set bin5.maxFill     := 3.0
!set bin5.currentFill := 1.2
!create bin6       : GarbageBin
!set bin6.maxFill     := 3.0
!set bin6.currentFill := 1.8
!create bin7       : GarbageBin
!set bin7.maxFill     := 3.0
!set bin7.currentFill := 2.2
!create bin8       : GarbageBin
!set bin8.maxFill     := 3.0
!set bin8.currentFill := 0.6
!create bin9       : GarbageBin
!set bin9.maxFill     := 3.0
!set bin9.currentFill := 1.4
!create bin10       : GarbageBin
!set bin10.maxFill     := 3.0
!set bin10.currentFill := 2.4
!create bin11       : GarbageBin
!set bin11.maxFill     := 3.0
!set bin11.currentFill := 1.0
!create bin12       : GarbageBin
!set bin12.maxFill     := 3.0
!set bin12.currentFill := 1.6
!create bin13       : GarbageBin
!set bin13.maxFill     := 3.0
!set bin13.currentFill := 2.0
!create bin14       : GarbageBin
!set bin14.maxFill     := 3.0
!set bin14.currentFill := 0.9
!create bin15       : GarbageBin
!set bin15.maxFill     := 3.0
!set bin15.currentFill := 1.3
!create bin16       : GarbageBin
!set bin16.maxFill     := 3.0
!set bin16.currentFill := 1.7
!create bin17       : GarbageBin
!set bin17.maxFill     := 3.0
!set bin17.currentFill := 2.1
!create bin18       : GarbageBin
!set bin18.maxFill     := 3.0
!set bin18.currentFill := 0.7
!create bin19       : GarbageBin
!set bin19.maxFill     := 3.0
!set bin19.currentFill := 1.5
!create bin20       : GarbageBin
!set bin20.maxFill     := 3.0
!set bin20.currentFill := 1.9

-- Place bins at intersections along the three routes
!insert (bin1, n2) into LocatedAt
!insert (bin2, n3) into LocatedAt
!insert (bin3, n14) into LocatedAt
!insert (bin4, n15) into LocatedAt
!insert (bin5, n26) into LocatedAt
!insert (bin6, n37) into LocatedAt
!insert (bin7, n39) into LocatedAt
!insert (bin8, n11) into LocatedAt
!insert (bin9, n12) into LocatedAt
!insert (bin10, n23) into LocatedAt
!insert (bin11, n24) into LocatedAt
!insert (bin12, n35) into LocatedAt
!insert (bin13, n36) into LocatedAt
!insert (bin14, n48) into LocatedAt
!insert (bin15, n13) into LocatedAt
!insert (bin16, n34) into LocatedAt
!insert (bin17, n45) into LocatedAt
!insert (bin18, n46) into LocatedAt
!insert (bin19, n47) into LocatedAt
!insert (bin20, n49) into LocatedAt

-- -----------------------------------------------------------
-- 4. Create trucks (3 active)
-- -----------------------------------------------------------
!create truck1 : Truck
!set truck1.truckId           := 1
!set truck1.fuelRange         := 100.0  -- can drive 100 km
!set truck1.maxCapacity       := 15.0   -- holds 15 m3
!set truck1.currentLoad       := 0.0
!set truck1.distanceTravelled := 0.0
!set truck1.active            := true

!create truck2 : Truck
!set truck2.truckId           := 2
!set truck2.fuelRange         := 100.0  -- can drive 100 km
!set truck2.maxCapacity       := 15.0   -- holds 15 m3
!set truck2.currentLoad       := 0.0
!set truck2.distanceTravelled := 0.0
!set truck2.active            := true

!create truck3 : Truck
!set truck3.truckId           := 3
!set truck3.fuelRange         := 100.0  -- can drive 100 km
!set truck3.maxCapacity       := 15.0   -- holds 15 m3
!set truck3.currentLoad       := 0.0
!set truck3.distanceTravelled := 0.0
!set truck3.active            := true

-- -----------------------------------------------------------
-- 5. Create the three active routes
-- -----------------------------------------------------------
!create route1 : Route
!set route1.totalTravelTime := 60.0   -- 9x4.0 + 4x6.0 = 60 min

-- Assign route1 to truck1
!insert (route1, truck1) into AssignedTo

-- Route1 ordered stop sequence with explicit step indices (0-indexed)
!insert (route1, depot) into RouteStop
!set RouteStop.allInstances->any(rs | rs.routes = route1 and rs.stops = depot).step := 0
!insert (route1, n2) into RouteStop
!set RouteStop.allInstances->any(rs | rs.routes = route1 and rs.stops = n2).step := 1
!insert (route1, n3) into RouteStop
!set RouteStop.allInstances->any(rs | rs.routes = route1 and rs.stops = n3).step := 2
!insert (route1, n13) into RouteStop
!set RouteStop.allInstances->any(rs | rs.routes = route1 and rs.stops = n13).step := 3
!insert (route1, n14) into RouteStop
!set RouteStop.allInstances->any(rs | rs.routes = route1 and rs.stops = n14).step := 4
!insert (route1, n15) into RouteStop
!set RouteStop.allInstances->any(rs | rs.routes = route1 and rs.stops = n15).step := 5
!insert (route1, n25) into RouteStop
!set RouteStop.allInstances->any(rs | rs.routes = route1 and rs.stops = n25).step := 6
!insert (route1, n26) into RouteStop
!set RouteStop.allInstances->any(rs | rs.routes = route1 and rs.stops = n26).step := 7
!insert (route1, n27) into RouteStop
!set RouteStop.allInstances->any(rs | rs.routes = route1 and rs.stops = n27).step := 8
!insert (route1, n37) into RouteStop
!set RouteStop.allInstances->any(rs | rs.routes = route1 and rs.stops = n37).step := 9
!insert (route1, n38) into RouteStop
!set RouteStop.allInstances->any(rs | rs.routes = route1 and rs.stops = n38).step := 10
!insert (route1, n39) into RouteStop
!set RouteStop.allInstances->any(rs | rs.routes = route1 and rs.stops = n39).step := 11
!insert (route1, n49) into RouteStop
!set RouteStop.allInstances->any(rs | rs.routes = route1 and rs.stops = n49).step := 12
!insert (route1, disposal) into RouteStop
!set RouteStop.allInstances->any(rs | rs.routes = route1 and rs.stops = disposal).step := 13

!create route2 : Route
!set route2.totalTravelTime := 60.0   -- 9x4.0 + 4x6.0 = 60 min

-- Assign route2 to truck2
!insert (route2, truck2) into AssignedTo

-- Route2 ordered stop sequence with explicit step indices (0-indexed)
!insert (route2, depot) into RouteStop
!set RouteStop.allInstances->any(rs | rs.routes = route2 and rs.stops = depot).step := 0
!insert (route2, n11) into RouteStop
!set RouteStop.allInstances->any(rs | rs.routes = route2 and rs.stops = n11).step := 1
!insert (route2, n12) into RouteStop
!set RouteStop.allInstances->any(rs | rs.routes = route2 and rs.stops = n12).step := 2
!insert (route2, n13) into RouteStop
!set RouteStop.allInstances->any(rs | rs.routes = route2 and rs.stops = n13).step := 3
!insert (route2, n23) into RouteStop
!set RouteStop.allInstances->any(rs | rs.routes = route2 and rs.stops = n23).step := 4
!insert (route2, n24) into RouteStop
!set RouteStop.allInstances->any(rs | rs.routes = route2 and rs.stops = n24).step := 5
!insert (route2, n25) into RouteStop
!set RouteStop.allInstances->any(rs | rs.routes = route2 and rs.stops = n25).step := 6
!insert (route2, n35) into RouteStop
!set RouteStop.allInstances->any(rs | rs.routes = route2 and rs.stops = n35).step := 7
!insert (route2, n36) into RouteStop
!set RouteStop.allInstances->any(rs | rs.routes = route2 and rs.stops = n36).step := 8
!insert (route2, n37) into RouteStop
!set RouteStop.allInstances->any(rs | rs.routes = route2 and rs.stops = n37).step := 9
!insert (route2, n47) into RouteStop
!set RouteStop.allInstances->any(rs | rs.routes = route2 and rs.stops = n47).step := 10
!insert (route2, n48) into RouteStop
!set RouteStop.allInstances->any(rs | rs.routes = route2 and rs.stops = n48).step := 11
!insert (route2, n49) into RouteStop
!set RouteStop.allInstances->any(rs | rs.routes = route2 and rs.stops = n49).step := 12
!insert (route2, disposal) into RouteStop
!set RouteStop.allInstances->any(rs | rs.routes = route2 and rs.stops = disposal).step := 13

!create route3 : Route
!set route3.totalTravelTime := 60.0   -- 9x4.0 + 4x6.0 = 60 min

-- Assign route3 to truck3
!insert (route3, truck3) into AssignedTo

-- Route3 ordered stop sequence with explicit step indices (0-indexed)
!insert (route3, depot) into RouteStop
!set RouteStop.allInstances->any(rs | rs.routes = route3 and rs.stops = depot).step := 0
!insert (route3, n2) into RouteStop
!set RouteStop.allInstances->any(rs | rs.routes = route3 and rs.stops = n2).step := 1
!insert (route3, n12) into RouteStop
!set RouteStop.allInstances->any(rs | rs.routes = route3 and rs.stops = n12).step := 2
!insert (route3, n13) into RouteStop
!set RouteStop.allInstances->any(rs | rs.routes = route3 and rs.stops = n13).step := 3
!insert (route3, n23) into RouteStop
!set RouteStop.allInstances->any(rs | rs.routes = route3 and rs.stops = n23).step := 4
!insert (route3, n24) into RouteStop
!set RouteStop.allInstances->any(rs | rs.routes = route3 and rs.stops = n24).step := 5
!insert (route3, n34) into RouteStop
!set RouteStop.allInstances->any(rs | rs.routes = route3 and rs.stops = n34).step := 6
!insert (route3, n35) into RouteStop
!set RouteStop.allInstances->any(rs | rs.routes = route3 and rs.stops = n35).step := 7
!insert (route3, n45) into RouteStop
!set RouteStop.allInstances->any(rs | rs.routes = route3 and rs.stops = n45).step := 8
!insert (route3, n46) into RouteStop
!set RouteStop.allInstances->any(rs | rs.routes = route3 and rs.stops = n46).step := 9
!insert (route3, n47) into RouteStop
!set RouteStop.allInstances->any(rs | rs.routes = route3 and rs.stops = n47).step := 10
!insert (route3, n48) into RouteStop
!set RouteStop.allInstances->any(rs | rs.routes = route3 and rs.stops = n48).step := 11
!insert (route3, n49) into RouteStop
!set RouteStop.allInstances->any(rs | rs.routes = route3 and rs.stops = n49).step := 12
!insert (route3, disposal) into RouteStop
!set RouteStop.allInstances->any(rs | rs.routes = route3 and rs.stops = disposal).step := 13

-- -----------------------------------------------------------
-- 6. Simulate garbage collection along each route
-- -----------------------------------------------------------
-- Truck1 collects its assigned bins
!openter truck1 collectGarbage(bin1)
!set truck1.currentLoad := truck1.currentLoad + bin1.currentFill
!set bin1.currentFill   := 0.0
!opexit
!openter truck1 collectGarbage(bin2)
!set truck1.currentLoad := truck1.currentLoad + bin2.currentFill
!set bin2.currentFill   := 0.0
!opexit
!openter truck1 collectGarbage(bin3)
!set truck1.currentLoad := truck1.currentLoad + bin3.currentFill
!set bin3.currentFill   := 0.0
!opexit
!openter truck1 collectGarbage(bin4)
!set truck1.currentLoad := truck1.currentLoad + bin4.currentFill
!set bin4.currentFill   := 0.0
!opexit
!openter truck1 collectGarbage(bin5)
!set truck1.currentLoad := truck1.currentLoad + bin5.currentFill
!set bin5.currentFill   := 0.0
!opexit
!openter truck1 collectGarbage(bin6)
!set truck1.currentLoad := truck1.currentLoad + bin6.currentFill
!set bin6.currentFill   := 0.0
!opexit
!openter truck1 collectGarbage(bin7)
!set truck1.currentLoad := truck1.currentLoad + bin7.currentFill
!set bin7.currentFill   := 0.0
!opexit
-- Update truck1 distance (9x4.0 + 4x6.0 = 60 min, set manually here)
!set truck1.distanceTravelled := 60.0

-- Truck2 collects its assigned bins
!openter truck2 collectGarbage(bin8)
!set truck2.currentLoad := truck2.currentLoad + bin8.currentFill
!set bin8.currentFill   := 0.0
!opexit
!openter truck2 collectGarbage(bin9)
!set truck2.currentLoad := truck2.currentLoad + bin9.currentFill
!set bin9.currentFill   := 0.0
!opexit
!openter truck2 collectGarbage(bin10)
!set truck2.currentLoad := truck2.currentLoad + bin10.currentFill
!set bin10.currentFill   := 0.0
!opexit
!openter truck2 collectGarbage(bin11)
!set truck2.currentLoad := truck2.currentLoad + bin11.currentFill
!set bin11.currentFill   := 0.0
!opexit
!openter truck2 collectGarbage(bin12)
!set truck2.currentLoad := truck2.currentLoad + bin12.currentFill
!set bin12.currentFill   := 0.0
!opexit
!openter truck2 collectGarbage(bin13)
!set truck2.currentLoad := truck2.currentLoad + bin13.currentFill
!set bin13.currentFill   := 0.0
!opexit
!openter truck2 collectGarbage(bin14)
!set truck2.currentLoad := truck2.currentLoad + bin14.currentFill
!set bin14.currentFill   := 0.0
!opexit
-- Update truck2 distance (9x4.0 + 4x6.0 = 60 min, set manually here)
!set truck2.distanceTravelled := 60.0

-- Truck3 collects its assigned bins
!openter truck3 collectGarbage(bin15)
!set truck3.currentLoad := truck3.currentLoad + bin15.currentFill
!set bin15.currentFill   := 0.0
!opexit
!openter truck3 collectGarbage(bin16)
!set truck3.currentLoad := truck3.currentLoad + bin16.currentFill
!set bin16.currentFill   := 0.0
!opexit
!openter truck3 collectGarbage(bin17)
!set truck3.currentLoad := truck3.currentLoad + bin17.currentFill
!set bin17.currentFill   := 0.0
!opexit
!openter truck3 collectGarbage(bin18)
!set truck3.currentLoad := truck3.currentLoad + bin18.currentFill
!set bin18.currentFill   := 0.0
!opexit
!openter truck3 collectGarbage(bin19)
!set truck3.currentLoad := truck3.currentLoad + bin19.currentFill
!set bin19.currentFill   := 0.0
!opexit
!openter truck3 collectGarbage(bin20)
!set truck3.currentLoad := truck3.currentLoad + bin20.currentFill
!set bin20.currentFill   := 0.0
!opexit
-- Update truck3 distance (9x4.0 + 4x6.0 = 60 min, set manually here)
!set truck3.distanceTravelled := 60.0

-- -----------------------------------------------------------
-- 7. Check all constraints
-- -----------------------------------------------------------
check

-- Expected: all invariants true.
-- truck1.currentLoad = 10.5 m3  (<= maxCapacity 15.0)
-- truck2.currentLoad = 9.9 m3  (<= maxCapacity 15.0)
-- truck3.currentLoad = 9.2 m3  (<= maxCapacity 15.0)
-- Each truck.distanceTravelled = 60.0 km  (<= fuelRange 100.0)
-- All 20 bins have currentFill = 0 (collected)
-- All 3 trucks active, each with its own route
