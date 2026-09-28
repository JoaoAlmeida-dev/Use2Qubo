-- ===========================================================
-- GarbageTruckRoutingHuge.cmd
-- Animation script for the GarbageTruckRouting USE model.
--
-- Scenario: 202 nodes (1 depot, 200 intersections, 1 disposal),
--           200 garbage bins, 10 trucks, 10 routes, 500 roads.
--
-- Topology: 10 independent lanes of 20 intersections each, sharing only
-- the depot and disposal nodes. Route t is the simple path
-- depot -> laneT_1 -> ... -> laneT_20 -> disposal (21 edges, travelTime
-- 1.0 each, edgeCost() = 21.0), so lanes never interact through the
-- routeConnected flow-conservation invariant. Each lane node hosts one
-- garbage bin (200 bins total, one per route). Truck t is AssignedTo
-- route t only, so routeBinLoad()/edgeCost() per route are compared
-- against that single truck's maxCapacity/fuelRange.
--
-- The 210 spine edges are padded with 290 unused decorative roads (29
-- per lane: 18 skip-two shortcuts laneT_i->laneT_(i+2), plus 11 depot
-- shortcuts depot->laneT_k for k=2..12) to reach exactly 500 Road
-- instances. Decorative roads are never selected into RouteRoad, so
-- they only need positiveTravelTime > 0 and do not affect feasibility.
--
-- Load with: open GarbageTruckRoutingHuge.cmd
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
!create n50       : Node
!set n50.nodeId   := 50
!set n50.nodeType := #Intersection
!create n51       : Node
!set n51.nodeId   := 51
!set n51.nodeType := #Intersection
!create n52       : Node
!set n52.nodeId   := 52
!set n52.nodeType := #Intersection
!create n53       : Node
!set n53.nodeId   := 53
!set n53.nodeType := #Intersection
!create n54       : Node
!set n54.nodeId   := 54
!set n54.nodeType := #Intersection
!create n55       : Node
!set n55.nodeId   := 55
!set n55.nodeType := #Intersection
!create n56       : Node
!set n56.nodeId   := 56
!set n56.nodeType := #Intersection
!create n57       : Node
!set n57.nodeId   := 57
!set n57.nodeType := #Intersection
!create n58       : Node
!set n58.nodeId   := 58
!set n58.nodeType := #Intersection
!create n59       : Node
!set n59.nodeId   := 59
!set n59.nodeType := #Intersection
!create n60       : Node
!set n60.nodeId   := 60
!set n60.nodeType := #Intersection
!create n61       : Node
!set n61.nodeId   := 61
!set n61.nodeType := #Intersection
!create n62       : Node
!set n62.nodeId   := 62
!set n62.nodeType := #Intersection
!create n63       : Node
!set n63.nodeId   := 63
!set n63.nodeType := #Intersection
!create n64       : Node
!set n64.nodeId   := 64
!set n64.nodeType := #Intersection
!create n65       : Node
!set n65.nodeId   := 65
!set n65.nodeType := #Intersection
!create n66       : Node
!set n66.nodeId   := 66
!set n66.nodeType := #Intersection
!create n67       : Node
!set n67.nodeId   := 67
!set n67.nodeType := #Intersection
!create n68       : Node
!set n68.nodeId   := 68
!set n68.nodeType := #Intersection
!create n69       : Node
!set n69.nodeId   := 69
!set n69.nodeType := #Intersection
!create n70       : Node
!set n70.nodeId   := 70
!set n70.nodeType := #Intersection
!create n71       : Node
!set n71.nodeId   := 71
!set n71.nodeType := #Intersection
!create n72       : Node
!set n72.nodeId   := 72
!set n72.nodeType := #Intersection
!create n73       : Node
!set n73.nodeId   := 73
!set n73.nodeType := #Intersection
!create n74       : Node
!set n74.nodeId   := 74
!set n74.nodeType := #Intersection
!create n75       : Node
!set n75.nodeId   := 75
!set n75.nodeType := #Intersection
!create n76       : Node
!set n76.nodeId   := 76
!set n76.nodeType := #Intersection
!create n77       : Node
!set n77.nodeId   := 77
!set n77.nodeType := #Intersection
!create n78       : Node
!set n78.nodeId   := 78
!set n78.nodeType := #Intersection
!create n79       : Node
!set n79.nodeId   := 79
!set n79.nodeType := #Intersection
!create n80       : Node
!set n80.nodeId   := 80
!set n80.nodeType := #Intersection
!create n81       : Node
!set n81.nodeId   := 81
!set n81.nodeType := #Intersection
!create n82       : Node
!set n82.nodeId   := 82
!set n82.nodeType := #Intersection
!create n83       : Node
!set n83.nodeId   := 83
!set n83.nodeType := #Intersection
!create n84       : Node
!set n84.nodeId   := 84
!set n84.nodeType := #Intersection
!create n85       : Node
!set n85.nodeId   := 85
!set n85.nodeType := #Intersection
!create n86       : Node
!set n86.nodeId   := 86
!set n86.nodeType := #Intersection
!create n87       : Node
!set n87.nodeId   := 87
!set n87.nodeType := #Intersection
!create n88       : Node
!set n88.nodeId   := 88
!set n88.nodeType := #Intersection
!create n89       : Node
!set n89.nodeId   := 89
!set n89.nodeType := #Intersection
!create n90       : Node
!set n90.nodeId   := 90
!set n90.nodeType := #Intersection
!create n91       : Node
!set n91.nodeId   := 91
!set n91.nodeType := #Intersection
!create n92       : Node
!set n92.nodeId   := 92
!set n92.nodeType := #Intersection
!create n93       : Node
!set n93.nodeId   := 93
!set n93.nodeType := #Intersection
!create n94       : Node
!set n94.nodeId   := 94
!set n94.nodeType := #Intersection
!create n95       : Node
!set n95.nodeId   := 95
!set n95.nodeType := #Intersection
!create n96       : Node
!set n96.nodeId   := 96
!set n96.nodeType := #Intersection
!create n97       : Node
!set n97.nodeId   := 97
!set n97.nodeType := #Intersection
!create n98       : Node
!set n98.nodeId   := 98
!set n98.nodeType := #Intersection
!create n99       : Node
!set n99.nodeId   := 99
!set n99.nodeType := #Intersection
!create n100       : Node
!set n100.nodeId   := 100
!set n100.nodeType := #Intersection
!create n101       : Node
!set n101.nodeId   := 101
!set n101.nodeType := #Intersection
!create n102       : Node
!set n102.nodeId   := 102
!set n102.nodeType := #Intersection
!create n103       : Node
!set n103.nodeId   := 103
!set n103.nodeType := #Intersection
!create n104       : Node
!set n104.nodeId   := 104
!set n104.nodeType := #Intersection
!create n105       : Node
!set n105.nodeId   := 105
!set n105.nodeType := #Intersection
!create n106       : Node
!set n106.nodeId   := 106
!set n106.nodeType := #Intersection
!create n107       : Node
!set n107.nodeId   := 107
!set n107.nodeType := #Intersection
!create n108       : Node
!set n108.nodeId   := 108
!set n108.nodeType := #Intersection
!create n109       : Node
!set n109.nodeId   := 109
!set n109.nodeType := #Intersection
!create n110       : Node
!set n110.nodeId   := 110
!set n110.nodeType := #Intersection
!create n111       : Node
!set n111.nodeId   := 111
!set n111.nodeType := #Intersection
!create n112       : Node
!set n112.nodeId   := 112
!set n112.nodeType := #Intersection
!create n113       : Node
!set n113.nodeId   := 113
!set n113.nodeType := #Intersection
!create n114       : Node
!set n114.nodeId   := 114
!set n114.nodeType := #Intersection
!create n115       : Node
!set n115.nodeId   := 115
!set n115.nodeType := #Intersection
!create n116       : Node
!set n116.nodeId   := 116
!set n116.nodeType := #Intersection
!create n117       : Node
!set n117.nodeId   := 117
!set n117.nodeType := #Intersection
!create n118       : Node
!set n118.nodeId   := 118
!set n118.nodeType := #Intersection
!create n119       : Node
!set n119.nodeId   := 119
!set n119.nodeType := #Intersection
!create n120       : Node
!set n120.nodeId   := 120
!set n120.nodeType := #Intersection
!create n121       : Node
!set n121.nodeId   := 121
!set n121.nodeType := #Intersection
!create n122       : Node
!set n122.nodeId   := 122
!set n122.nodeType := #Intersection
!create n123       : Node
!set n123.nodeId   := 123
!set n123.nodeType := #Intersection
!create n124       : Node
!set n124.nodeId   := 124
!set n124.nodeType := #Intersection
!create n125       : Node
!set n125.nodeId   := 125
!set n125.nodeType := #Intersection
!create n126       : Node
!set n126.nodeId   := 126
!set n126.nodeType := #Intersection
!create n127       : Node
!set n127.nodeId   := 127
!set n127.nodeType := #Intersection
!create n128       : Node
!set n128.nodeId   := 128
!set n128.nodeType := #Intersection
!create n129       : Node
!set n129.nodeId   := 129
!set n129.nodeType := #Intersection
!create n130       : Node
!set n130.nodeId   := 130
!set n130.nodeType := #Intersection
!create n131       : Node
!set n131.nodeId   := 131
!set n131.nodeType := #Intersection
!create n132       : Node
!set n132.nodeId   := 132
!set n132.nodeType := #Intersection
!create n133       : Node
!set n133.nodeId   := 133
!set n133.nodeType := #Intersection
!create n134       : Node
!set n134.nodeId   := 134
!set n134.nodeType := #Intersection
!create n135       : Node
!set n135.nodeId   := 135
!set n135.nodeType := #Intersection
!create n136       : Node
!set n136.nodeId   := 136
!set n136.nodeType := #Intersection
!create n137       : Node
!set n137.nodeId   := 137
!set n137.nodeType := #Intersection
!create n138       : Node
!set n138.nodeId   := 138
!set n138.nodeType := #Intersection
!create n139       : Node
!set n139.nodeId   := 139
!set n139.nodeType := #Intersection
!create n140       : Node
!set n140.nodeId   := 140
!set n140.nodeType := #Intersection
!create n141       : Node
!set n141.nodeId   := 141
!set n141.nodeType := #Intersection
!create n142       : Node
!set n142.nodeId   := 142
!set n142.nodeType := #Intersection
!create n143       : Node
!set n143.nodeId   := 143
!set n143.nodeType := #Intersection
!create n144       : Node
!set n144.nodeId   := 144
!set n144.nodeType := #Intersection
!create n145       : Node
!set n145.nodeId   := 145
!set n145.nodeType := #Intersection
!create n146       : Node
!set n146.nodeId   := 146
!set n146.nodeType := #Intersection
!create n147       : Node
!set n147.nodeId   := 147
!set n147.nodeType := #Intersection
!create n148       : Node
!set n148.nodeId   := 148
!set n148.nodeType := #Intersection
!create n149       : Node
!set n149.nodeId   := 149
!set n149.nodeType := #Intersection
!create n150       : Node
!set n150.nodeId   := 150
!set n150.nodeType := #Intersection
!create n151       : Node
!set n151.nodeId   := 151
!set n151.nodeType := #Intersection
!create n152       : Node
!set n152.nodeId   := 152
!set n152.nodeType := #Intersection
!create n153       : Node
!set n153.nodeId   := 153
!set n153.nodeType := #Intersection
!create n154       : Node
!set n154.nodeId   := 154
!set n154.nodeType := #Intersection
!create n155       : Node
!set n155.nodeId   := 155
!set n155.nodeType := #Intersection
!create n156       : Node
!set n156.nodeId   := 156
!set n156.nodeType := #Intersection
!create n157       : Node
!set n157.nodeId   := 157
!set n157.nodeType := #Intersection
!create n158       : Node
!set n158.nodeId   := 158
!set n158.nodeType := #Intersection
!create n159       : Node
!set n159.nodeId   := 159
!set n159.nodeType := #Intersection
!create n160       : Node
!set n160.nodeId   := 160
!set n160.nodeType := #Intersection
!create n161       : Node
!set n161.nodeId   := 161
!set n161.nodeType := #Intersection
!create n162       : Node
!set n162.nodeId   := 162
!set n162.nodeType := #Intersection
!create n163       : Node
!set n163.nodeId   := 163
!set n163.nodeType := #Intersection
!create n164       : Node
!set n164.nodeId   := 164
!set n164.nodeType := #Intersection
!create n165       : Node
!set n165.nodeId   := 165
!set n165.nodeType := #Intersection
!create n166       : Node
!set n166.nodeId   := 166
!set n166.nodeType := #Intersection
!create n167       : Node
!set n167.nodeId   := 167
!set n167.nodeType := #Intersection
!create n168       : Node
!set n168.nodeId   := 168
!set n168.nodeType := #Intersection
!create n169       : Node
!set n169.nodeId   := 169
!set n169.nodeType := #Intersection
!create n170       : Node
!set n170.nodeId   := 170
!set n170.nodeType := #Intersection
!create n171       : Node
!set n171.nodeId   := 171
!set n171.nodeType := #Intersection
!create n172       : Node
!set n172.nodeId   := 172
!set n172.nodeType := #Intersection
!create n173       : Node
!set n173.nodeId   := 173
!set n173.nodeType := #Intersection
!create n174       : Node
!set n174.nodeId   := 174
!set n174.nodeType := #Intersection
!create n175       : Node
!set n175.nodeId   := 175
!set n175.nodeType := #Intersection
!create n176       : Node
!set n176.nodeId   := 176
!set n176.nodeType := #Intersection
!create n177       : Node
!set n177.nodeId   := 177
!set n177.nodeType := #Intersection
!create n178       : Node
!set n178.nodeId   := 178
!set n178.nodeType := #Intersection
!create n179       : Node
!set n179.nodeId   := 179
!set n179.nodeType := #Intersection
!create n180       : Node
!set n180.nodeId   := 180
!set n180.nodeType := #Intersection
!create n181       : Node
!set n181.nodeId   := 181
!set n181.nodeType := #Intersection
!create n182       : Node
!set n182.nodeId   := 182
!set n182.nodeType := #Intersection
!create n183       : Node
!set n183.nodeId   := 183
!set n183.nodeType := #Intersection
!create n184       : Node
!set n184.nodeId   := 184
!set n184.nodeType := #Intersection
!create n185       : Node
!set n185.nodeId   := 185
!set n185.nodeType := #Intersection
!create n186       : Node
!set n186.nodeId   := 186
!set n186.nodeType := #Intersection
!create n187       : Node
!set n187.nodeId   := 187
!set n187.nodeType := #Intersection
!create n188       : Node
!set n188.nodeId   := 188
!set n188.nodeType := #Intersection
!create n189       : Node
!set n189.nodeId   := 189
!set n189.nodeType := #Intersection
!create n190       : Node
!set n190.nodeId   := 190
!set n190.nodeType := #Intersection
!create n191       : Node
!set n191.nodeId   := 191
!set n191.nodeType := #Intersection
!create n192       : Node
!set n192.nodeId   := 192
!set n192.nodeType := #Intersection
!create n193       : Node
!set n193.nodeId   := 193
!set n193.nodeType := #Intersection
!create n194       : Node
!set n194.nodeId   := 194
!set n194.nodeType := #Intersection
!create n195       : Node
!set n195.nodeId   := 195
!set n195.nodeType := #Intersection
!create n196       : Node
!set n196.nodeId   := 196
!set n196.nodeType := #Intersection
!create n197       : Node
!set n197.nodeId   := 197
!set n197.nodeType := #Intersection
!create n198       : Node
!set n198.nodeId   := 198
!set n198.nodeType := #Intersection
!create n199       : Node
!set n199.nodeId   := 199
!set n199.nodeType := #Intersection
!create n200       : Node
!set n200.nodeId   := 200
!set n200.nodeType := #Intersection
!create n201       : Node
!set n201.nodeId   := 201
!set n201.nodeType := #Intersection

!create disposal       : Node
!set disposal.nodeId   := 202
!set disposal.nodeType := #DisposalFacility

-- -----------------------------------------------------------
-- 2. Create road network (directed edges with travel times)
-- -----------------------------------------------------------
!insert (depot, n2) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n2).travelTime := 1.0
!insert (n2, n3) into Road
!set Road.allInstances->any(r | r.origin = n2 and r.destination = n3).travelTime := 1.0
!insert (n3, n4) into Road
!set Road.allInstances->any(r | r.origin = n3 and r.destination = n4).travelTime := 1.0
!insert (n4, n5) into Road
!set Road.allInstances->any(r | r.origin = n4 and r.destination = n5).travelTime := 1.0
!insert (n5, n6) into Road
!set Road.allInstances->any(r | r.origin = n5 and r.destination = n6).travelTime := 1.0
!insert (n6, n7) into Road
!set Road.allInstances->any(r | r.origin = n6 and r.destination = n7).travelTime := 1.0
!insert (n7, n8) into Road
!set Road.allInstances->any(r | r.origin = n7 and r.destination = n8).travelTime := 1.0
!insert (n8, n9) into Road
!set Road.allInstances->any(r | r.origin = n8 and r.destination = n9).travelTime := 1.0
!insert (n9, n10) into Road
!set Road.allInstances->any(r | r.origin = n9 and r.destination = n10).travelTime := 1.0
!insert (n10, n11) into Road
!set Road.allInstances->any(r | r.origin = n10 and r.destination = n11).travelTime := 1.0
!insert (n11, n12) into Road
!set Road.allInstances->any(r | r.origin = n11 and r.destination = n12).travelTime := 1.0
!insert (n12, n13) into Road
!set Road.allInstances->any(r | r.origin = n12 and r.destination = n13).travelTime := 1.0
!insert (n13, n14) into Road
!set Road.allInstances->any(r | r.origin = n13 and r.destination = n14).travelTime := 1.0
!insert (n14, n15) into Road
!set Road.allInstances->any(r | r.origin = n14 and r.destination = n15).travelTime := 1.0
!insert (n15, n16) into Road
!set Road.allInstances->any(r | r.origin = n15 and r.destination = n16).travelTime := 1.0
!insert (n16, n17) into Road
!set Road.allInstances->any(r | r.origin = n16 and r.destination = n17).travelTime := 1.0
!insert (n17, n18) into Road
!set Road.allInstances->any(r | r.origin = n17 and r.destination = n18).travelTime := 1.0
!insert (n18, n19) into Road
!set Road.allInstances->any(r | r.origin = n18 and r.destination = n19).travelTime := 1.0
!insert (n19, n20) into Road
!set Road.allInstances->any(r | r.origin = n19 and r.destination = n20).travelTime := 1.0
!insert (n20, n21) into Road
!set Road.allInstances->any(r | r.origin = n20 and r.destination = n21).travelTime := 1.0
!insert (n21, disposal) into Road
!set Road.allInstances->any(r | r.origin = n21 and r.destination = disposal).travelTime := 1.0
!insert (n2, n4) into Road
!set Road.allInstances->any(r | r.origin = n2 and r.destination = n4).travelTime := 2.0
!insert (n3, n5) into Road
!set Road.allInstances->any(r | r.origin = n3 and r.destination = n5).travelTime := 2.0
!insert (n4, n6) into Road
!set Road.allInstances->any(r | r.origin = n4 and r.destination = n6).travelTime := 2.0
!insert (n5, n7) into Road
!set Road.allInstances->any(r | r.origin = n5 and r.destination = n7).travelTime := 2.0
!insert (n6, n8) into Road
!set Road.allInstances->any(r | r.origin = n6 and r.destination = n8).travelTime := 2.0
!insert (n7, n9) into Road
!set Road.allInstances->any(r | r.origin = n7 and r.destination = n9).travelTime := 2.0
!insert (n8, n10) into Road
!set Road.allInstances->any(r | r.origin = n8 and r.destination = n10).travelTime := 2.0
!insert (n9, n11) into Road
!set Road.allInstances->any(r | r.origin = n9 and r.destination = n11).travelTime := 2.0
!insert (n10, n12) into Road
!set Road.allInstances->any(r | r.origin = n10 and r.destination = n12).travelTime := 2.0
!insert (n11, n13) into Road
!set Road.allInstances->any(r | r.origin = n11 and r.destination = n13).travelTime := 2.0
!insert (n12, n14) into Road
!set Road.allInstances->any(r | r.origin = n12 and r.destination = n14).travelTime := 2.0
!insert (n13, n15) into Road
!set Road.allInstances->any(r | r.origin = n13 and r.destination = n15).travelTime := 2.0
!insert (n14, n16) into Road
!set Road.allInstances->any(r | r.origin = n14 and r.destination = n16).travelTime := 2.0
!insert (n15, n17) into Road
!set Road.allInstances->any(r | r.origin = n15 and r.destination = n17).travelTime := 2.0
!insert (n16, n18) into Road
!set Road.allInstances->any(r | r.origin = n16 and r.destination = n18).travelTime := 2.0
!insert (n17, n19) into Road
!set Road.allInstances->any(r | r.origin = n17 and r.destination = n19).travelTime := 2.0
!insert (n18, n20) into Road
!set Road.allInstances->any(r | r.origin = n18 and r.destination = n20).travelTime := 2.0
!insert (n19, n21) into Road
!set Road.allInstances->any(r | r.origin = n19 and r.destination = n21).travelTime := 2.0
!insert (depot, n3) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n3).travelTime := 3.0
!insert (depot, n4) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n4).travelTime := 3.0
!insert (depot, n5) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n5).travelTime := 3.0
!insert (depot, n6) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n6).travelTime := 3.0
!insert (depot, n7) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n7).travelTime := 3.0
!insert (depot, n8) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n8).travelTime := 3.0
!insert (depot, n9) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n9).travelTime := 3.0
!insert (depot, n10) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n10).travelTime := 3.0
!insert (depot, n11) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n11).travelTime := 3.0
!insert (depot, n12) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n12).travelTime := 3.0
!insert (depot, n13) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n13).travelTime := 3.0
!insert (depot, n22) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n22).travelTime := 1.0
!insert (n22, n23) into Road
!set Road.allInstances->any(r | r.origin = n22 and r.destination = n23).travelTime := 1.0
!insert (n23, n24) into Road
!set Road.allInstances->any(r | r.origin = n23 and r.destination = n24).travelTime := 1.0
!insert (n24, n25) into Road
!set Road.allInstances->any(r | r.origin = n24 and r.destination = n25).travelTime := 1.0
!insert (n25, n26) into Road
!set Road.allInstances->any(r | r.origin = n25 and r.destination = n26).travelTime := 1.0
!insert (n26, n27) into Road
!set Road.allInstances->any(r | r.origin = n26 and r.destination = n27).travelTime := 1.0
!insert (n27, n28) into Road
!set Road.allInstances->any(r | r.origin = n27 and r.destination = n28).travelTime := 1.0
!insert (n28, n29) into Road
!set Road.allInstances->any(r | r.origin = n28 and r.destination = n29).travelTime := 1.0
!insert (n29, n30) into Road
!set Road.allInstances->any(r | r.origin = n29 and r.destination = n30).travelTime := 1.0
!insert (n30, n31) into Road
!set Road.allInstances->any(r | r.origin = n30 and r.destination = n31).travelTime := 1.0
!insert (n31, n32) into Road
!set Road.allInstances->any(r | r.origin = n31 and r.destination = n32).travelTime := 1.0
!insert (n32, n33) into Road
!set Road.allInstances->any(r | r.origin = n32 and r.destination = n33).travelTime := 1.0
!insert (n33, n34) into Road
!set Road.allInstances->any(r | r.origin = n33 and r.destination = n34).travelTime := 1.0
!insert (n34, n35) into Road
!set Road.allInstances->any(r | r.origin = n34 and r.destination = n35).travelTime := 1.0
!insert (n35, n36) into Road
!set Road.allInstances->any(r | r.origin = n35 and r.destination = n36).travelTime := 1.0
!insert (n36, n37) into Road
!set Road.allInstances->any(r | r.origin = n36 and r.destination = n37).travelTime := 1.0
!insert (n37, n38) into Road
!set Road.allInstances->any(r | r.origin = n37 and r.destination = n38).travelTime := 1.0
!insert (n38, n39) into Road
!set Road.allInstances->any(r | r.origin = n38 and r.destination = n39).travelTime := 1.0
!insert (n39, n40) into Road
!set Road.allInstances->any(r | r.origin = n39 and r.destination = n40).travelTime := 1.0
!insert (n40, n41) into Road
!set Road.allInstances->any(r | r.origin = n40 and r.destination = n41).travelTime := 1.0
!insert (n41, disposal) into Road
!set Road.allInstances->any(r | r.origin = n41 and r.destination = disposal).travelTime := 1.0
!insert (n22, n24) into Road
!set Road.allInstances->any(r | r.origin = n22 and r.destination = n24).travelTime := 2.0
!insert (n23, n25) into Road
!set Road.allInstances->any(r | r.origin = n23 and r.destination = n25).travelTime := 2.0
!insert (n24, n26) into Road
!set Road.allInstances->any(r | r.origin = n24 and r.destination = n26).travelTime := 2.0
!insert (n25, n27) into Road
!set Road.allInstances->any(r | r.origin = n25 and r.destination = n27).travelTime := 2.0
!insert (n26, n28) into Road
!set Road.allInstances->any(r | r.origin = n26 and r.destination = n28).travelTime := 2.0
!insert (n27, n29) into Road
!set Road.allInstances->any(r | r.origin = n27 and r.destination = n29).travelTime := 2.0
!insert (n28, n30) into Road
!set Road.allInstances->any(r | r.origin = n28 and r.destination = n30).travelTime := 2.0
!insert (n29, n31) into Road
!set Road.allInstances->any(r | r.origin = n29 and r.destination = n31).travelTime := 2.0
!insert (n30, n32) into Road
!set Road.allInstances->any(r | r.origin = n30 and r.destination = n32).travelTime := 2.0
!insert (n31, n33) into Road
!set Road.allInstances->any(r | r.origin = n31 and r.destination = n33).travelTime := 2.0
!insert (n32, n34) into Road
!set Road.allInstances->any(r | r.origin = n32 and r.destination = n34).travelTime := 2.0
!insert (n33, n35) into Road
!set Road.allInstances->any(r | r.origin = n33 and r.destination = n35).travelTime := 2.0
!insert (n34, n36) into Road
!set Road.allInstances->any(r | r.origin = n34 and r.destination = n36).travelTime := 2.0
!insert (n35, n37) into Road
!set Road.allInstances->any(r | r.origin = n35 and r.destination = n37).travelTime := 2.0
!insert (n36, n38) into Road
!set Road.allInstances->any(r | r.origin = n36 and r.destination = n38).travelTime := 2.0
!insert (n37, n39) into Road
!set Road.allInstances->any(r | r.origin = n37 and r.destination = n39).travelTime := 2.0
!insert (n38, n40) into Road
!set Road.allInstances->any(r | r.origin = n38 and r.destination = n40).travelTime := 2.0
!insert (n39, n41) into Road
!set Road.allInstances->any(r | r.origin = n39 and r.destination = n41).travelTime := 2.0
!insert (depot, n23) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n23).travelTime := 3.0
!insert (depot, n24) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n24).travelTime := 3.0
!insert (depot, n25) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n25).travelTime := 3.0
!insert (depot, n26) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n26).travelTime := 3.0
!insert (depot, n27) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n27).travelTime := 3.0
!insert (depot, n28) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n28).travelTime := 3.0
!insert (depot, n29) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n29).travelTime := 3.0
!insert (depot, n30) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n30).travelTime := 3.0
!insert (depot, n31) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n31).travelTime := 3.0
!insert (depot, n32) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n32).travelTime := 3.0
!insert (depot, n33) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n33).travelTime := 3.0
!insert (depot, n42) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n42).travelTime := 1.0
!insert (n42, n43) into Road
!set Road.allInstances->any(r | r.origin = n42 and r.destination = n43).travelTime := 1.0
!insert (n43, n44) into Road
!set Road.allInstances->any(r | r.origin = n43 and r.destination = n44).travelTime := 1.0
!insert (n44, n45) into Road
!set Road.allInstances->any(r | r.origin = n44 and r.destination = n45).travelTime := 1.0
!insert (n45, n46) into Road
!set Road.allInstances->any(r | r.origin = n45 and r.destination = n46).travelTime := 1.0
!insert (n46, n47) into Road
!set Road.allInstances->any(r | r.origin = n46 and r.destination = n47).travelTime := 1.0
!insert (n47, n48) into Road
!set Road.allInstances->any(r | r.origin = n47 and r.destination = n48).travelTime := 1.0
!insert (n48, n49) into Road
!set Road.allInstances->any(r | r.origin = n48 and r.destination = n49).travelTime := 1.0
!insert (n49, n50) into Road
!set Road.allInstances->any(r | r.origin = n49 and r.destination = n50).travelTime := 1.0
!insert (n50, n51) into Road
!set Road.allInstances->any(r | r.origin = n50 and r.destination = n51).travelTime := 1.0
!insert (n51, n52) into Road
!set Road.allInstances->any(r | r.origin = n51 and r.destination = n52).travelTime := 1.0
!insert (n52, n53) into Road
!set Road.allInstances->any(r | r.origin = n52 and r.destination = n53).travelTime := 1.0
!insert (n53, n54) into Road
!set Road.allInstances->any(r | r.origin = n53 and r.destination = n54).travelTime := 1.0
!insert (n54, n55) into Road
!set Road.allInstances->any(r | r.origin = n54 and r.destination = n55).travelTime := 1.0
!insert (n55, n56) into Road
!set Road.allInstances->any(r | r.origin = n55 and r.destination = n56).travelTime := 1.0
!insert (n56, n57) into Road
!set Road.allInstances->any(r | r.origin = n56 and r.destination = n57).travelTime := 1.0
!insert (n57, n58) into Road
!set Road.allInstances->any(r | r.origin = n57 and r.destination = n58).travelTime := 1.0
!insert (n58, n59) into Road
!set Road.allInstances->any(r | r.origin = n58 and r.destination = n59).travelTime := 1.0
!insert (n59, n60) into Road
!set Road.allInstances->any(r | r.origin = n59 and r.destination = n60).travelTime := 1.0
!insert (n60, n61) into Road
!set Road.allInstances->any(r | r.origin = n60 and r.destination = n61).travelTime := 1.0
!insert (n61, disposal) into Road
!set Road.allInstances->any(r | r.origin = n61 and r.destination = disposal).travelTime := 1.0
!insert (n42, n44) into Road
!set Road.allInstances->any(r | r.origin = n42 and r.destination = n44).travelTime := 2.0
!insert (n43, n45) into Road
!set Road.allInstances->any(r | r.origin = n43 and r.destination = n45).travelTime := 2.0
!insert (n44, n46) into Road
!set Road.allInstances->any(r | r.origin = n44 and r.destination = n46).travelTime := 2.0
!insert (n45, n47) into Road
!set Road.allInstances->any(r | r.origin = n45 and r.destination = n47).travelTime := 2.0
!insert (n46, n48) into Road
!set Road.allInstances->any(r | r.origin = n46 and r.destination = n48).travelTime := 2.0
!insert (n47, n49) into Road
!set Road.allInstances->any(r | r.origin = n47 and r.destination = n49).travelTime := 2.0
!insert (n48, n50) into Road
!set Road.allInstances->any(r | r.origin = n48 and r.destination = n50).travelTime := 2.0
!insert (n49, n51) into Road
!set Road.allInstances->any(r | r.origin = n49 and r.destination = n51).travelTime := 2.0
!insert (n50, n52) into Road
!set Road.allInstances->any(r | r.origin = n50 and r.destination = n52).travelTime := 2.0
!insert (n51, n53) into Road
!set Road.allInstances->any(r | r.origin = n51 and r.destination = n53).travelTime := 2.0
!insert (n52, n54) into Road
!set Road.allInstances->any(r | r.origin = n52 and r.destination = n54).travelTime := 2.0
!insert (n53, n55) into Road
!set Road.allInstances->any(r | r.origin = n53 and r.destination = n55).travelTime := 2.0
!insert (n54, n56) into Road
!set Road.allInstances->any(r | r.origin = n54 and r.destination = n56).travelTime := 2.0
!insert (n55, n57) into Road
!set Road.allInstances->any(r | r.origin = n55 and r.destination = n57).travelTime := 2.0
!insert (n56, n58) into Road
!set Road.allInstances->any(r | r.origin = n56 and r.destination = n58).travelTime := 2.0
!insert (n57, n59) into Road
!set Road.allInstances->any(r | r.origin = n57 and r.destination = n59).travelTime := 2.0
!insert (n58, n60) into Road
!set Road.allInstances->any(r | r.origin = n58 and r.destination = n60).travelTime := 2.0
!insert (n59, n61) into Road
!set Road.allInstances->any(r | r.origin = n59 and r.destination = n61).travelTime := 2.0
!insert (depot, n43) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n43).travelTime := 3.0
!insert (depot, n44) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n44).travelTime := 3.0
!insert (depot, n45) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n45).travelTime := 3.0
!insert (depot, n46) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n46).travelTime := 3.0
!insert (depot, n47) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n47).travelTime := 3.0
!insert (depot, n48) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n48).travelTime := 3.0
!insert (depot, n49) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n49).travelTime := 3.0
!insert (depot, n50) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n50).travelTime := 3.0
!insert (depot, n51) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n51).travelTime := 3.0
!insert (depot, n52) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n52).travelTime := 3.0
!insert (depot, n53) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n53).travelTime := 3.0
!insert (depot, n62) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n62).travelTime := 1.0
!insert (n62, n63) into Road
!set Road.allInstances->any(r | r.origin = n62 and r.destination = n63).travelTime := 1.0
!insert (n63, n64) into Road
!set Road.allInstances->any(r | r.origin = n63 and r.destination = n64).travelTime := 1.0
!insert (n64, n65) into Road
!set Road.allInstances->any(r | r.origin = n64 and r.destination = n65).travelTime := 1.0
!insert (n65, n66) into Road
!set Road.allInstances->any(r | r.origin = n65 and r.destination = n66).travelTime := 1.0
!insert (n66, n67) into Road
!set Road.allInstances->any(r | r.origin = n66 and r.destination = n67).travelTime := 1.0
!insert (n67, n68) into Road
!set Road.allInstances->any(r | r.origin = n67 and r.destination = n68).travelTime := 1.0
!insert (n68, n69) into Road
!set Road.allInstances->any(r | r.origin = n68 and r.destination = n69).travelTime := 1.0
!insert (n69, n70) into Road
!set Road.allInstances->any(r | r.origin = n69 and r.destination = n70).travelTime := 1.0
!insert (n70, n71) into Road
!set Road.allInstances->any(r | r.origin = n70 and r.destination = n71).travelTime := 1.0
!insert (n71, n72) into Road
!set Road.allInstances->any(r | r.origin = n71 and r.destination = n72).travelTime := 1.0
!insert (n72, n73) into Road
!set Road.allInstances->any(r | r.origin = n72 and r.destination = n73).travelTime := 1.0
!insert (n73, n74) into Road
!set Road.allInstances->any(r | r.origin = n73 and r.destination = n74).travelTime := 1.0
!insert (n74, n75) into Road
!set Road.allInstances->any(r | r.origin = n74 and r.destination = n75).travelTime := 1.0
!insert (n75, n76) into Road
!set Road.allInstances->any(r | r.origin = n75 and r.destination = n76).travelTime := 1.0
!insert (n76, n77) into Road
!set Road.allInstances->any(r | r.origin = n76 and r.destination = n77).travelTime := 1.0
!insert (n77, n78) into Road
!set Road.allInstances->any(r | r.origin = n77 and r.destination = n78).travelTime := 1.0
!insert (n78, n79) into Road
!set Road.allInstances->any(r | r.origin = n78 and r.destination = n79).travelTime := 1.0
!insert (n79, n80) into Road
!set Road.allInstances->any(r | r.origin = n79 and r.destination = n80).travelTime := 1.0
!insert (n80, n81) into Road
!set Road.allInstances->any(r | r.origin = n80 and r.destination = n81).travelTime := 1.0
!insert (n81, disposal) into Road
!set Road.allInstances->any(r | r.origin = n81 and r.destination = disposal).travelTime := 1.0
!insert (n62, n64) into Road
!set Road.allInstances->any(r | r.origin = n62 and r.destination = n64).travelTime := 2.0
!insert (n63, n65) into Road
!set Road.allInstances->any(r | r.origin = n63 and r.destination = n65).travelTime := 2.0
!insert (n64, n66) into Road
!set Road.allInstances->any(r | r.origin = n64 and r.destination = n66).travelTime := 2.0
!insert (n65, n67) into Road
!set Road.allInstances->any(r | r.origin = n65 and r.destination = n67).travelTime := 2.0
!insert (n66, n68) into Road
!set Road.allInstances->any(r | r.origin = n66 and r.destination = n68).travelTime := 2.0
!insert (n67, n69) into Road
!set Road.allInstances->any(r | r.origin = n67 and r.destination = n69).travelTime := 2.0
!insert (n68, n70) into Road
!set Road.allInstances->any(r | r.origin = n68 and r.destination = n70).travelTime := 2.0
!insert (n69, n71) into Road
!set Road.allInstances->any(r | r.origin = n69 and r.destination = n71).travelTime := 2.0
!insert (n70, n72) into Road
!set Road.allInstances->any(r | r.origin = n70 and r.destination = n72).travelTime := 2.0
!insert (n71, n73) into Road
!set Road.allInstances->any(r | r.origin = n71 and r.destination = n73).travelTime := 2.0
!insert (n72, n74) into Road
!set Road.allInstances->any(r | r.origin = n72 and r.destination = n74).travelTime := 2.0
!insert (n73, n75) into Road
!set Road.allInstances->any(r | r.origin = n73 and r.destination = n75).travelTime := 2.0
!insert (n74, n76) into Road
!set Road.allInstances->any(r | r.origin = n74 and r.destination = n76).travelTime := 2.0
!insert (n75, n77) into Road
!set Road.allInstances->any(r | r.origin = n75 and r.destination = n77).travelTime := 2.0
!insert (n76, n78) into Road
!set Road.allInstances->any(r | r.origin = n76 and r.destination = n78).travelTime := 2.0
!insert (n77, n79) into Road
!set Road.allInstances->any(r | r.origin = n77 and r.destination = n79).travelTime := 2.0
!insert (n78, n80) into Road
!set Road.allInstances->any(r | r.origin = n78 and r.destination = n80).travelTime := 2.0
!insert (n79, n81) into Road
!set Road.allInstances->any(r | r.origin = n79 and r.destination = n81).travelTime := 2.0
!insert (depot, n63) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n63).travelTime := 3.0
!insert (depot, n64) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n64).travelTime := 3.0
!insert (depot, n65) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n65).travelTime := 3.0
!insert (depot, n66) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n66).travelTime := 3.0
!insert (depot, n67) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n67).travelTime := 3.0
!insert (depot, n68) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n68).travelTime := 3.0
!insert (depot, n69) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n69).travelTime := 3.0
!insert (depot, n70) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n70).travelTime := 3.0
!insert (depot, n71) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n71).travelTime := 3.0
!insert (depot, n72) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n72).travelTime := 3.0
!insert (depot, n73) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n73).travelTime := 3.0
!insert (depot, n82) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n82).travelTime := 1.0
!insert (n82, n83) into Road
!set Road.allInstances->any(r | r.origin = n82 and r.destination = n83).travelTime := 1.0
!insert (n83, n84) into Road
!set Road.allInstances->any(r | r.origin = n83 and r.destination = n84).travelTime := 1.0
!insert (n84, n85) into Road
!set Road.allInstances->any(r | r.origin = n84 and r.destination = n85).travelTime := 1.0
!insert (n85, n86) into Road
!set Road.allInstances->any(r | r.origin = n85 and r.destination = n86).travelTime := 1.0
!insert (n86, n87) into Road
!set Road.allInstances->any(r | r.origin = n86 and r.destination = n87).travelTime := 1.0
!insert (n87, n88) into Road
!set Road.allInstances->any(r | r.origin = n87 and r.destination = n88).travelTime := 1.0
!insert (n88, n89) into Road
!set Road.allInstances->any(r | r.origin = n88 and r.destination = n89).travelTime := 1.0
!insert (n89, n90) into Road
!set Road.allInstances->any(r | r.origin = n89 and r.destination = n90).travelTime := 1.0
!insert (n90, n91) into Road
!set Road.allInstances->any(r | r.origin = n90 and r.destination = n91).travelTime := 1.0
!insert (n91, n92) into Road
!set Road.allInstances->any(r | r.origin = n91 and r.destination = n92).travelTime := 1.0
!insert (n92, n93) into Road
!set Road.allInstances->any(r | r.origin = n92 and r.destination = n93).travelTime := 1.0
!insert (n93, n94) into Road
!set Road.allInstances->any(r | r.origin = n93 and r.destination = n94).travelTime := 1.0
!insert (n94, n95) into Road
!set Road.allInstances->any(r | r.origin = n94 and r.destination = n95).travelTime := 1.0
!insert (n95, n96) into Road
!set Road.allInstances->any(r | r.origin = n95 and r.destination = n96).travelTime := 1.0
!insert (n96, n97) into Road
!set Road.allInstances->any(r | r.origin = n96 and r.destination = n97).travelTime := 1.0
!insert (n97, n98) into Road
!set Road.allInstances->any(r | r.origin = n97 and r.destination = n98).travelTime := 1.0
!insert (n98, n99) into Road
!set Road.allInstances->any(r | r.origin = n98 and r.destination = n99).travelTime := 1.0
!insert (n99, n100) into Road
!set Road.allInstances->any(r | r.origin = n99 and r.destination = n100).travelTime := 1.0
!insert (n100, n101) into Road
!set Road.allInstances->any(r | r.origin = n100 and r.destination = n101).travelTime := 1.0
!insert (n101, disposal) into Road
!set Road.allInstances->any(r | r.origin = n101 and r.destination = disposal).travelTime := 1.0
!insert (n82, n84) into Road
!set Road.allInstances->any(r | r.origin = n82 and r.destination = n84).travelTime := 2.0
!insert (n83, n85) into Road
!set Road.allInstances->any(r | r.origin = n83 and r.destination = n85).travelTime := 2.0
!insert (n84, n86) into Road
!set Road.allInstances->any(r | r.origin = n84 and r.destination = n86).travelTime := 2.0
!insert (n85, n87) into Road
!set Road.allInstances->any(r | r.origin = n85 and r.destination = n87).travelTime := 2.0
!insert (n86, n88) into Road
!set Road.allInstances->any(r | r.origin = n86 and r.destination = n88).travelTime := 2.0
!insert (n87, n89) into Road
!set Road.allInstances->any(r | r.origin = n87 and r.destination = n89).travelTime := 2.0
!insert (n88, n90) into Road
!set Road.allInstances->any(r | r.origin = n88 and r.destination = n90).travelTime := 2.0
!insert (n89, n91) into Road
!set Road.allInstances->any(r | r.origin = n89 and r.destination = n91).travelTime := 2.0
!insert (n90, n92) into Road
!set Road.allInstances->any(r | r.origin = n90 and r.destination = n92).travelTime := 2.0
!insert (n91, n93) into Road
!set Road.allInstances->any(r | r.origin = n91 and r.destination = n93).travelTime := 2.0
!insert (n92, n94) into Road
!set Road.allInstances->any(r | r.origin = n92 and r.destination = n94).travelTime := 2.0
!insert (n93, n95) into Road
!set Road.allInstances->any(r | r.origin = n93 and r.destination = n95).travelTime := 2.0
!insert (n94, n96) into Road
!set Road.allInstances->any(r | r.origin = n94 and r.destination = n96).travelTime := 2.0
!insert (n95, n97) into Road
!set Road.allInstances->any(r | r.origin = n95 and r.destination = n97).travelTime := 2.0
!insert (n96, n98) into Road
!set Road.allInstances->any(r | r.origin = n96 and r.destination = n98).travelTime := 2.0
!insert (n97, n99) into Road
!set Road.allInstances->any(r | r.origin = n97 and r.destination = n99).travelTime := 2.0
!insert (n98, n100) into Road
!set Road.allInstances->any(r | r.origin = n98 and r.destination = n100).travelTime := 2.0
!insert (n99, n101) into Road
!set Road.allInstances->any(r | r.origin = n99 and r.destination = n101).travelTime := 2.0
!insert (depot, n83) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n83).travelTime := 3.0
!insert (depot, n84) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n84).travelTime := 3.0
!insert (depot, n85) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n85).travelTime := 3.0
!insert (depot, n86) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n86).travelTime := 3.0
!insert (depot, n87) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n87).travelTime := 3.0
!insert (depot, n88) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n88).travelTime := 3.0
!insert (depot, n89) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n89).travelTime := 3.0
!insert (depot, n90) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n90).travelTime := 3.0
!insert (depot, n91) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n91).travelTime := 3.0
!insert (depot, n92) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n92).travelTime := 3.0
!insert (depot, n93) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n93).travelTime := 3.0
!insert (depot, n102) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n102).travelTime := 1.0
!insert (n102, n103) into Road
!set Road.allInstances->any(r | r.origin = n102 and r.destination = n103).travelTime := 1.0
!insert (n103, n104) into Road
!set Road.allInstances->any(r | r.origin = n103 and r.destination = n104).travelTime := 1.0
!insert (n104, n105) into Road
!set Road.allInstances->any(r | r.origin = n104 and r.destination = n105).travelTime := 1.0
!insert (n105, n106) into Road
!set Road.allInstances->any(r | r.origin = n105 and r.destination = n106).travelTime := 1.0
!insert (n106, n107) into Road
!set Road.allInstances->any(r | r.origin = n106 and r.destination = n107).travelTime := 1.0
!insert (n107, n108) into Road
!set Road.allInstances->any(r | r.origin = n107 and r.destination = n108).travelTime := 1.0
!insert (n108, n109) into Road
!set Road.allInstances->any(r | r.origin = n108 and r.destination = n109).travelTime := 1.0
!insert (n109, n110) into Road
!set Road.allInstances->any(r | r.origin = n109 and r.destination = n110).travelTime := 1.0
!insert (n110, n111) into Road
!set Road.allInstances->any(r | r.origin = n110 and r.destination = n111).travelTime := 1.0
!insert (n111, n112) into Road
!set Road.allInstances->any(r | r.origin = n111 and r.destination = n112).travelTime := 1.0
!insert (n112, n113) into Road
!set Road.allInstances->any(r | r.origin = n112 and r.destination = n113).travelTime := 1.0
!insert (n113, n114) into Road
!set Road.allInstances->any(r | r.origin = n113 and r.destination = n114).travelTime := 1.0
!insert (n114, n115) into Road
!set Road.allInstances->any(r | r.origin = n114 and r.destination = n115).travelTime := 1.0
!insert (n115, n116) into Road
!set Road.allInstances->any(r | r.origin = n115 and r.destination = n116).travelTime := 1.0
!insert (n116, n117) into Road
!set Road.allInstances->any(r | r.origin = n116 and r.destination = n117).travelTime := 1.0
!insert (n117, n118) into Road
!set Road.allInstances->any(r | r.origin = n117 and r.destination = n118).travelTime := 1.0
!insert (n118, n119) into Road
!set Road.allInstances->any(r | r.origin = n118 and r.destination = n119).travelTime := 1.0
!insert (n119, n120) into Road
!set Road.allInstances->any(r | r.origin = n119 and r.destination = n120).travelTime := 1.0
!insert (n120, n121) into Road
!set Road.allInstances->any(r | r.origin = n120 and r.destination = n121).travelTime := 1.0
!insert (n121, disposal) into Road
!set Road.allInstances->any(r | r.origin = n121 and r.destination = disposal).travelTime := 1.0
!insert (n102, n104) into Road
!set Road.allInstances->any(r | r.origin = n102 and r.destination = n104).travelTime := 2.0
!insert (n103, n105) into Road
!set Road.allInstances->any(r | r.origin = n103 and r.destination = n105).travelTime := 2.0
!insert (n104, n106) into Road
!set Road.allInstances->any(r | r.origin = n104 and r.destination = n106).travelTime := 2.0
!insert (n105, n107) into Road
!set Road.allInstances->any(r | r.origin = n105 and r.destination = n107).travelTime := 2.0
!insert (n106, n108) into Road
!set Road.allInstances->any(r | r.origin = n106 and r.destination = n108).travelTime := 2.0
!insert (n107, n109) into Road
!set Road.allInstances->any(r | r.origin = n107 and r.destination = n109).travelTime := 2.0
!insert (n108, n110) into Road
!set Road.allInstances->any(r | r.origin = n108 and r.destination = n110).travelTime := 2.0
!insert (n109, n111) into Road
!set Road.allInstances->any(r | r.origin = n109 and r.destination = n111).travelTime := 2.0
!insert (n110, n112) into Road
!set Road.allInstances->any(r | r.origin = n110 and r.destination = n112).travelTime := 2.0
!insert (n111, n113) into Road
!set Road.allInstances->any(r | r.origin = n111 and r.destination = n113).travelTime := 2.0
!insert (n112, n114) into Road
!set Road.allInstances->any(r | r.origin = n112 and r.destination = n114).travelTime := 2.0
!insert (n113, n115) into Road
!set Road.allInstances->any(r | r.origin = n113 and r.destination = n115).travelTime := 2.0
!insert (n114, n116) into Road
!set Road.allInstances->any(r | r.origin = n114 and r.destination = n116).travelTime := 2.0
!insert (n115, n117) into Road
!set Road.allInstances->any(r | r.origin = n115 and r.destination = n117).travelTime := 2.0
!insert (n116, n118) into Road
!set Road.allInstances->any(r | r.origin = n116 and r.destination = n118).travelTime := 2.0
!insert (n117, n119) into Road
!set Road.allInstances->any(r | r.origin = n117 and r.destination = n119).travelTime := 2.0
!insert (n118, n120) into Road
!set Road.allInstances->any(r | r.origin = n118 and r.destination = n120).travelTime := 2.0
!insert (n119, n121) into Road
!set Road.allInstances->any(r | r.origin = n119 and r.destination = n121).travelTime := 2.0
!insert (depot, n103) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n103).travelTime := 3.0
!insert (depot, n104) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n104).travelTime := 3.0
!insert (depot, n105) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n105).travelTime := 3.0
!insert (depot, n106) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n106).travelTime := 3.0
!insert (depot, n107) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n107).travelTime := 3.0
!insert (depot, n108) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n108).travelTime := 3.0
!insert (depot, n109) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n109).travelTime := 3.0
!insert (depot, n110) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n110).travelTime := 3.0
!insert (depot, n111) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n111).travelTime := 3.0
!insert (depot, n112) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n112).travelTime := 3.0
!insert (depot, n113) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n113).travelTime := 3.0
!insert (depot, n122) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n122).travelTime := 1.0
!insert (n122, n123) into Road
!set Road.allInstances->any(r | r.origin = n122 and r.destination = n123).travelTime := 1.0
!insert (n123, n124) into Road
!set Road.allInstances->any(r | r.origin = n123 and r.destination = n124).travelTime := 1.0
!insert (n124, n125) into Road
!set Road.allInstances->any(r | r.origin = n124 and r.destination = n125).travelTime := 1.0
!insert (n125, n126) into Road
!set Road.allInstances->any(r | r.origin = n125 and r.destination = n126).travelTime := 1.0
!insert (n126, n127) into Road
!set Road.allInstances->any(r | r.origin = n126 and r.destination = n127).travelTime := 1.0
!insert (n127, n128) into Road
!set Road.allInstances->any(r | r.origin = n127 and r.destination = n128).travelTime := 1.0
!insert (n128, n129) into Road
!set Road.allInstances->any(r | r.origin = n128 and r.destination = n129).travelTime := 1.0
!insert (n129, n130) into Road
!set Road.allInstances->any(r | r.origin = n129 and r.destination = n130).travelTime := 1.0
!insert (n130, n131) into Road
!set Road.allInstances->any(r | r.origin = n130 and r.destination = n131).travelTime := 1.0
!insert (n131, n132) into Road
!set Road.allInstances->any(r | r.origin = n131 and r.destination = n132).travelTime := 1.0
!insert (n132, n133) into Road
!set Road.allInstances->any(r | r.origin = n132 and r.destination = n133).travelTime := 1.0
!insert (n133, n134) into Road
!set Road.allInstances->any(r | r.origin = n133 and r.destination = n134).travelTime := 1.0
!insert (n134, n135) into Road
!set Road.allInstances->any(r | r.origin = n134 and r.destination = n135).travelTime := 1.0
!insert (n135, n136) into Road
!set Road.allInstances->any(r | r.origin = n135 and r.destination = n136).travelTime := 1.0
!insert (n136, n137) into Road
!set Road.allInstances->any(r | r.origin = n136 and r.destination = n137).travelTime := 1.0
!insert (n137, n138) into Road
!set Road.allInstances->any(r | r.origin = n137 and r.destination = n138).travelTime := 1.0
!insert (n138, n139) into Road
!set Road.allInstances->any(r | r.origin = n138 and r.destination = n139).travelTime := 1.0
!insert (n139, n140) into Road
!set Road.allInstances->any(r | r.origin = n139 and r.destination = n140).travelTime := 1.0
!insert (n140, n141) into Road
!set Road.allInstances->any(r | r.origin = n140 and r.destination = n141).travelTime := 1.0
!insert (n141, disposal) into Road
!set Road.allInstances->any(r | r.origin = n141 and r.destination = disposal).travelTime := 1.0
!insert (n122, n124) into Road
!set Road.allInstances->any(r | r.origin = n122 and r.destination = n124).travelTime := 2.0
!insert (n123, n125) into Road
!set Road.allInstances->any(r | r.origin = n123 and r.destination = n125).travelTime := 2.0
!insert (n124, n126) into Road
!set Road.allInstances->any(r | r.origin = n124 and r.destination = n126).travelTime := 2.0
!insert (n125, n127) into Road
!set Road.allInstances->any(r | r.origin = n125 and r.destination = n127).travelTime := 2.0
!insert (n126, n128) into Road
!set Road.allInstances->any(r | r.origin = n126 and r.destination = n128).travelTime := 2.0
!insert (n127, n129) into Road
!set Road.allInstances->any(r | r.origin = n127 and r.destination = n129).travelTime := 2.0
!insert (n128, n130) into Road
!set Road.allInstances->any(r | r.origin = n128 and r.destination = n130).travelTime := 2.0
!insert (n129, n131) into Road
!set Road.allInstances->any(r | r.origin = n129 and r.destination = n131).travelTime := 2.0
!insert (n130, n132) into Road
!set Road.allInstances->any(r | r.origin = n130 and r.destination = n132).travelTime := 2.0
!insert (n131, n133) into Road
!set Road.allInstances->any(r | r.origin = n131 and r.destination = n133).travelTime := 2.0
!insert (n132, n134) into Road
!set Road.allInstances->any(r | r.origin = n132 and r.destination = n134).travelTime := 2.0
!insert (n133, n135) into Road
!set Road.allInstances->any(r | r.origin = n133 and r.destination = n135).travelTime := 2.0
!insert (n134, n136) into Road
!set Road.allInstances->any(r | r.origin = n134 and r.destination = n136).travelTime := 2.0
!insert (n135, n137) into Road
!set Road.allInstances->any(r | r.origin = n135 and r.destination = n137).travelTime := 2.0
!insert (n136, n138) into Road
!set Road.allInstances->any(r | r.origin = n136 and r.destination = n138).travelTime := 2.0
!insert (n137, n139) into Road
!set Road.allInstances->any(r | r.origin = n137 and r.destination = n139).travelTime := 2.0
!insert (n138, n140) into Road
!set Road.allInstances->any(r | r.origin = n138 and r.destination = n140).travelTime := 2.0
!insert (n139, n141) into Road
!set Road.allInstances->any(r | r.origin = n139 and r.destination = n141).travelTime := 2.0
!insert (depot, n123) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n123).travelTime := 3.0
!insert (depot, n124) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n124).travelTime := 3.0
!insert (depot, n125) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n125).travelTime := 3.0
!insert (depot, n126) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n126).travelTime := 3.0
!insert (depot, n127) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n127).travelTime := 3.0
!insert (depot, n128) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n128).travelTime := 3.0
!insert (depot, n129) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n129).travelTime := 3.0
!insert (depot, n130) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n130).travelTime := 3.0
!insert (depot, n131) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n131).travelTime := 3.0
!insert (depot, n132) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n132).travelTime := 3.0
!insert (depot, n133) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n133).travelTime := 3.0
!insert (depot, n142) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n142).travelTime := 1.0
!insert (n142, n143) into Road
!set Road.allInstances->any(r | r.origin = n142 and r.destination = n143).travelTime := 1.0
!insert (n143, n144) into Road
!set Road.allInstances->any(r | r.origin = n143 and r.destination = n144).travelTime := 1.0
!insert (n144, n145) into Road
!set Road.allInstances->any(r | r.origin = n144 and r.destination = n145).travelTime := 1.0
!insert (n145, n146) into Road
!set Road.allInstances->any(r | r.origin = n145 and r.destination = n146).travelTime := 1.0
!insert (n146, n147) into Road
!set Road.allInstances->any(r | r.origin = n146 and r.destination = n147).travelTime := 1.0
!insert (n147, n148) into Road
!set Road.allInstances->any(r | r.origin = n147 and r.destination = n148).travelTime := 1.0
!insert (n148, n149) into Road
!set Road.allInstances->any(r | r.origin = n148 and r.destination = n149).travelTime := 1.0
!insert (n149, n150) into Road
!set Road.allInstances->any(r | r.origin = n149 and r.destination = n150).travelTime := 1.0
!insert (n150, n151) into Road
!set Road.allInstances->any(r | r.origin = n150 and r.destination = n151).travelTime := 1.0
!insert (n151, n152) into Road
!set Road.allInstances->any(r | r.origin = n151 and r.destination = n152).travelTime := 1.0
!insert (n152, n153) into Road
!set Road.allInstances->any(r | r.origin = n152 and r.destination = n153).travelTime := 1.0
!insert (n153, n154) into Road
!set Road.allInstances->any(r | r.origin = n153 and r.destination = n154).travelTime := 1.0
!insert (n154, n155) into Road
!set Road.allInstances->any(r | r.origin = n154 and r.destination = n155).travelTime := 1.0
!insert (n155, n156) into Road
!set Road.allInstances->any(r | r.origin = n155 and r.destination = n156).travelTime := 1.0
!insert (n156, n157) into Road
!set Road.allInstances->any(r | r.origin = n156 and r.destination = n157).travelTime := 1.0
!insert (n157, n158) into Road
!set Road.allInstances->any(r | r.origin = n157 and r.destination = n158).travelTime := 1.0
!insert (n158, n159) into Road
!set Road.allInstances->any(r | r.origin = n158 and r.destination = n159).travelTime := 1.0
!insert (n159, n160) into Road
!set Road.allInstances->any(r | r.origin = n159 and r.destination = n160).travelTime := 1.0
!insert (n160, n161) into Road
!set Road.allInstances->any(r | r.origin = n160 and r.destination = n161).travelTime := 1.0
!insert (n161, disposal) into Road
!set Road.allInstances->any(r | r.origin = n161 and r.destination = disposal).travelTime := 1.0
!insert (n142, n144) into Road
!set Road.allInstances->any(r | r.origin = n142 and r.destination = n144).travelTime := 2.0
!insert (n143, n145) into Road
!set Road.allInstances->any(r | r.origin = n143 and r.destination = n145).travelTime := 2.0
!insert (n144, n146) into Road
!set Road.allInstances->any(r | r.origin = n144 and r.destination = n146).travelTime := 2.0
!insert (n145, n147) into Road
!set Road.allInstances->any(r | r.origin = n145 and r.destination = n147).travelTime := 2.0
!insert (n146, n148) into Road
!set Road.allInstances->any(r | r.origin = n146 and r.destination = n148).travelTime := 2.0
!insert (n147, n149) into Road
!set Road.allInstances->any(r | r.origin = n147 and r.destination = n149).travelTime := 2.0
!insert (n148, n150) into Road
!set Road.allInstances->any(r | r.origin = n148 and r.destination = n150).travelTime := 2.0
!insert (n149, n151) into Road
!set Road.allInstances->any(r | r.origin = n149 and r.destination = n151).travelTime := 2.0
!insert (n150, n152) into Road
!set Road.allInstances->any(r | r.origin = n150 and r.destination = n152).travelTime := 2.0
!insert (n151, n153) into Road
!set Road.allInstances->any(r | r.origin = n151 and r.destination = n153).travelTime := 2.0
!insert (n152, n154) into Road
!set Road.allInstances->any(r | r.origin = n152 and r.destination = n154).travelTime := 2.0
!insert (n153, n155) into Road
!set Road.allInstances->any(r | r.origin = n153 and r.destination = n155).travelTime := 2.0
!insert (n154, n156) into Road
!set Road.allInstances->any(r | r.origin = n154 and r.destination = n156).travelTime := 2.0
!insert (n155, n157) into Road
!set Road.allInstances->any(r | r.origin = n155 and r.destination = n157).travelTime := 2.0
!insert (n156, n158) into Road
!set Road.allInstances->any(r | r.origin = n156 and r.destination = n158).travelTime := 2.0
!insert (n157, n159) into Road
!set Road.allInstances->any(r | r.origin = n157 and r.destination = n159).travelTime := 2.0
!insert (n158, n160) into Road
!set Road.allInstances->any(r | r.origin = n158 and r.destination = n160).travelTime := 2.0
!insert (n159, n161) into Road
!set Road.allInstances->any(r | r.origin = n159 and r.destination = n161).travelTime := 2.0
!insert (depot, n143) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n143).travelTime := 3.0
!insert (depot, n144) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n144).travelTime := 3.0
!insert (depot, n145) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n145).travelTime := 3.0
!insert (depot, n146) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n146).travelTime := 3.0
!insert (depot, n147) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n147).travelTime := 3.0
!insert (depot, n148) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n148).travelTime := 3.0
!insert (depot, n149) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n149).travelTime := 3.0
!insert (depot, n150) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n150).travelTime := 3.0
!insert (depot, n151) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n151).travelTime := 3.0
!insert (depot, n152) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n152).travelTime := 3.0
!insert (depot, n153) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n153).travelTime := 3.0
!insert (depot, n162) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n162).travelTime := 1.0
!insert (n162, n163) into Road
!set Road.allInstances->any(r | r.origin = n162 and r.destination = n163).travelTime := 1.0
!insert (n163, n164) into Road
!set Road.allInstances->any(r | r.origin = n163 and r.destination = n164).travelTime := 1.0
!insert (n164, n165) into Road
!set Road.allInstances->any(r | r.origin = n164 and r.destination = n165).travelTime := 1.0
!insert (n165, n166) into Road
!set Road.allInstances->any(r | r.origin = n165 and r.destination = n166).travelTime := 1.0
!insert (n166, n167) into Road
!set Road.allInstances->any(r | r.origin = n166 and r.destination = n167).travelTime := 1.0
!insert (n167, n168) into Road
!set Road.allInstances->any(r | r.origin = n167 and r.destination = n168).travelTime := 1.0
!insert (n168, n169) into Road
!set Road.allInstances->any(r | r.origin = n168 and r.destination = n169).travelTime := 1.0
!insert (n169, n170) into Road
!set Road.allInstances->any(r | r.origin = n169 and r.destination = n170).travelTime := 1.0
!insert (n170, n171) into Road
!set Road.allInstances->any(r | r.origin = n170 and r.destination = n171).travelTime := 1.0
!insert (n171, n172) into Road
!set Road.allInstances->any(r | r.origin = n171 and r.destination = n172).travelTime := 1.0
!insert (n172, n173) into Road
!set Road.allInstances->any(r | r.origin = n172 and r.destination = n173).travelTime := 1.0
!insert (n173, n174) into Road
!set Road.allInstances->any(r | r.origin = n173 and r.destination = n174).travelTime := 1.0
!insert (n174, n175) into Road
!set Road.allInstances->any(r | r.origin = n174 and r.destination = n175).travelTime := 1.0
!insert (n175, n176) into Road
!set Road.allInstances->any(r | r.origin = n175 and r.destination = n176).travelTime := 1.0
!insert (n176, n177) into Road
!set Road.allInstances->any(r | r.origin = n176 and r.destination = n177).travelTime := 1.0
!insert (n177, n178) into Road
!set Road.allInstances->any(r | r.origin = n177 and r.destination = n178).travelTime := 1.0
!insert (n178, n179) into Road
!set Road.allInstances->any(r | r.origin = n178 and r.destination = n179).travelTime := 1.0
!insert (n179, n180) into Road
!set Road.allInstances->any(r | r.origin = n179 and r.destination = n180).travelTime := 1.0
!insert (n180, n181) into Road
!set Road.allInstances->any(r | r.origin = n180 and r.destination = n181).travelTime := 1.0
!insert (n181, disposal) into Road
!set Road.allInstances->any(r | r.origin = n181 and r.destination = disposal).travelTime := 1.0
!insert (n162, n164) into Road
!set Road.allInstances->any(r | r.origin = n162 and r.destination = n164).travelTime := 2.0
!insert (n163, n165) into Road
!set Road.allInstances->any(r | r.origin = n163 and r.destination = n165).travelTime := 2.0
!insert (n164, n166) into Road
!set Road.allInstances->any(r | r.origin = n164 and r.destination = n166).travelTime := 2.0
!insert (n165, n167) into Road
!set Road.allInstances->any(r | r.origin = n165 and r.destination = n167).travelTime := 2.0
!insert (n166, n168) into Road
!set Road.allInstances->any(r | r.origin = n166 and r.destination = n168).travelTime := 2.0
!insert (n167, n169) into Road
!set Road.allInstances->any(r | r.origin = n167 and r.destination = n169).travelTime := 2.0
!insert (n168, n170) into Road
!set Road.allInstances->any(r | r.origin = n168 and r.destination = n170).travelTime := 2.0
!insert (n169, n171) into Road
!set Road.allInstances->any(r | r.origin = n169 and r.destination = n171).travelTime := 2.0
!insert (n170, n172) into Road
!set Road.allInstances->any(r | r.origin = n170 and r.destination = n172).travelTime := 2.0
!insert (n171, n173) into Road
!set Road.allInstances->any(r | r.origin = n171 and r.destination = n173).travelTime := 2.0
!insert (n172, n174) into Road
!set Road.allInstances->any(r | r.origin = n172 and r.destination = n174).travelTime := 2.0
!insert (n173, n175) into Road
!set Road.allInstances->any(r | r.origin = n173 and r.destination = n175).travelTime := 2.0
!insert (n174, n176) into Road
!set Road.allInstances->any(r | r.origin = n174 and r.destination = n176).travelTime := 2.0
!insert (n175, n177) into Road
!set Road.allInstances->any(r | r.origin = n175 and r.destination = n177).travelTime := 2.0
!insert (n176, n178) into Road
!set Road.allInstances->any(r | r.origin = n176 and r.destination = n178).travelTime := 2.0
!insert (n177, n179) into Road
!set Road.allInstances->any(r | r.origin = n177 and r.destination = n179).travelTime := 2.0
!insert (n178, n180) into Road
!set Road.allInstances->any(r | r.origin = n178 and r.destination = n180).travelTime := 2.0
!insert (n179, n181) into Road
!set Road.allInstances->any(r | r.origin = n179 and r.destination = n181).travelTime := 2.0
!insert (depot, n163) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n163).travelTime := 3.0
!insert (depot, n164) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n164).travelTime := 3.0
!insert (depot, n165) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n165).travelTime := 3.0
!insert (depot, n166) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n166).travelTime := 3.0
!insert (depot, n167) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n167).travelTime := 3.0
!insert (depot, n168) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n168).travelTime := 3.0
!insert (depot, n169) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n169).travelTime := 3.0
!insert (depot, n170) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n170).travelTime := 3.0
!insert (depot, n171) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n171).travelTime := 3.0
!insert (depot, n172) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n172).travelTime := 3.0
!insert (depot, n173) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n173).travelTime := 3.0
!insert (depot, n182) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n182).travelTime := 1.0
!insert (n182, n183) into Road
!set Road.allInstances->any(r | r.origin = n182 and r.destination = n183).travelTime := 1.0
!insert (n183, n184) into Road
!set Road.allInstances->any(r | r.origin = n183 and r.destination = n184).travelTime := 1.0
!insert (n184, n185) into Road
!set Road.allInstances->any(r | r.origin = n184 and r.destination = n185).travelTime := 1.0
!insert (n185, n186) into Road
!set Road.allInstances->any(r | r.origin = n185 and r.destination = n186).travelTime := 1.0
!insert (n186, n187) into Road
!set Road.allInstances->any(r | r.origin = n186 and r.destination = n187).travelTime := 1.0
!insert (n187, n188) into Road
!set Road.allInstances->any(r | r.origin = n187 and r.destination = n188).travelTime := 1.0
!insert (n188, n189) into Road
!set Road.allInstances->any(r | r.origin = n188 and r.destination = n189).travelTime := 1.0
!insert (n189, n190) into Road
!set Road.allInstances->any(r | r.origin = n189 and r.destination = n190).travelTime := 1.0
!insert (n190, n191) into Road
!set Road.allInstances->any(r | r.origin = n190 and r.destination = n191).travelTime := 1.0
!insert (n191, n192) into Road
!set Road.allInstances->any(r | r.origin = n191 and r.destination = n192).travelTime := 1.0
!insert (n192, n193) into Road
!set Road.allInstances->any(r | r.origin = n192 and r.destination = n193).travelTime := 1.0
!insert (n193, n194) into Road
!set Road.allInstances->any(r | r.origin = n193 and r.destination = n194).travelTime := 1.0
!insert (n194, n195) into Road
!set Road.allInstances->any(r | r.origin = n194 and r.destination = n195).travelTime := 1.0
!insert (n195, n196) into Road
!set Road.allInstances->any(r | r.origin = n195 and r.destination = n196).travelTime := 1.0
!insert (n196, n197) into Road
!set Road.allInstances->any(r | r.origin = n196 and r.destination = n197).travelTime := 1.0
!insert (n197, n198) into Road
!set Road.allInstances->any(r | r.origin = n197 and r.destination = n198).travelTime := 1.0
!insert (n198, n199) into Road
!set Road.allInstances->any(r | r.origin = n198 and r.destination = n199).travelTime := 1.0
!insert (n199, n200) into Road
!set Road.allInstances->any(r | r.origin = n199 and r.destination = n200).travelTime := 1.0
!insert (n200, n201) into Road
!set Road.allInstances->any(r | r.origin = n200 and r.destination = n201).travelTime := 1.0
!insert (n201, disposal) into Road
!set Road.allInstances->any(r | r.origin = n201 and r.destination = disposal).travelTime := 1.0
!insert (n182, n184) into Road
!set Road.allInstances->any(r | r.origin = n182 and r.destination = n184).travelTime := 2.0
!insert (n183, n185) into Road
!set Road.allInstances->any(r | r.origin = n183 and r.destination = n185).travelTime := 2.0
!insert (n184, n186) into Road
!set Road.allInstances->any(r | r.origin = n184 and r.destination = n186).travelTime := 2.0
!insert (n185, n187) into Road
!set Road.allInstances->any(r | r.origin = n185 and r.destination = n187).travelTime := 2.0
!insert (n186, n188) into Road
!set Road.allInstances->any(r | r.origin = n186 and r.destination = n188).travelTime := 2.0
!insert (n187, n189) into Road
!set Road.allInstances->any(r | r.origin = n187 and r.destination = n189).travelTime := 2.0
!insert (n188, n190) into Road
!set Road.allInstances->any(r | r.origin = n188 and r.destination = n190).travelTime := 2.0
!insert (n189, n191) into Road
!set Road.allInstances->any(r | r.origin = n189 and r.destination = n191).travelTime := 2.0
!insert (n190, n192) into Road
!set Road.allInstances->any(r | r.origin = n190 and r.destination = n192).travelTime := 2.0
!insert (n191, n193) into Road
!set Road.allInstances->any(r | r.origin = n191 and r.destination = n193).travelTime := 2.0
!insert (n192, n194) into Road
!set Road.allInstances->any(r | r.origin = n192 and r.destination = n194).travelTime := 2.0
!insert (n193, n195) into Road
!set Road.allInstances->any(r | r.origin = n193 and r.destination = n195).travelTime := 2.0
!insert (n194, n196) into Road
!set Road.allInstances->any(r | r.origin = n194 and r.destination = n196).travelTime := 2.0
!insert (n195, n197) into Road
!set Road.allInstances->any(r | r.origin = n195 and r.destination = n197).travelTime := 2.0
!insert (n196, n198) into Road
!set Road.allInstances->any(r | r.origin = n196 and r.destination = n198).travelTime := 2.0
!insert (n197, n199) into Road
!set Road.allInstances->any(r | r.origin = n197 and r.destination = n199).travelTime := 2.0
!insert (n198, n200) into Road
!set Road.allInstances->any(r | r.origin = n198 and r.destination = n200).travelTime := 2.0
!insert (n199, n201) into Road
!set Road.allInstances->any(r | r.origin = n199 and r.destination = n201).travelTime := 2.0
!insert (depot, n183) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n183).travelTime := 3.0
!insert (depot, n184) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n184).travelTime := 3.0
!insert (depot, n185) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n185).travelTime := 3.0
!insert (depot, n186) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n186).travelTime := 3.0
!insert (depot, n187) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n187).travelTime := 3.0
!insert (depot, n188) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n188).travelTime := 3.0
!insert (depot, n189) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n189).travelTime := 3.0
!insert (depot, n190) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n190).travelTime := 3.0
!insert (depot, n191) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n191).travelTime := 3.0
!insert (depot, n192) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n192).travelTime := 3.0
!insert (depot, n193) into Road
!set Road.allInstances->any(r | r.origin = depot and r.destination = n193).travelTime := 3.0

-- -----------------------------------------------------------
-- 3. Create garbage bins (200 total, one per lane intersection)
-- -----------------------------------------------------------
!create bin1       : GarbageBin
!set bin1.maxFill     := 1.0
!set bin1.currentFill := 0.18
!create bin2       : GarbageBin
!set bin2.maxFill     := 1.0
!set bin2.currentFill := 0.22
!create bin3       : GarbageBin
!set bin3.maxFill     := 1.0
!set bin3.currentFill := 0.26
!create bin4       : GarbageBin
!set bin4.maxFill     := 1.0
!set bin4.currentFill := 0.3
!create bin5       : GarbageBin
!set bin5.maxFill     := 1.0
!set bin5.currentFill := 0.34
!create bin6       : GarbageBin
!set bin6.maxFill     := 1.0
!set bin6.currentFill := 0.38
!create bin7       : GarbageBin
!set bin7.maxFill     := 1.0
!set bin7.currentFill := 0.42
!create bin8       : GarbageBin
!set bin8.maxFill     := 1.0
!set bin8.currentFill := 0.46
!create bin9       : GarbageBin
!set bin9.maxFill     := 1.0
!set bin9.currentFill := 0.5
!create bin10       : GarbageBin
!set bin10.maxFill     := 1.0
!set bin10.currentFill := 0.54
!create bin11       : GarbageBin
!set bin11.maxFill     := 1.0
!set bin11.currentFill := 0.58
!create bin12       : GarbageBin
!set bin12.maxFill     := 1.0
!set bin12.currentFill := 0.62
!create bin13       : GarbageBin
!set bin13.maxFill     := 1.0
!set bin13.currentFill := 0.66
!create bin14       : GarbageBin
!set bin14.maxFill     := 1.0
!set bin14.currentFill := 0.7
!create bin15       : GarbageBin
!set bin15.maxFill     := 1.0
!set bin15.currentFill := 0.74
!create bin16       : GarbageBin
!set bin16.maxFill     := 1.0
!set bin16.currentFill := 0.78
!create bin17       : GarbageBin
!set bin17.maxFill     := 1.0
!set bin17.currentFill := 0.82
!create bin18       : GarbageBin
!set bin18.maxFill     := 1.0
!set bin18.currentFill := 0.86
!create bin19       : GarbageBin
!set bin19.maxFill     := 1.0
!set bin19.currentFill := 0.1
!create bin20       : GarbageBin
!set bin20.maxFill     := 1.0
!set bin20.currentFill := 0.14
!create bin21       : GarbageBin
!set bin21.maxFill     := 1.0
!set bin21.currentFill := 0.22
!create bin22       : GarbageBin
!set bin22.maxFill     := 1.0
!set bin22.currentFill := 0.26
!create bin23       : GarbageBin
!set bin23.maxFill     := 1.0
!set bin23.currentFill := 0.3
!create bin24       : GarbageBin
!set bin24.maxFill     := 1.0
!set bin24.currentFill := 0.34
!create bin25       : GarbageBin
!set bin25.maxFill     := 1.0
!set bin25.currentFill := 0.38
!create bin26       : GarbageBin
!set bin26.maxFill     := 1.0
!set bin26.currentFill := 0.42
!create bin27       : GarbageBin
!set bin27.maxFill     := 1.0
!set bin27.currentFill := 0.46
!create bin28       : GarbageBin
!set bin28.maxFill     := 1.0
!set bin28.currentFill := 0.5
!create bin29       : GarbageBin
!set bin29.maxFill     := 1.0
!set bin29.currentFill := 0.54
!create bin30       : GarbageBin
!set bin30.maxFill     := 1.0
!set bin30.currentFill := 0.58
!create bin31       : GarbageBin
!set bin31.maxFill     := 1.0
!set bin31.currentFill := 0.62
!create bin32       : GarbageBin
!set bin32.maxFill     := 1.0
!set bin32.currentFill := 0.66
!create bin33       : GarbageBin
!set bin33.maxFill     := 1.0
!set bin33.currentFill := 0.7
!create bin34       : GarbageBin
!set bin34.maxFill     := 1.0
!set bin34.currentFill := 0.74
!create bin35       : GarbageBin
!set bin35.maxFill     := 1.0
!set bin35.currentFill := 0.78
!create bin36       : GarbageBin
!set bin36.maxFill     := 1.0
!set bin36.currentFill := 0.82
!create bin37       : GarbageBin
!set bin37.maxFill     := 1.0
!set bin37.currentFill := 0.86
!create bin38       : GarbageBin
!set bin38.maxFill     := 1.0
!set bin38.currentFill := 0.1
!create bin39       : GarbageBin
!set bin39.maxFill     := 1.0
!set bin39.currentFill := 0.14
!create bin40       : GarbageBin
!set bin40.maxFill     := 1.0
!set bin40.currentFill := 0.18
!create bin41       : GarbageBin
!set bin41.maxFill     := 1.0
!set bin41.currentFill := 0.26
!create bin42       : GarbageBin
!set bin42.maxFill     := 1.0
!set bin42.currentFill := 0.3
!create bin43       : GarbageBin
!set bin43.maxFill     := 1.0
!set bin43.currentFill := 0.34
!create bin44       : GarbageBin
!set bin44.maxFill     := 1.0
!set bin44.currentFill := 0.38
!create bin45       : GarbageBin
!set bin45.maxFill     := 1.0
!set bin45.currentFill := 0.42
!create bin46       : GarbageBin
!set bin46.maxFill     := 1.0
!set bin46.currentFill := 0.46
!create bin47       : GarbageBin
!set bin47.maxFill     := 1.0
!set bin47.currentFill := 0.5
!create bin48       : GarbageBin
!set bin48.maxFill     := 1.0
!set bin48.currentFill := 0.54
!create bin49       : GarbageBin
!set bin49.maxFill     := 1.0
!set bin49.currentFill := 0.58
!create bin50       : GarbageBin
!set bin50.maxFill     := 1.0
!set bin50.currentFill := 0.62
!create bin51       : GarbageBin
!set bin51.maxFill     := 1.0
!set bin51.currentFill := 0.66
!create bin52       : GarbageBin
!set bin52.maxFill     := 1.0
!set bin52.currentFill := 0.7
!create bin53       : GarbageBin
!set bin53.maxFill     := 1.0
!set bin53.currentFill := 0.74
!create bin54       : GarbageBin
!set bin54.maxFill     := 1.0
!set bin54.currentFill := 0.78
!create bin55       : GarbageBin
!set bin55.maxFill     := 1.0
!set bin55.currentFill := 0.82
!create bin56       : GarbageBin
!set bin56.maxFill     := 1.0
!set bin56.currentFill := 0.86
!create bin57       : GarbageBin
!set bin57.maxFill     := 1.0
!set bin57.currentFill := 0.1
!create bin58       : GarbageBin
!set bin58.maxFill     := 1.0
!set bin58.currentFill := 0.14
!create bin59       : GarbageBin
!set bin59.maxFill     := 1.0
!set bin59.currentFill := 0.18
!create bin60       : GarbageBin
!set bin60.maxFill     := 1.0
!set bin60.currentFill := 0.22
!create bin61       : GarbageBin
!set bin61.maxFill     := 1.0
!set bin61.currentFill := 0.3
!create bin62       : GarbageBin
!set bin62.maxFill     := 1.0
!set bin62.currentFill := 0.34
!create bin63       : GarbageBin
!set bin63.maxFill     := 1.0
!set bin63.currentFill := 0.38
!create bin64       : GarbageBin
!set bin64.maxFill     := 1.0
!set bin64.currentFill := 0.42
!create bin65       : GarbageBin
!set bin65.maxFill     := 1.0
!set bin65.currentFill := 0.46
!create bin66       : GarbageBin
!set bin66.maxFill     := 1.0
!set bin66.currentFill := 0.5
!create bin67       : GarbageBin
!set bin67.maxFill     := 1.0
!set bin67.currentFill := 0.54
!create bin68       : GarbageBin
!set bin68.maxFill     := 1.0
!set bin68.currentFill := 0.58
!create bin69       : GarbageBin
!set bin69.maxFill     := 1.0
!set bin69.currentFill := 0.62
!create bin70       : GarbageBin
!set bin70.maxFill     := 1.0
!set bin70.currentFill := 0.66
!create bin71       : GarbageBin
!set bin71.maxFill     := 1.0
!set bin71.currentFill := 0.7
!create bin72       : GarbageBin
!set bin72.maxFill     := 1.0
!set bin72.currentFill := 0.74
!create bin73       : GarbageBin
!set bin73.maxFill     := 1.0
!set bin73.currentFill := 0.78
!create bin74       : GarbageBin
!set bin74.maxFill     := 1.0
!set bin74.currentFill := 0.82
!create bin75       : GarbageBin
!set bin75.maxFill     := 1.0
!set bin75.currentFill := 0.86
!create bin76       : GarbageBin
!set bin76.maxFill     := 1.0
!set bin76.currentFill := 0.1
!create bin77       : GarbageBin
!set bin77.maxFill     := 1.0
!set bin77.currentFill := 0.14
!create bin78       : GarbageBin
!set bin78.maxFill     := 1.0
!set bin78.currentFill := 0.18
!create bin79       : GarbageBin
!set bin79.maxFill     := 1.0
!set bin79.currentFill := 0.22
!create bin80       : GarbageBin
!set bin80.maxFill     := 1.0
!set bin80.currentFill := 0.26
!create bin81       : GarbageBin
!set bin81.maxFill     := 1.0
!set bin81.currentFill := 0.34
!create bin82       : GarbageBin
!set bin82.maxFill     := 1.0
!set bin82.currentFill := 0.38
!create bin83       : GarbageBin
!set bin83.maxFill     := 1.0
!set bin83.currentFill := 0.42
!create bin84       : GarbageBin
!set bin84.maxFill     := 1.0
!set bin84.currentFill := 0.46
!create bin85       : GarbageBin
!set bin85.maxFill     := 1.0
!set bin85.currentFill := 0.5
!create bin86       : GarbageBin
!set bin86.maxFill     := 1.0
!set bin86.currentFill := 0.54
!create bin87       : GarbageBin
!set bin87.maxFill     := 1.0
!set bin87.currentFill := 0.58
!create bin88       : GarbageBin
!set bin88.maxFill     := 1.0
!set bin88.currentFill := 0.62
!create bin89       : GarbageBin
!set bin89.maxFill     := 1.0
!set bin89.currentFill := 0.66
!create bin90       : GarbageBin
!set bin90.maxFill     := 1.0
!set bin90.currentFill := 0.7
!create bin91       : GarbageBin
!set bin91.maxFill     := 1.0
!set bin91.currentFill := 0.74
!create bin92       : GarbageBin
!set bin92.maxFill     := 1.0
!set bin92.currentFill := 0.78
!create bin93       : GarbageBin
!set bin93.maxFill     := 1.0
!set bin93.currentFill := 0.82
!create bin94       : GarbageBin
!set bin94.maxFill     := 1.0
!set bin94.currentFill := 0.86
!create bin95       : GarbageBin
!set bin95.maxFill     := 1.0
!set bin95.currentFill := 0.1
!create bin96       : GarbageBin
!set bin96.maxFill     := 1.0
!set bin96.currentFill := 0.14
!create bin97       : GarbageBin
!set bin97.maxFill     := 1.0
!set bin97.currentFill := 0.18
!create bin98       : GarbageBin
!set bin98.maxFill     := 1.0
!set bin98.currentFill := 0.22
!create bin99       : GarbageBin
!set bin99.maxFill     := 1.0
!set bin99.currentFill := 0.26
!create bin100       : GarbageBin
!set bin100.maxFill     := 1.0
!set bin100.currentFill := 0.3
!create bin101       : GarbageBin
!set bin101.maxFill     := 1.0
!set bin101.currentFill := 0.38
!create bin102       : GarbageBin
!set bin102.maxFill     := 1.0
!set bin102.currentFill := 0.42
!create bin103       : GarbageBin
!set bin103.maxFill     := 1.0
!set bin103.currentFill := 0.46
!create bin104       : GarbageBin
!set bin104.maxFill     := 1.0
!set bin104.currentFill := 0.5
!create bin105       : GarbageBin
!set bin105.maxFill     := 1.0
!set bin105.currentFill := 0.54
!create bin106       : GarbageBin
!set bin106.maxFill     := 1.0
!set bin106.currentFill := 0.58
!create bin107       : GarbageBin
!set bin107.maxFill     := 1.0
!set bin107.currentFill := 0.62
!create bin108       : GarbageBin
!set bin108.maxFill     := 1.0
!set bin108.currentFill := 0.66
!create bin109       : GarbageBin
!set bin109.maxFill     := 1.0
!set bin109.currentFill := 0.7
!create bin110       : GarbageBin
!set bin110.maxFill     := 1.0
!set bin110.currentFill := 0.74
!create bin111       : GarbageBin
!set bin111.maxFill     := 1.0
!set bin111.currentFill := 0.78
!create bin112       : GarbageBin
!set bin112.maxFill     := 1.0
!set bin112.currentFill := 0.82
!create bin113       : GarbageBin
!set bin113.maxFill     := 1.0
!set bin113.currentFill := 0.86
!create bin114       : GarbageBin
!set bin114.maxFill     := 1.0
!set bin114.currentFill := 0.1
!create bin115       : GarbageBin
!set bin115.maxFill     := 1.0
!set bin115.currentFill := 0.14
!create bin116       : GarbageBin
!set bin116.maxFill     := 1.0
!set bin116.currentFill := 0.18
!create bin117       : GarbageBin
!set bin117.maxFill     := 1.0
!set bin117.currentFill := 0.22
!create bin118       : GarbageBin
!set bin118.maxFill     := 1.0
!set bin118.currentFill := 0.26
!create bin119       : GarbageBin
!set bin119.maxFill     := 1.0
!set bin119.currentFill := 0.3
!create bin120       : GarbageBin
!set bin120.maxFill     := 1.0
!set bin120.currentFill := 0.34
!create bin121       : GarbageBin
!set bin121.maxFill     := 1.0
!set bin121.currentFill := 0.42
!create bin122       : GarbageBin
!set bin122.maxFill     := 1.0
!set bin122.currentFill := 0.46
!create bin123       : GarbageBin
!set bin123.maxFill     := 1.0
!set bin123.currentFill := 0.5
!create bin124       : GarbageBin
!set bin124.maxFill     := 1.0
!set bin124.currentFill := 0.54
!create bin125       : GarbageBin
!set bin125.maxFill     := 1.0
!set bin125.currentFill := 0.58
!create bin126       : GarbageBin
!set bin126.maxFill     := 1.0
!set bin126.currentFill := 0.62
!create bin127       : GarbageBin
!set bin127.maxFill     := 1.0
!set bin127.currentFill := 0.66
!create bin128       : GarbageBin
!set bin128.maxFill     := 1.0
!set bin128.currentFill := 0.7
!create bin129       : GarbageBin
!set bin129.maxFill     := 1.0
!set bin129.currentFill := 0.74
!create bin130       : GarbageBin
!set bin130.maxFill     := 1.0
!set bin130.currentFill := 0.78
!create bin131       : GarbageBin
!set bin131.maxFill     := 1.0
!set bin131.currentFill := 0.82
!create bin132       : GarbageBin
!set bin132.maxFill     := 1.0
!set bin132.currentFill := 0.86
!create bin133       : GarbageBin
!set bin133.maxFill     := 1.0
!set bin133.currentFill := 0.1
!create bin134       : GarbageBin
!set bin134.maxFill     := 1.0
!set bin134.currentFill := 0.14
!create bin135       : GarbageBin
!set bin135.maxFill     := 1.0
!set bin135.currentFill := 0.18
!create bin136       : GarbageBin
!set bin136.maxFill     := 1.0
!set bin136.currentFill := 0.22
!create bin137       : GarbageBin
!set bin137.maxFill     := 1.0
!set bin137.currentFill := 0.26
!create bin138       : GarbageBin
!set bin138.maxFill     := 1.0
!set bin138.currentFill := 0.3
!create bin139       : GarbageBin
!set bin139.maxFill     := 1.0
!set bin139.currentFill := 0.34
!create bin140       : GarbageBin
!set bin140.maxFill     := 1.0
!set bin140.currentFill := 0.38
!create bin141       : GarbageBin
!set bin141.maxFill     := 1.0
!set bin141.currentFill := 0.46
!create bin142       : GarbageBin
!set bin142.maxFill     := 1.0
!set bin142.currentFill := 0.5
!create bin143       : GarbageBin
!set bin143.maxFill     := 1.0
!set bin143.currentFill := 0.54
!create bin144       : GarbageBin
!set bin144.maxFill     := 1.0
!set bin144.currentFill := 0.58
!create bin145       : GarbageBin
!set bin145.maxFill     := 1.0
!set bin145.currentFill := 0.62
!create bin146       : GarbageBin
!set bin146.maxFill     := 1.0
!set bin146.currentFill := 0.66
!create bin147       : GarbageBin
!set bin147.maxFill     := 1.0
!set bin147.currentFill := 0.7
!create bin148       : GarbageBin
!set bin148.maxFill     := 1.0
!set bin148.currentFill := 0.74
!create bin149       : GarbageBin
!set bin149.maxFill     := 1.0
!set bin149.currentFill := 0.78
!create bin150       : GarbageBin
!set bin150.maxFill     := 1.0
!set bin150.currentFill := 0.82
!create bin151       : GarbageBin
!set bin151.maxFill     := 1.0
!set bin151.currentFill := 0.86
!create bin152       : GarbageBin
!set bin152.maxFill     := 1.0
!set bin152.currentFill := 0.1
!create bin153       : GarbageBin
!set bin153.maxFill     := 1.0
!set bin153.currentFill := 0.14
!create bin154       : GarbageBin
!set bin154.maxFill     := 1.0
!set bin154.currentFill := 0.18
!create bin155       : GarbageBin
!set bin155.maxFill     := 1.0
!set bin155.currentFill := 0.22
!create bin156       : GarbageBin
!set bin156.maxFill     := 1.0
!set bin156.currentFill := 0.26
!create bin157       : GarbageBin
!set bin157.maxFill     := 1.0
!set bin157.currentFill := 0.3
!create bin158       : GarbageBin
!set bin158.maxFill     := 1.0
!set bin158.currentFill := 0.34
!create bin159       : GarbageBin
!set bin159.maxFill     := 1.0
!set bin159.currentFill := 0.38
!create bin160       : GarbageBin
!set bin160.maxFill     := 1.0
!set bin160.currentFill := 0.42
!create bin161       : GarbageBin
!set bin161.maxFill     := 1.0
!set bin161.currentFill := 0.5
!create bin162       : GarbageBin
!set bin162.maxFill     := 1.0
!set bin162.currentFill := 0.54
!create bin163       : GarbageBin
!set bin163.maxFill     := 1.0
!set bin163.currentFill := 0.58
!create bin164       : GarbageBin
!set bin164.maxFill     := 1.0
!set bin164.currentFill := 0.62
!create bin165       : GarbageBin
!set bin165.maxFill     := 1.0
!set bin165.currentFill := 0.66
!create bin166       : GarbageBin
!set bin166.maxFill     := 1.0
!set bin166.currentFill := 0.7
!create bin167       : GarbageBin
!set bin167.maxFill     := 1.0
!set bin167.currentFill := 0.74
!create bin168       : GarbageBin
!set bin168.maxFill     := 1.0
!set bin168.currentFill := 0.78
!create bin169       : GarbageBin
!set bin169.maxFill     := 1.0
!set bin169.currentFill := 0.82
!create bin170       : GarbageBin
!set bin170.maxFill     := 1.0
!set bin170.currentFill := 0.86
!create bin171       : GarbageBin
!set bin171.maxFill     := 1.0
!set bin171.currentFill := 0.1
!create bin172       : GarbageBin
!set bin172.maxFill     := 1.0
!set bin172.currentFill := 0.14
!create bin173       : GarbageBin
!set bin173.maxFill     := 1.0
!set bin173.currentFill := 0.18
!create bin174       : GarbageBin
!set bin174.maxFill     := 1.0
!set bin174.currentFill := 0.22
!create bin175       : GarbageBin
!set bin175.maxFill     := 1.0
!set bin175.currentFill := 0.26
!create bin176       : GarbageBin
!set bin176.maxFill     := 1.0
!set bin176.currentFill := 0.3
!create bin177       : GarbageBin
!set bin177.maxFill     := 1.0
!set bin177.currentFill := 0.34
!create bin178       : GarbageBin
!set bin178.maxFill     := 1.0
!set bin178.currentFill := 0.38
!create bin179       : GarbageBin
!set bin179.maxFill     := 1.0
!set bin179.currentFill := 0.42
!create bin180       : GarbageBin
!set bin180.maxFill     := 1.0
!set bin180.currentFill := 0.46
!create bin181       : GarbageBin
!set bin181.maxFill     := 1.0
!set bin181.currentFill := 0.54
!create bin182       : GarbageBin
!set bin182.maxFill     := 1.0
!set bin182.currentFill := 0.58
!create bin183       : GarbageBin
!set bin183.maxFill     := 1.0
!set bin183.currentFill := 0.62
!create bin184       : GarbageBin
!set bin184.maxFill     := 1.0
!set bin184.currentFill := 0.66
!create bin185       : GarbageBin
!set bin185.maxFill     := 1.0
!set bin185.currentFill := 0.7
!create bin186       : GarbageBin
!set bin186.maxFill     := 1.0
!set bin186.currentFill := 0.74
!create bin187       : GarbageBin
!set bin187.maxFill     := 1.0
!set bin187.currentFill := 0.78
!create bin188       : GarbageBin
!set bin188.maxFill     := 1.0
!set bin188.currentFill := 0.82
!create bin189       : GarbageBin
!set bin189.maxFill     := 1.0
!set bin189.currentFill := 0.86
!create bin190       : GarbageBin
!set bin190.maxFill     := 1.0
!set bin190.currentFill := 0.1
!create bin191       : GarbageBin
!set bin191.maxFill     := 1.0
!set bin191.currentFill := 0.14
!create bin192       : GarbageBin
!set bin192.maxFill     := 1.0
!set bin192.currentFill := 0.18
!create bin193       : GarbageBin
!set bin193.maxFill     := 1.0
!set bin193.currentFill := 0.22
!create bin194       : GarbageBin
!set bin194.maxFill     := 1.0
!set bin194.currentFill := 0.26
!create bin195       : GarbageBin
!set bin195.maxFill     := 1.0
!set bin195.currentFill := 0.3
!create bin196       : GarbageBin
!set bin196.maxFill     := 1.0
!set bin196.currentFill := 0.34
!create bin197       : GarbageBin
!set bin197.maxFill     := 1.0
!set bin197.currentFill := 0.38
!create bin198       : GarbageBin
!set bin198.maxFill     := 1.0
!set bin198.currentFill := 0.42
!create bin199       : GarbageBin
!set bin199.maxFill     := 1.0
!set bin199.currentFill := 0.46
!create bin200       : GarbageBin
!set bin200.maxFill     := 1.0
!set bin200.currentFill := 0.5

-- Place bins at their lane intersection
!insert (bin1, n2) into LocatedAt
!insert (bin2, n3) into LocatedAt
!insert (bin3, n4) into LocatedAt
!insert (bin4, n5) into LocatedAt
!insert (bin5, n6) into LocatedAt
!insert (bin6, n7) into LocatedAt
!insert (bin7, n8) into LocatedAt
!insert (bin8, n9) into LocatedAt
!insert (bin9, n10) into LocatedAt
!insert (bin10, n11) into LocatedAt
!insert (bin11, n12) into LocatedAt
!insert (bin12, n13) into LocatedAt
!insert (bin13, n14) into LocatedAt
!insert (bin14, n15) into LocatedAt
!insert (bin15, n16) into LocatedAt
!insert (bin16, n17) into LocatedAt
!insert (bin17, n18) into LocatedAt
!insert (bin18, n19) into LocatedAt
!insert (bin19, n20) into LocatedAt
!insert (bin20, n21) into LocatedAt
!insert (bin21, n22) into LocatedAt
!insert (bin22, n23) into LocatedAt
!insert (bin23, n24) into LocatedAt
!insert (bin24, n25) into LocatedAt
!insert (bin25, n26) into LocatedAt
!insert (bin26, n27) into LocatedAt
!insert (bin27, n28) into LocatedAt
!insert (bin28, n29) into LocatedAt
!insert (bin29, n30) into LocatedAt
!insert (bin30, n31) into LocatedAt
!insert (bin31, n32) into LocatedAt
!insert (bin32, n33) into LocatedAt
!insert (bin33, n34) into LocatedAt
!insert (bin34, n35) into LocatedAt
!insert (bin35, n36) into LocatedAt
!insert (bin36, n37) into LocatedAt
!insert (bin37, n38) into LocatedAt
!insert (bin38, n39) into LocatedAt
!insert (bin39, n40) into LocatedAt
!insert (bin40, n41) into LocatedAt
!insert (bin41, n42) into LocatedAt
!insert (bin42, n43) into LocatedAt
!insert (bin43, n44) into LocatedAt
!insert (bin44, n45) into LocatedAt
!insert (bin45, n46) into LocatedAt
!insert (bin46, n47) into LocatedAt
!insert (bin47, n48) into LocatedAt
!insert (bin48, n49) into LocatedAt
!insert (bin49, n50) into LocatedAt
!insert (bin50, n51) into LocatedAt
!insert (bin51, n52) into LocatedAt
!insert (bin52, n53) into LocatedAt
!insert (bin53, n54) into LocatedAt
!insert (bin54, n55) into LocatedAt
!insert (bin55, n56) into LocatedAt
!insert (bin56, n57) into LocatedAt
!insert (bin57, n58) into LocatedAt
!insert (bin58, n59) into LocatedAt
!insert (bin59, n60) into LocatedAt
!insert (bin60, n61) into LocatedAt
!insert (bin61, n62) into LocatedAt
!insert (bin62, n63) into LocatedAt
!insert (bin63, n64) into LocatedAt
!insert (bin64, n65) into LocatedAt
!insert (bin65, n66) into LocatedAt
!insert (bin66, n67) into LocatedAt
!insert (bin67, n68) into LocatedAt
!insert (bin68, n69) into LocatedAt
!insert (bin69, n70) into LocatedAt
!insert (bin70, n71) into LocatedAt
!insert (bin71, n72) into LocatedAt
!insert (bin72, n73) into LocatedAt
!insert (bin73, n74) into LocatedAt
!insert (bin74, n75) into LocatedAt
!insert (bin75, n76) into LocatedAt
!insert (bin76, n77) into LocatedAt
!insert (bin77, n78) into LocatedAt
!insert (bin78, n79) into LocatedAt
!insert (bin79, n80) into LocatedAt
!insert (bin80, n81) into LocatedAt
!insert (bin81, n82) into LocatedAt
!insert (bin82, n83) into LocatedAt
!insert (bin83, n84) into LocatedAt
!insert (bin84, n85) into LocatedAt
!insert (bin85, n86) into LocatedAt
!insert (bin86, n87) into LocatedAt
!insert (bin87, n88) into LocatedAt
!insert (bin88, n89) into LocatedAt
!insert (bin89, n90) into LocatedAt
!insert (bin90, n91) into LocatedAt
!insert (bin91, n92) into LocatedAt
!insert (bin92, n93) into LocatedAt
!insert (bin93, n94) into LocatedAt
!insert (bin94, n95) into LocatedAt
!insert (bin95, n96) into LocatedAt
!insert (bin96, n97) into LocatedAt
!insert (bin97, n98) into LocatedAt
!insert (bin98, n99) into LocatedAt
!insert (bin99, n100) into LocatedAt
!insert (bin100, n101) into LocatedAt
!insert (bin101, n102) into LocatedAt
!insert (bin102, n103) into LocatedAt
!insert (bin103, n104) into LocatedAt
!insert (bin104, n105) into LocatedAt
!insert (bin105, n106) into LocatedAt
!insert (bin106, n107) into LocatedAt
!insert (bin107, n108) into LocatedAt
!insert (bin108, n109) into LocatedAt
!insert (bin109, n110) into LocatedAt
!insert (bin110, n111) into LocatedAt
!insert (bin111, n112) into LocatedAt
!insert (bin112, n113) into LocatedAt
!insert (bin113, n114) into LocatedAt
!insert (bin114, n115) into LocatedAt
!insert (bin115, n116) into LocatedAt
!insert (bin116, n117) into LocatedAt
!insert (bin117, n118) into LocatedAt
!insert (bin118, n119) into LocatedAt
!insert (bin119, n120) into LocatedAt
!insert (bin120, n121) into LocatedAt
!insert (bin121, n122) into LocatedAt
!insert (bin122, n123) into LocatedAt
!insert (bin123, n124) into LocatedAt
!insert (bin124, n125) into LocatedAt
!insert (bin125, n126) into LocatedAt
!insert (bin126, n127) into LocatedAt
!insert (bin127, n128) into LocatedAt
!insert (bin128, n129) into LocatedAt
!insert (bin129, n130) into LocatedAt
!insert (bin130, n131) into LocatedAt
!insert (bin131, n132) into LocatedAt
!insert (bin132, n133) into LocatedAt
!insert (bin133, n134) into LocatedAt
!insert (bin134, n135) into LocatedAt
!insert (bin135, n136) into LocatedAt
!insert (bin136, n137) into LocatedAt
!insert (bin137, n138) into LocatedAt
!insert (bin138, n139) into LocatedAt
!insert (bin139, n140) into LocatedAt
!insert (bin140, n141) into LocatedAt
!insert (bin141, n142) into LocatedAt
!insert (bin142, n143) into LocatedAt
!insert (bin143, n144) into LocatedAt
!insert (bin144, n145) into LocatedAt
!insert (bin145, n146) into LocatedAt
!insert (bin146, n147) into LocatedAt
!insert (bin147, n148) into LocatedAt
!insert (bin148, n149) into LocatedAt
!insert (bin149, n150) into LocatedAt
!insert (bin150, n151) into LocatedAt
!insert (bin151, n152) into LocatedAt
!insert (bin152, n153) into LocatedAt
!insert (bin153, n154) into LocatedAt
!insert (bin154, n155) into LocatedAt
!insert (bin155, n156) into LocatedAt
!insert (bin156, n157) into LocatedAt
!insert (bin157, n158) into LocatedAt
!insert (bin158, n159) into LocatedAt
!insert (bin159, n160) into LocatedAt
!insert (bin160, n161) into LocatedAt
!insert (bin161, n162) into LocatedAt
!insert (bin162, n163) into LocatedAt
!insert (bin163, n164) into LocatedAt
!insert (bin164, n165) into LocatedAt
!insert (bin165, n166) into LocatedAt
!insert (bin166, n167) into LocatedAt
!insert (bin167, n168) into LocatedAt
!insert (bin168, n169) into LocatedAt
!insert (bin169, n170) into LocatedAt
!insert (bin170, n171) into LocatedAt
!insert (bin171, n172) into LocatedAt
!insert (bin172, n173) into LocatedAt
!insert (bin173, n174) into LocatedAt
!insert (bin174, n175) into LocatedAt
!insert (bin175, n176) into LocatedAt
!insert (bin176, n177) into LocatedAt
!insert (bin177, n178) into LocatedAt
!insert (bin178, n179) into LocatedAt
!insert (bin179, n180) into LocatedAt
!insert (bin180, n181) into LocatedAt
!insert (bin181, n182) into LocatedAt
!insert (bin182, n183) into LocatedAt
!insert (bin183, n184) into LocatedAt
!insert (bin184, n185) into LocatedAt
!insert (bin185, n186) into LocatedAt
!insert (bin186, n187) into LocatedAt
!insert (bin187, n188) into LocatedAt
!insert (bin188, n189) into LocatedAt
!insert (bin189, n190) into LocatedAt
!insert (bin190, n191) into LocatedAt
!insert (bin191, n192) into LocatedAt
!insert (bin192, n193) into LocatedAt
!insert (bin193, n194) into LocatedAt
!insert (bin194, n195) into LocatedAt
!insert (bin195, n196) into LocatedAt
!insert (bin196, n197) into LocatedAt
!insert (bin197, n198) into LocatedAt
!insert (bin198, n199) into LocatedAt
!insert (bin199, n200) into LocatedAt
!insert (bin200, n201) into LocatedAt

-- -----------------------------------------------------------
-- 4. Create trucks (10, one per lane/route)
-- -----------------------------------------------------------
!create truck1 : Truck
!set truck1.truckId     := 1
!set truck1.fuelRange   := 50.0  -- edgeCost() per route is 21.0
!set truck1.maxCapacity := 20.0  -- >= any lane's 20-bin fill sum
!set truck1.currentLoad := 0.0

!create truck2 : Truck
!set truck2.truckId     := 2
!set truck2.fuelRange   := 50.0  -- edgeCost() per route is 21.0
!set truck2.maxCapacity := 20.0  -- >= any lane's 20-bin fill sum
!set truck2.currentLoad := 0.0

!create truck3 : Truck
!set truck3.truckId     := 3
!set truck3.fuelRange   := 50.0  -- edgeCost() per route is 21.0
!set truck3.maxCapacity := 20.0  -- >= any lane's 20-bin fill sum
!set truck3.currentLoad := 0.0

!create truck4 : Truck
!set truck4.truckId     := 4
!set truck4.fuelRange   := 50.0  -- edgeCost() per route is 21.0
!set truck4.maxCapacity := 20.0  -- >= any lane's 20-bin fill sum
!set truck4.currentLoad := 0.0

!create truck5 : Truck
!set truck5.truckId     := 5
!set truck5.fuelRange   := 50.0  -- edgeCost() per route is 21.0
!set truck5.maxCapacity := 20.0  -- >= any lane's 20-bin fill sum
!set truck5.currentLoad := 0.0

!create truck6 : Truck
!set truck6.truckId     := 6
!set truck6.fuelRange   := 50.0  -- edgeCost() per route is 21.0
!set truck6.maxCapacity := 20.0  -- >= any lane's 20-bin fill sum
!set truck6.currentLoad := 0.0

!create truck7 : Truck
!set truck7.truckId     := 7
!set truck7.fuelRange   := 50.0  -- edgeCost() per route is 21.0
!set truck7.maxCapacity := 20.0  -- >= any lane's 20-bin fill sum
!set truck7.currentLoad := 0.0

!create truck8 : Truck
!set truck8.truckId     := 8
!set truck8.fuelRange   := 50.0  -- edgeCost() per route is 21.0
!set truck8.maxCapacity := 20.0  -- >= any lane's 20-bin fill sum
!set truck8.currentLoad := 0.0

!create truck9 : Truck
!set truck9.truckId     := 9
!set truck9.fuelRange   := 50.0  -- edgeCost() per route is 21.0
!set truck9.maxCapacity := 20.0  -- >= any lane's 20-bin fill sum
!set truck9.currentLoad := 0.0

!create truck10 : Truck
!set truck10.truckId     := 10
!set truck10.fuelRange   := 50.0  -- edgeCost() per route is 21.0
!set truck10.maxCapacity := 20.0  -- >= any lane's 20-bin fill sum
!set truck10.currentLoad := 0.0

-- -----------------------------------------------------------
-- 5. Create the 10 routes
-- -----------------------------------------------------------
!create route1 : Route
!set route1.totalTravelTime := 21.0   -- 21 x 1.0 spine edges

-- Assign route1 to truck1
!insert (route1, truck1) into AssignedTo

-- Route1: select the spine edges it uses (RouteRoad is the decision variable)
!insert (route1, Road.allInstances->any(r | r.origin = depot and r.destination = n2)) into RouteRoad
!insert (route1, Road.allInstances->any(r | r.origin = n2 and r.destination = n3)) into RouteRoad
!insert (route1, Road.allInstances->any(r | r.origin = n3 and r.destination = n4)) into RouteRoad
!insert (route1, Road.allInstances->any(r | r.origin = n4 and r.destination = n5)) into RouteRoad
!insert (route1, Road.allInstances->any(r | r.origin = n5 and r.destination = n6)) into RouteRoad
!insert (route1, Road.allInstances->any(r | r.origin = n6 and r.destination = n7)) into RouteRoad
!insert (route1, Road.allInstances->any(r | r.origin = n7 and r.destination = n8)) into RouteRoad
!insert (route1, Road.allInstances->any(r | r.origin = n8 and r.destination = n9)) into RouteRoad
!insert (route1, Road.allInstances->any(r | r.origin = n9 and r.destination = n10)) into RouteRoad
!insert (route1, Road.allInstances->any(r | r.origin = n10 and r.destination = n11)) into RouteRoad
!insert (route1, Road.allInstances->any(r | r.origin = n11 and r.destination = n12)) into RouteRoad
!insert (route1, Road.allInstances->any(r | r.origin = n12 and r.destination = n13)) into RouteRoad
!insert (route1, Road.allInstances->any(r | r.origin = n13 and r.destination = n14)) into RouteRoad
!insert (route1, Road.allInstances->any(r | r.origin = n14 and r.destination = n15)) into RouteRoad
!insert (route1, Road.allInstances->any(r | r.origin = n15 and r.destination = n16)) into RouteRoad
!insert (route1, Road.allInstances->any(r | r.origin = n16 and r.destination = n17)) into RouteRoad
!insert (route1, Road.allInstances->any(r | r.origin = n17 and r.destination = n18)) into RouteRoad
!insert (route1, Road.allInstances->any(r | r.origin = n18 and r.destination = n19)) into RouteRoad
!insert (route1, Road.allInstances->any(r | r.origin = n19 and r.destination = n20)) into RouteRoad
!insert (route1, Road.allInstances->any(r | r.origin = n20 and r.destination = n21)) into RouteRoad
!insert (route1, Road.allInstances->any(r | r.origin = n21 and r.destination = disposal)) into RouteRoad

!create route2 : Route
!set route2.totalTravelTime := 21.0   -- 21 x 1.0 spine edges

-- Assign route2 to truck2
!insert (route2, truck2) into AssignedTo

-- Route2: select the spine edges it uses (RouteRoad is the decision variable)
!insert (route2, Road.allInstances->any(r | r.origin = depot and r.destination = n22)) into RouteRoad
!insert (route2, Road.allInstances->any(r | r.origin = n22 and r.destination = n23)) into RouteRoad
!insert (route2, Road.allInstances->any(r | r.origin = n23 and r.destination = n24)) into RouteRoad
!insert (route2, Road.allInstances->any(r | r.origin = n24 and r.destination = n25)) into RouteRoad
!insert (route2, Road.allInstances->any(r | r.origin = n25 and r.destination = n26)) into RouteRoad
!insert (route2, Road.allInstances->any(r | r.origin = n26 and r.destination = n27)) into RouteRoad
!insert (route2, Road.allInstances->any(r | r.origin = n27 and r.destination = n28)) into RouteRoad
!insert (route2, Road.allInstances->any(r | r.origin = n28 and r.destination = n29)) into RouteRoad
!insert (route2, Road.allInstances->any(r | r.origin = n29 and r.destination = n30)) into RouteRoad
!insert (route2, Road.allInstances->any(r | r.origin = n30 and r.destination = n31)) into RouteRoad
!insert (route2, Road.allInstances->any(r | r.origin = n31 and r.destination = n32)) into RouteRoad
!insert (route2, Road.allInstances->any(r | r.origin = n32 and r.destination = n33)) into RouteRoad
!insert (route2, Road.allInstances->any(r | r.origin = n33 and r.destination = n34)) into RouteRoad
!insert (route2, Road.allInstances->any(r | r.origin = n34 and r.destination = n35)) into RouteRoad
!insert (route2, Road.allInstances->any(r | r.origin = n35 and r.destination = n36)) into RouteRoad
!insert (route2, Road.allInstances->any(r | r.origin = n36 and r.destination = n37)) into RouteRoad
!insert (route2, Road.allInstances->any(r | r.origin = n37 and r.destination = n38)) into RouteRoad
!insert (route2, Road.allInstances->any(r | r.origin = n38 and r.destination = n39)) into RouteRoad
!insert (route2, Road.allInstances->any(r | r.origin = n39 and r.destination = n40)) into RouteRoad
!insert (route2, Road.allInstances->any(r | r.origin = n40 and r.destination = n41)) into RouteRoad
!insert (route2, Road.allInstances->any(r | r.origin = n41 and r.destination = disposal)) into RouteRoad

!create route3 : Route
!set route3.totalTravelTime := 21.0   -- 21 x 1.0 spine edges

-- Assign route3 to truck3
!insert (route3, truck3) into AssignedTo

-- Route3: select the spine edges it uses (RouteRoad is the decision variable)
!insert (route3, Road.allInstances->any(r | r.origin = depot and r.destination = n42)) into RouteRoad
!insert (route3, Road.allInstances->any(r | r.origin = n42 and r.destination = n43)) into RouteRoad
!insert (route3, Road.allInstances->any(r | r.origin = n43 and r.destination = n44)) into RouteRoad
!insert (route3, Road.allInstances->any(r | r.origin = n44 and r.destination = n45)) into RouteRoad
!insert (route3, Road.allInstances->any(r | r.origin = n45 and r.destination = n46)) into RouteRoad
!insert (route3, Road.allInstances->any(r | r.origin = n46 and r.destination = n47)) into RouteRoad
!insert (route3, Road.allInstances->any(r | r.origin = n47 and r.destination = n48)) into RouteRoad
!insert (route3, Road.allInstances->any(r | r.origin = n48 and r.destination = n49)) into RouteRoad
!insert (route3, Road.allInstances->any(r | r.origin = n49 and r.destination = n50)) into RouteRoad
!insert (route3, Road.allInstances->any(r | r.origin = n50 and r.destination = n51)) into RouteRoad
!insert (route3, Road.allInstances->any(r | r.origin = n51 and r.destination = n52)) into RouteRoad
!insert (route3, Road.allInstances->any(r | r.origin = n52 and r.destination = n53)) into RouteRoad
!insert (route3, Road.allInstances->any(r | r.origin = n53 and r.destination = n54)) into RouteRoad
!insert (route3, Road.allInstances->any(r | r.origin = n54 and r.destination = n55)) into RouteRoad
!insert (route3, Road.allInstances->any(r | r.origin = n55 and r.destination = n56)) into RouteRoad
!insert (route3, Road.allInstances->any(r | r.origin = n56 and r.destination = n57)) into RouteRoad
!insert (route3, Road.allInstances->any(r | r.origin = n57 and r.destination = n58)) into RouteRoad
!insert (route3, Road.allInstances->any(r | r.origin = n58 and r.destination = n59)) into RouteRoad
!insert (route3, Road.allInstances->any(r | r.origin = n59 and r.destination = n60)) into RouteRoad
!insert (route3, Road.allInstances->any(r | r.origin = n60 and r.destination = n61)) into RouteRoad
!insert (route3, Road.allInstances->any(r | r.origin = n61 and r.destination = disposal)) into RouteRoad

!create route4 : Route
!set route4.totalTravelTime := 21.0   -- 21 x 1.0 spine edges

-- Assign route4 to truck4
!insert (route4, truck4) into AssignedTo

-- Route4: select the spine edges it uses (RouteRoad is the decision variable)
!insert (route4, Road.allInstances->any(r | r.origin = depot and r.destination = n62)) into RouteRoad
!insert (route4, Road.allInstances->any(r | r.origin = n62 and r.destination = n63)) into RouteRoad
!insert (route4, Road.allInstances->any(r | r.origin = n63 and r.destination = n64)) into RouteRoad
!insert (route4, Road.allInstances->any(r | r.origin = n64 and r.destination = n65)) into RouteRoad
!insert (route4, Road.allInstances->any(r | r.origin = n65 and r.destination = n66)) into RouteRoad
!insert (route4, Road.allInstances->any(r | r.origin = n66 and r.destination = n67)) into RouteRoad
!insert (route4, Road.allInstances->any(r | r.origin = n67 and r.destination = n68)) into RouteRoad
!insert (route4, Road.allInstances->any(r | r.origin = n68 and r.destination = n69)) into RouteRoad
!insert (route4, Road.allInstances->any(r | r.origin = n69 and r.destination = n70)) into RouteRoad
!insert (route4, Road.allInstances->any(r | r.origin = n70 and r.destination = n71)) into RouteRoad
!insert (route4, Road.allInstances->any(r | r.origin = n71 and r.destination = n72)) into RouteRoad
!insert (route4, Road.allInstances->any(r | r.origin = n72 and r.destination = n73)) into RouteRoad
!insert (route4, Road.allInstances->any(r | r.origin = n73 and r.destination = n74)) into RouteRoad
!insert (route4, Road.allInstances->any(r | r.origin = n74 and r.destination = n75)) into RouteRoad
!insert (route4, Road.allInstances->any(r | r.origin = n75 and r.destination = n76)) into RouteRoad
!insert (route4, Road.allInstances->any(r | r.origin = n76 and r.destination = n77)) into RouteRoad
!insert (route4, Road.allInstances->any(r | r.origin = n77 and r.destination = n78)) into RouteRoad
!insert (route4, Road.allInstances->any(r | r.origin = n78 and r.destination = n79)) into RouteRoad
!insert (route4, Road.allInstances->any(r | r.origin = n79 and r.destination = n80)) into RouteRoad
!insert (route4, Road.allInstances->any(r | r.origin = n80 and r.destination = n81)) into RouteRoad
!insert (route4, Road.allInstances->any(r | r.origin = n81 and r.destination = disposal)) into RouteRoad

!create route5 : Route
!set route5.totalTravelTime := 21.0   -- 21 x 1.0 spine edges

-- Assign route5 to truck5
!insert (route5, truck5) into AssignedTo

-- Route5: select the spine edges it uses (RouteRoad is the decision variable)
!insert (route5, Road.allInstances->any(r | r.origin = depot and r.destination = n82)) into RouteRoad
!insert (route5, Road.allInstances->any(r | r.origin = n82 and r.destination = n83)) into RouteRoad
!insert (route5, Road.allInstances->any(r | r.origin = n83 and r.destination = n84)) into RouteRoad
!insert (route5, Road.allInstances->any(r | r.origin = n84 and r.destination = n85)) into RouteRoad
!insert (route5, Road.allInstances->any(r | r.origin = n85 and r.destination = n86)) into RouteRoad
!insert (route5, Road.allInstances->any(r | r.origin = n86 and r.destination = n87)) into RouteRoad
!insert (route5, Road.allInstances->any(r | r.origin = n87 and r.destination = n88)) into RouteRoad
!insert (route5, Road.allInstances->any(r | r.origin = n88 and r.destination = n89)) into RouteRoad
!insert (route5, Road.allInstances->any(r | r.origin = n89 and r.destination = n90)) into RouteRoad
!insert (route5, Road.allInstances->any(r | r.origin = n90 and r.destination = n91)) into RouteRoad
!insert (route5, Road.allInstances->any(r | r.origin = n91 and r.destination = n92)) into RouteRoad
!insert (route5, Road.allInstances->any(r | r.origin = n92 and r.destination = n93)) into RouteRoad
!insert (route5, Road.allInstances->any(r | r.origin = n93 and r.destination = n94)) into RouteRoad
!insert (route5, Road.allInstances->any(r | r.origin = n94 and r.destination = n95)) into RouteRoad
!insert (route5, Road.allInstances->any(r | r.origin = n95 and r.destination = n96)) into RouteRoad
!insert (route5, Road.allInstances->any(r | r.origin = n96 and r.destination = n97)) into RouteRoad
!insert (route5, Road.allInstances->any(r | r.origin = n97 and r.destination = n98)) into RouteRoad
!insert (route5, Road.allInstances->any(r | r.origin = n98 and r.destination = n99)) into RouteRoad
!insert (route5, Road.allInstances->any(r | r.origin = n99 and r.destination = n100)) into RouteRoad
!insert (route5, Road.allInstances->any(r | r.origin = n100 and r.destination = n101)) into RouteRoad
!insert (route5, Road.allInstances->any(r | r.origin = n101 and r.destination = disposal)) into RouteRoad

!create route6 : Route
!set route6.totalTravelTime := 21.0   -- 21 x 1.0 spine edges

-- Assign route6 to truck6
!insert (route6, truck6) into AssignedTo

-- Route6: select the spine edges it uses (RouteRoad is the decision variable)
!insert (route6, Road.allInstances->any(r | r.origin = depot and r.destination = n102)) into RouteRoad
!insert (route6, Road.allInstances->any(r | r.origin = n102 and r.destination = n103)) into RouteRoad
!insert (route6, Road.allInstances->any(r | r.origin = n103 and r.destination = n104)) into RouteRoad
!insert (route6, Road.allInstances->any(r | r.origin = n104 and r.destination = n105)) into RouteRoad
!insert (route6, Road.allInstances->any(r | r.origin = n105 and r.destination = n106)) into RouteRoad
!insert (route6, Road.allInstances->any(r | r.origin = n106 and r.destination = n107)) into RouteRoad
!insert (route6, Road.allInstances->any(r | r.origin = n107 and r.destination = n108)) into RouteRoad
!insert (route6, Road.allInstances->any(r | r.origin = n108 and r.destination = n109)) into RouteRoad
!insert (route6, Road.allInstances->any(r | r.origin = n109 and r.destination = n110)) into RouteRoad
!insert (route6, Road.allInstances->any(r | r.origin = n110 and r.destination = n111)) into RouteRoad
!insert (route6, Road.allInstances->any(r | r.origin = n111 and r.destination = n112)) into RouteRoad
!insert (route6, Road.allInstances->any(r | r.origin = n112 and r.destination = n113)) into RouteRoad
!insert (route6, Road.allInstances->any(r | r.origin = n113 and r.destination = n114)) into RouteRoad
!insert (route6, Road.allInstances->any(r | r.origin = n114 and r.destination = n115)) into RouteRoad
!insert (route6, Road.allInstances->any(r | r.origin = n115 and r.destination = n116)) into RouteRoad
!insert (route6, Road.allInstances->any(r | r.origin = n116 and r.destination = n117)) into RouteRoad
!insert (route6, Road.allInstances->any(r | r.origin = n117 and r.destination = n118)) into RouteRoad
!insert (route6, Road.allInstances->any(r | r.origin = n118 and r.destination = n119)) into RouteRoad
!insert (route6, Road.allInstances->any(r | r.origin = n119 and r.destination = n120)) into RouteRoad
!insert (route6, Road.allInstances->any(r | r.origin = n120 and r.destination = n121)) into RouteRoad
!insert (route6, Road.allInstances->any(r | r.origin = n121 and r.destination = disposal)) into RouteRoad

!create route7 : Route
!set route7.totalTravelTime := 21.0   -- 21 x 1.0 spine edges

-- Assign route7 to truck7
!insert (route7, truck7) into AssignedTo

-- Route7: select the spine edges it uses (RouteRoad is the decision variable)
!insert (route7, Road.allInstances->any(r | r.origin = depot and r.destination = n122)) into RouteRoad
!insert (route7, Road.allInstances->any(r | r.origin = n122 and r.destination = n123)) into RouteRoad
!insert (route7, Road.allInstances->any(r | r.origin = n123 and r.destination = n124)) into RouteRoad
!insert (route7, Road.allInstances->any(r | r.origin = n124 and r.destination = n125)) into RouteRoad
!insert (route7, Road.allInstances->any(r | r.origin = n125 and r.destination = n126)) into RouteRoad
!insert (route7, Road.allInstances->any(r | r.origin = n126 and r.destination = n127)) into RouteRoad
!insert (route7, Road.allInstances->any(r | r.origin = n127 and r.destination = n128)) into RouteRoad
!insert (route7, Road.allInstances->any(r | r.origin = n128 and r.destination = n129)) into RouteRoad
!insert (route7, Road.allInstances->any(r | r.origin = n129 and r.destination = n130)) into RouteRoad
!insert (route7, Road.allInstances->any(r | r.origin = n130 and r.destination = n131)) into RouteRoad
!insert (route7, Road.allInstances->any(r | r.origin = n131 and r.destination = n132)) into RouteRoad
!insert (route7, Road.allInstances->any(r | r.origin = n132 and r.destination = n133)) into RouteRoad
!insert (route7, Road.allInstances->any(r | r.origin = n133 and r.destination = n134)) into RouteRoad
!insert (route7, Road.allInstances->any(r | r.origin = n134 and r.destination = n135)) into RouteRoad
!insert (route7, Road.allInstances->any(r | r.origin = n135 and r.destination = n136)) into RouteRoad
!insert (route7, Road.allInstances->any(r | r.origin = n136 and r.destination = n137)) into RouteRoad
!insert (route7, Road.allInstances->any(r | r.origin = n137 and r.destination = n138)) into RouteRoad
!insert (route7, Road.allInstances->any(r | r.origin = n138 and r.destination = n139)) into RouteRoad
!insert (route7, Road.allInstances->any(r | r.origin = n139 and r.destination = n140)) into RouteRoad
!insert (route7, Road.allInstances->any(r | r.origin = n140 and r.destination = n141)) into RouteRoad
!insert (route7, Road.allInstances->any(r | r.origin = n141 and r.destination = disposal)) into RouteRoad

!create route8 : Route
!set route8.totalTravelTime := 21.0   -- 21 x 1.0 spine edges

-- Assign route8 to truck8
!insert (route8, truck8) into AssignedTo

-- Route8: select the spine edges it uses (RouteRoad is the decision variable)
!insert (route8, Road.allInstances->any(r | r.origin = depot and r.destination = n142)) into RouteRoad
!insert (route8, Road.allInstances->any(r | r.origin = n142 and r.destination = n143)) into RouteRoad
!insert (route8, Road.allInstances->any(r | r.origin = n143 and r.destination = n144)) into RouteRoad
!insert (route8, Road.allInstances->any(r | r.origin = n144 and r.destination = n145)) into RouteRoad
!insert (route8, Road.allInstances->any(r | r.origin = n145 and r.destination = n146)) into RouteRoad
!insert (route8, Road.allInstances->any(r | r.origin = n146 and r.destination = n147)) into RouteRoad
!insert (route8, Road.allInstances->any(r | r.origin = n147 and r.destination = n148)) into RouteRoad
!insert (route8, Road.allInstances->any(r | r.origin = n148 and r.destination = n149)) into RouteRoad
!insert (route8, Road.allInstances->any(r | r.origin = n149 and r.destination = n150)) into RouteRoad
!insert (route8, Road.allInstances->any(r | r.origin = n150 and r.destination = n151)) into RouteRoad
!insert (route8, Road.allInstances->any(r | r.origin = n151 and r.destination = n152)) into RouteRoad
!insert (route8, Road.allInstances->any(r | r.origin = n152 and r.destination = n153)) into RouteRoad
!insert (route8, Road.allInstances->any(r | r.origin = n153 and r.destination = n154)) into RouteRoad
!insert (route8, Road.allInstances->any(r | r.origin = n154 and r.destination = n155)) into RouteRoad
!insert (route8, Road.allInstances->any(r | r.origin = n155 and r.destination = n156)) into RouteRoad
!insert (route8, Road.allInstances->any(r | r.origin = n156 and r.destination = n157)) into RouteRoad
!insert (route8, Road.allInstances->any(r | r.origin = n157 and r.destination = n158)) into RouteRoad
!insert (route8, Road.allInstances->any(r | r.origin = n158 and r.destination = n159)) into RouteRoad
!insert (route8, Road.allInstances->any(r | r.origin = n159 and r.destination = n160)) into RouteRoad
!insert (route8, Road.allInstances->any(r | r.origin = n160 and r.destination = n161)) into RouteRoad
!insert (route8, Road.allInstances->any(r | r.origin = n161 and r.destination = disposal)) into RouteRoad

!create route9 : Route
!set route9.totalTravelTime := 21.0   -- 21 x 1.0 spine edges

-- Assign route9 to truck9
!insert (route9, truck9) into AssignedTo

-- Route9: select the spine edges it uses (RouteRoad is the decision variable)
!insert (route9, Road.allInstances->any(r | r.origin = depot and r.destination = n162)) into RouteRoad
!insert (route9, Road.allInstances->any(r | r.origin = n162 and r.destination = n163)) into RouteRoad
!insert (route9, Road.allInstances->any(r | r.origin = n163 and r.destination = n164)) into RouteRoad
!insert (route9, Road.allInstances->any(r | r.origin = n164 and r.destination = n165)) into RouteRoad
!insert (route9, Road.allInstances->any(r | r.origin = n165 and r.destination = n166)) into RouteRoad
!insert (route9, Road.allInstances->any(r | r.origin = n166 and r.destination = n167)) into RouteRoad
!insert (route9, Road.allInstances->any(r | r.origin = n167 and r.destination = n168)) into RouteRoad
!insert (route9, Road.allInstances->any(r | r.origin = n168 and r.destination = n169)) into RouteRoad
!insert (route9, Road.allInstances->any(r | r.origin = n169 and r.destination = n170)) into RouteRoad
!insert (route9, Road.allInstances->any(r | r.origin = n170 and r.destination = n171)) into RouteRoad
!insert (route9, Road.allInstances->any(r | r.origin = n171 and r.destination = n172)) into RouteRoad
!insert (route9, Road.allInstances->any(r | r.origin = n172 and r.destination = n173)) into RouteRoad
!insert (route9, Road.allInstances->any(r | r.origin = n173 and r.destination = n174)) into RouteRoad
!insert (route9, Road.allInstances->any(r | r.origin = n174 and r.destination = n175)) into RouteRoad
!insert (route9, Road.allInstances->any(r | r.origin = n175 and r.destination = n176)) into RouteRoad
!insert (route9, Road.allInstances->any(r | r.origin = n176 and r.destination = n177)) into RouteRoad
!insert (route9, Road.allInstances->any(r | r.origin = n177 and r.destination = n178)) into RouteRoad
!insert (route9, Road.allInstances->any(r | r.origin = n178 and r.destination = n179)) into RouteRoad
!insert (route9, Road.allInstances->any(r | r.origin = n179 and r.destination = n180)) into RouteRoad
!insert (route9, Road.allInstances->any(r | r.origin = n180 and r.destination = n181)) into RouteRoad
!insert (route9, Road.allInstances->any(r | r.origin = n181 and r.destination = disposal)) into RouteRoad

!create route10 : Route
!set route10.totalTravelTime := 21.0   -- 21 x 1.0 spine edges

-- Assign route10 to truck10
!insert (route10, truck10) into AssignedTo

-- Route10: select the spine edges it uses (RouteRoad is the decision variable)
!insert (route10, Road.allInstances->any(r | r.origin = depot and r.destination = n182)) into RouteRoad
!insert (route10, Road.allInstances->any(r | r.origin = n182 and r.destination = n183)) into RouteRoad
!insert (route10, Road.allInstances->any(r | r.origin = n183 and r.destination = n184)) into RouteRoad
!insert (route10, Road.allInstances->any(r | r.origin = n184 and r.destination = n185)) into RouteRoad
!insert (route10, Road.allInstances->any(r | r.origin = n185 and r.destination = n186)) into RouteRoad
!insert (route10, Road.allInstances->any(r | r.origin = n186 and r.destination = n187)) into RouteRoad
!insert (route10, Road.allInstances->any(r | r.origin = n187 and r.destination = n188)) into RouteRoad
!insert (route10, Road.allInstances->any(r | r.origin = n188 and r.destination = n189)) into RouteRoad
!insert (route10, Road.allInstances->any(r | r.origin = n189 and r.destination = n190)) into RouteRoad
!insert (route10, Road.allInstances->any(r | r.origin = n190 and r.destination = n191)) into RouteRoad
!insert (route10, Road.allInstances->any(r | r.origin = n191 and r.destination = n192)) into RouteRoad
!insert (route10, Road.allInstances->any(r | r.origin = n192 and r.destination = n193)) into RouteRoad
!insert (route10, Road.allInstances->any(r | r.origin = n193 and r.destination = n194)) into RouteRoad
!insert (route10, Road.allInstances->any(r | r.origin = n194 and r.destination = n195)) into RouteRoad
!insert (route10, Road.allInstances->any(r | r.origin = n195 and r.destination = n196)) into RouteRoad
!insert (route10, Road.allInstances->any(r | r.origin = n196 and r.destination = n197)) into RouteRoad
!insert (route10, Road.allInstances->any(r | r.origin = n197 and r.destination = n198)) into RouteRoad
!insert (route10, Road.allInstances->any(r | r.origin = n198 and r.destination = n199)) into RouteRoad
!insert (route10, Road.allInstances->any(r | r.origin = n199 and r.destination = n200)) into RouteRoad
!insert (route10, Road.allInstances->any(r | r.origin = n200 and r.destination = n201)) into RouteRoad
!insert (route10, Road.allInstances->any(r | r.origin = n201 and r.destination = disposal)) into RouteRoad

-- -----------------------------------------------------------
-- 6. Simulate garbage collection along each route
-- -----------------------------------------------------------
-- Truck1 collects all 20 bins on lane 1
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
!openter truck1 collectGarbage(bin8)
!set truck1.currentLoad := truck1.currentLoad + bin8.currentFill
!set bin8.currentFill   := 0.0
!opexit
!openter truck1 collectGarbage(bin9)
!set truck1.currentLoad := truck1.currentLoad + bin9.currentFill
!set bin9.currentFill   := 0.0
!opexit
!openter truck1 collectGarbage(bin10)
!set truck1.currentLoad := truck1.currentLoad + bin10.currentFill
!set bin10.currentFill   := 0.0
!opexit
!openter truck1 collectGarbage(bin11)
!set truck1.currentLoad := truck1.currentLoad + bin11.currentFill
!set bin11.currentFill   := 0.0
!opexit
!openter truck1 collectGarbage(bin12)
!set truck1.currentLoad := truck1.currentLoad + bin12.currentFill
!set bin12.currentFill   := 0.0
!opexit
!openter truck1 collectGarbage(bin13)
!set truck1.currentLoad := truck1.currentLoad + bin13.currentFill
!set bin13.currentFill   := 0.0
!opexit
!openter truck1 collectGarbage(bin14)
!set truck1.currentLoad := truck1.currentLoad + bin14.currentFill
!set bin14.currentFill   := 0.0
!opexit
!openter truck1 collectGarbage(bin15)
!set truck1.currentLoad := truck1.currentLoad + bin15.currentFill
!set bin15.currentFill   := 0.0
!opexit
!openter truck1 collectGarbage(bin16)
!set truck1.currentLoad := truck1.currentLoad + bin16.currentFill
!set bin16.currentFill   := 0.0
!opexit
!openter truck1 collectGarbage(bin17)
!set truck1.currentLoad := truck1.currentLoad + bin17.currentFill
!set bin17.currentFill   := 0.0
!opexit
!openter truck1 collectGarbage(bin18)
!set truck1.currentLoad := truck1.currentLoad + bin18.currentFill
!set bin18.currentFill   := 0.0
!opexit
!openter truck1 collectGarbage(bin19)
!set truck1.currentLoad := truck1.currentLoad + bin19.currentFill
!set bin19.currentFill   := 0.0
!opexit
!openter truck1 collectGarbage(bin20)
!set truck1.currentLoad := truck1.currentLoad + bin20.currentFill
!set bin20.currentFill   := 0.0
!opexit

-- Truck2 collects all 20 bins on lane 2
!openter truck2 collectGarbage(bin21)
!set truck2.currentLoad := truck2.currentLoad + bin21.currentFill
!set bin21.currentFill   := 0.0
!opexit
!openter truck2 collectGarbage(bin22)
!set truck2.currentLoad := truck2.currentLoad + bin22.currentFill
!set bin22.currentFill   := 0.0
!opexit
!openter truck2 collectGarbage(bin23)
!set truck2.currentLoad := truck2.currentLoad + bin23.currentFill
!set bin23.currentFill   := 0.0
!opexit
!openter truck2 collectGarbage(bin24)
!set truck2.currentLoad := truck2.currentLoad + bin24.currentFill
!set bin24.currentFill   := 0.0
!opexit
!openter truck2 collectGarbage(bin25)
!set truck2.currentLoad := truck2.currentLoad + bin25.currentFill
!set bin25.currentFill   := 0.0
!opexit
!openter truck2 collectGarbage(bin26)
!set truck2.currentLoad := truck2.currentLoad + bin26.currentFill
!set bin26.currentFill   := 0.0
!opexit
!openter truck2 collectGarbage(bin27)
!set truck2.currentLoad := truck2.currentLoad + bin27.currentFill
!set bin27.currentFill   := 0.0
!opexit
!openter truck2 collectGarbage(bin28)
!set truck2.currentLoad := truck2.currentLoad + bin28.currentFill
!set bin28.currentFill   := 0.0
!opexit
!openter truck2 collectGarbage(bin29)
!set truck2.currentLoad := truck2.currentLoad + bin29.currentFill
!set bin29.currentFill   := 0.0
!opexit
!openter truck2 collectGarbage(bin30)
!set truck2.currentLoad := truck2.currentLoad + bin30.currentFill
!set bin30.currentFill   := 0.0
!opexit
!openter truck2 collectGarbage(bin31)
!set truck2.currentLoad := truck2.currentLoad + bin31.currentFill
!set bin31.currentFill   := 0.0
!opexit
!openter truck2 collectGarbage(bin32)
!set truck2.currentLoad := truck2.currentLoad + bin32.currentFill
!set bin32.currentFill   := 0.0
!opexit
!openter truck2 collectGarbage(bin33)
!set truck2.currentLoad := truck2.currentLoad + bin33.currentFill
!set bin33.currentFill   := 0.0
!opexit
!openter truck2 collectGarbage(bin34)
!set truck2.currentLoad := truck2.currentLoad + bin34.currentFill
!set bin34.currentFill   := 0.0
!opexit
!openter truck2 collectGarbage(bin35)
!set truck2.currentLoad := truck2.currentLoad + bin35.currentFill
!set bin35.currentFill   := 0.0
!opexit
!openter truck2 collectGarbage(bin36)
!set truck2.currentLoad := truck2.currentLoad + bin36.currentFill
!set bin36.currentFill   := 0.0
!opexit
!openter truck2 collectGarbage(bin37)
!set truck2.currentLoad := truck2.currentLoad + bin37.currentFill
!set bin37.currentFill   := 0.0
!opexit
!openter truck2 collectGarbage(bin38)
!set truck2.currentLoad := truck2.currentLoad + bin38.currentFill
!set bin38.currentFill   := 0.0
!opexit
!openter truck2 collectGarbage(bin39)
!set truck2.currentLoad := truck2.currentLoad + bin39.currentFill
!set bin39.currentFill   := 0.0
!opexit
!openter truck2 collectGarbage(bin40)
!set truck2.currentLoad := truck2.currentLoad + bin40.currentFill
!set bin40.currentFill   := 0.0
!opexit

-- Truck3 collects all 20 bins on lane 3
!openter truck3 collectGarbage(bin41)
!set truck3.currentLoad := truck3.currentLoad + bin41.currentFill
!set bin41.currentFill   := 0.0
!opexit
!openter truck3 collectGarbage(bin42)
!set truck3.currentLoad := truck3.currentLoad + bin42.currentFill
!set bin42.currentFill   := 0.0
!opexit
!openter truck3 collectGarbage(bin43)
!set truck3.currentLoad := truck3.currentLoad + bin43.currentFill
!set bin43.currentFill   := 0.0
!opexit
!openter truck3 collectGarbage(bin44)
!set truck3.currentLoad := truck3.currentLoad + bin44.currentFill
!set bin44.currentFill   := 0.0
!opexit
!openter truck3 collectGarbage(bin45)
!set truck3.currentLoad := truck3.currentLoad + bin45.currentFill
!set bin45.currentFill   := 0.0
!opexit
!openter truck3 collectGarbage(bin46)
!set truck3.currentLoad := truck3.currentLoad + bin46.currentFill
!set bin46.currentFill   := 0.0
!opexit
!openter truck3 collectGarbage(bin47)
!set truck3.currentLoad := truck3.currentLoad + bin47.currentFill
!set bin47.currentFill   := 0.0
!opexit
!openter truck3 collectGarbage(bin48)
!set truck3.currentLoad := truck3.currentLoad + bin48.currentFill
!set bin48.currentFill   := 0.0
!opexit
!openter truck3 collectGarbage(bin49)
!set truck3.currentLoad := truck3.currentLoad + bin49.currentFill
!set bin49.currentFill   := 0.0
!opexit
!openter truck3 collectGarbage(bin50)
!set truck3.currentLoad := truck3.currentLoad + bin50.currentFill
!set bin50.currentFill   := 0.0
!opexit
!openter truck3 collectGarbage(bin51)
!set truck3.currentLoad := truck3.currentLoad + bin51.currentFill
!set bin51.currentFill   := 0.0
!opexit
!openter truck3 collectGarbage(bin52)
!set truck3.currentLoad := truck3.currentLoad + bin52.currentFill
!set bin52.currentFill   := 0.0
!opexit
!openter truck3 collectGarbage(bin53)
!set truck3.currentLoad := truck3.currentLoad + bin53.currentFill
!set bin53.currentFill   := 0.0
!opexit
!openter truck3 collectGarbage(bin54)
!set truck3.currentLoad := truck3.currentLoad + bin54.currentFill
!set bin54.currentFill   := 0.0
!opexit
!openter truck3 collectGarbage(bin55)
!set truck3.currentLoad := truck3.currentLoad + bin55.currentFill
!set bin55.currentFill   := 0.0
!opexit
!openter truck3 collectGarbage(bin56)
!set truck3.currentLoad := truck3.currentLoad + bin56.currentFill
!set bin56.currentFill   := 0.0
!opexit
!openter truck3 collectGarbage(bin57)
!set truck3.currentLoad := truck3.currentLoad + bin57.currentFill
!set bin57.currentFill   := 0.0
!opexit
!openter truck3 collectGarbage(bin58)
!set truck3.currentLoad := truck3.currentLoad + bin58.currentFill
!set bin58.currentFill   := 0.0
!opexit
!openter truck3 collectGarbage(bin59)
!set truck3.currentLoad := truck3.currentLoad + bin59.currentFill
!set bin59.currentFill   := 0.0
!opexit
!openter truck3 collectGarbage(bin60)
!set truck3.currentLoad := truck3.currentLoad + bin60.currentFill
!set bin60.currentFill   := 0.0
!opexit

-- Truck4 collects all 20 bins on lane 4
!openter truck4 collectGarbage(bin61)
!set truck4.currentLoad := truck4.currentLoad + bin61.currentFill
!set bin61.currentFill   := 0.0
!opexit
!openter truck4 collectGarbage(bin62)
!set truck4.currentLoad := truck4.currentLoad + bin62.currentFill
!set bin62.currentFill   := 0.0
!opexit
!openter truck4 collectGarbage(bin63)
!set truck4.currentLoad := truck4.currentLoad + bin63.currentFill
!set bin63.currentFill   := 0.0
!opexit
!openter truck4 collectGarbage(bin64)
!set truck4.currentLoad := truck4.currentLoad + bin64.currentFill
!set bin64.currentFill   := 0.0
!opexit
!openter truck4 collectGarbage(bin65)
!set truck4.currentLoad := truck4.currentLoad + bin65.currentFill
!set bin65.currentFill   := 0.0
!opexit
!openter truck4 collectGarbage(bin66)
!set truck4.currentLoad := truck4.currentLoad + bin66.currentFill
!set bin66.currentFill   := 0.0
!opexit
!openter truck4 collectGarbage(bin67)
!set truck4.currentLoad := truck4.currentLoad + bin67.currentFill
!set bin67.currentFill   := 0.0
!opexit
!openter truck4 collectGarbage(bin68)
!set truck4.currentLoad := truck4.currentLoad + bin68.currentFill
!set bin68.currentFill   := 0.0
!opexit
!openter truck4 collectGarbage(bin69)
!set truck4.currentLoad := truck4.currentLoad + bin69.currentFill
!set bin69.currentFill   := 0.0
!opexit
!openter truck4 collectGarbage(bin70)
!set truck4.currentLoad := truck4.currentLoad + bin70.currentFill
!set bin70.currentFill   := 0.0
!opexit
!openter truck4 collectGarbage(bin71)
!set truck4.currentLoad := truck4.currentLoad + bin71.currentFill
!set bin71.currentFill   := 0.0
!opexit
!openter truck4 collectGarbage(bin72)
!set truck4.currentLoad := truck4.currentLoad + bin72.currentFill
!set bin72.currentFill   := 0.0
!opexit
!openter truck4 collectGarbage(bin73)
!set truck4.currentLoad := truck4.currentLoad + bin73.currentFill
!set bin73.currentFill   := 0.0
!opexit
!openter truck4 collectGarbage(bin74)
!set truck4.currentLoad := truck4.currentLoad + bin74.currentFill
!set bin74.currentFill   := 0.0
!opexit
!openter truck4 collectGarbage(bin75)
!set truck4.currentLoad := truck4.currentLoad + bin75.currentFill
!set bin75.currentFill   := 0.0
!opexit
!openter truck4 collectGarbage(bin76)
!set truck4.currentLoad := truck4.currentLoad + bin76.currentFill
!set bin76.currentFill   := 0.0
!opexit
!openter truck4 collectGarbage(bin77)
!set truck4.currentLoad := truck4.currentLoad + bin77.currentFill
!set bin77.currentFill   := 0.0
!opexit
!openter truck4 collectGarbage(bin78)
!set truck4.currentLoad := truck4.currentLoad + bin78.currentFill
!set bin78.currentFill   := 0.0
!opexit
!openter truck4 collectGarbage(bin79)
!set truck4.currentLoad := truck4.currentLoad + bin79.currentFill
!set bin79.currentFill   := 0.0
!opexit
!openter truck4 collectGarbage(bin80)
!set truck4.currentLoad := truck4.currentLoad + bin80.currentFill
!set bin80.currentFill   := 0.0
!opexit

-- Truck5 collects all 20 bins on lane 5
!openter truck5 collectGarbage(bin81)
!set truck5.currentLoad := truck5.currentLoad + bin81.currentFill
!set bin81.currentFill   := 0.0
!opexit
!openter truck5 collectGarbage(bin82)
!set truck5.currentLoad := truck5.currentLoad + bin82.currentFill
!set bin82.currentFill   := 0.0
!opexit
!openter truck5 collectGarbage(bin83)
!set truck5.currentLoad := truck5.currentLoad + bin83.currentFill
!set bin83.currentFill   := 0.0
!opexit
!openter truck5 collectGarbage(bin84)
!set truck5.currentLoad := truck5.currentLoad + bin84.currentFill
!set bin84.currentFill   := 0.0
!opexit
!openter truck5 collectGarbage(bin85)
!set truck5.currentLoad := truck5.currentLoad + bin85.currentFill
!set bin85.currentFill   := 0.0
!opexit
!openter truck5 collectGarbage(bin86)
!set truck5.currentLoad := truck5.currentLoad + bin86.currentFill
!set bin86.currentFill   := 0.0
!opexit
!openter truck5 collectGarbage(bin87)
!set truck5.currentLoad := truck5.currentLoad + bin87.currentFill
!set bin87.currentFill   := 0.0
!opexit
!openter truck5 collectGarbage(bin88)
!set truck5.currentLoad := truck5.currentLoad + bin88.currentFill
!set bin88.currentFill   := 0.0
!opexit
!openter truck5 collectGarbage(bin89)
!set truck5.currentLoad := truck5.currentLoad + bin89.currentFill
!set bin89.currentFill   := 0.0
!opexit
!openter truck5 collectGarbage(bin90)
!set truck5.currentLoad := truck5.currentLoad + bin90.currentFill
!set bin90.currentFill   := 0.0
!opexit
!openter truck5 collectGarbage(bin91)
!set truck5.currentLoad := truck5.currentLoad + bin91.currentFill
!set bin91.currentFill   := 0.0
!opexit
!openter truck5 collectGarbage(bin92)
!set truck5.currentLoad := truck5.currentLoad + bin92.currentFill
!set bin92.currentFill   := 0.0
!opexit
!openter truck5 collectGarbage(bin93)
!set truck5.currentLoad := truck5.currentLoad + bin93.currentFill
!set bin93.currentFill   := 0.0
!opexit
!openter truck5 collectGarbage(bin94)
!set truck5.currentLoad := truck5.currentLoad + bin94.currentFill
!set bin94.currentFill   := 0.0
!opexit
!openter truck5 collectGarbage(bin95)
!set truck5.currentLoad := truck5.currentLoad + bin95.currentFill
!set bin95.currentFill   := 0.0
!opexit
!openter truck5 collectGarbage(bin96)
!set truck5.currentLoad := truck5.currentLoad + bin96.currentFill
!set bin96.currentFill   := 0.0
!opexit
!openter truck5 collectGarbage(bin97)
!set truck5.currentLoad := truck5.currentLoad + bin97.currentFill
!set bin97.currentFill   := 0.0
!opexit
!openter truck5 collectGarbage(bin98)
!set truck5.currentLoad := truck5.currentLoad + bin98.currentFill
!set bin98.currentFill   := 0.0
!opexit
!openter truck5 collectGarbage(bin99)
!set truck5.currentLoad := truck5.currentLoad + bin99.currentFill
!set bin99.currentFill   := 0.0
!opexit
!openter truck5 collectGarbage(bin100)
!set truck5.currentLoad := truck5.currentLoad + bin100.currentFill
!set bin100.currentFill   := 0.0
!opexit

-- Truck6 collects all 20 bins on lane 6
!openter truck6 collectGarbage(bin101)
!set truck6.currentLoad := truck6.currentLoad + bin101.currentFill
!set bin101.currentFill   := 0.0
!opexit
!openter truck6 collectGarbage(bin102)
!set truck6.currentLoad := truck6.currentLoad + bin102.currentFill
!set bin102.currentFill   := 0.0
!opexit
!openter truck6 collectGarbage(bin103)
!set truck6.currentLoad := truck6.currentLoad + bin103.currentFill
!set bin103.currentFill   := 0.0
!opexit
!openter truck6 collectGarbage(bin104)
!set truck6.currentLoad := truck6.currentLoad + bin104.currentFill
!set bin104.currentFill   := 0.0
!opexit
!openter truck6 collectGarbage(bin105)
!set truck6.currentLoad := truck6.currentLoad + bin105.currentFill
!set bin105.currentFill   := 0.0
!opexit
!openter truck6 collectGarbage(bin106)
!set truck6.currentLoad := truck6.currentLoad + bin106.currentFill
!set bin106.currentFill   := 0.0
!opexit
!openter truck6 collectGarbage(bin107)
!set truck6.currentLoad := truck6.currentLoad + bin107.currentFill
!set bin107.currentFill   := 0.0
!opexit
!openter truck6 collectGarbage(bin108)
!set truck6.currentLoad := truck6.currentLoad + bin108.currentFill
!set bin108.currentFill   := 0.0
!opexit
!openter truck6 collectGarbage(bin109)
!set truck6.currentLoad := truck6.currentLoad + bin109.currentFill
!set bin109.currentFill   := 0.0
!opexit
!openter truck6 collectGarbage(bin110)
!set truck6.currentLoad := truck6.currentLoad + bin110.currentFill
!set bin110.currentFill   := 0.0
!opexit
!openter truck6 collectGarbage(bin111)
!set truck6.currentLoad := truck6.currentLoad + bin111.currentFill
!set bin111.currentFill   := 0.0
!opexit
!openter truck6 collectGarbage(bin112)
!set truck6.currentLoad := truck6.currentLoad + bin112.currentFill
!set bin112.currentFill   := 0.0
!opexit
!openter truck6 collectGarbage(bin113)
!set truck6.currentLoad := truck6.currentLoad + bin113.currentFill
!set bin113.currentFill   := 0.0
!opexit
!openter truck6 collectGarbage(bin114)
!set truck6.currentLoad := truck6.currentLoad + bin114.currentFill
!set bin114.currentFill   := 0.0
!opexit
!openter truck6 collectGarbage(bin115)
!set truck6.currentLoad := truck6.currentLoad + bin115.currentFill
!set bin115.currentFill   := 0.0
!opexit
!openter truck6 collectGarbage(bin116)
!set truck6.currentLoad := truck6.currentLoad + bin116.currentFill
!set bin116.currentFill   := 0.0
!opexit
!openter truck6 collectGarbage(bin117)
!set truck6.currentLoad := truck6.currentLoad + bin117.currentFill
!set bin117.currentFill   := 0.0
!opexit
!openter truck6 collectGarbage(bin118)
!set truck6.currentLoad := truck6.currentLoad + bin118.currentFill
!set bin118.currentFill   := 0.0
!opexit
!openter truck6 collectGarbage(bin119)
!set truck6.currentLoad := truck6.currentLoad + bin119.currentFill
!set bin119.currentFill   := 0.0
!opexit
!openter truck6 collectGarbage(bin120)
!set truck6.currentLoad := truck6.currentLoad + bin120.currentFill
!set bin120.currentFill   := 0.0
!opexit

-- Truck7 collects all 20 bins on lane 7
!openter truck7 collectGarbage(bin121)
!set truck7.currentLoad := truck7.currentLoad + bin121.currentFill
!set bin121.currentFill   := 0.0
!opexit
!openter truck7 collectGarbage(bin122)
!set truck7.currentLoad := truck7.currentLoad + bin122.currentFill
!set bin122.currentFill   := 0.0
!opexit
!openter truck7 collectGarbage(bin123)
!set truck7.currentLoad := truck7.currentLoad + bin123.currentFill
!set bin123.currentFill   := 0.0
!opexit
!openter truck7 collectGarbage(bin124)
!set truck7.currentLoad := truck7.currentLoad + bin124.currentFill
!set bin124.currentFill   := 0.0
!opexit
!openter truck7 collectGarbage(bin125)
!set truck7.currentLoad := truck7.currentLoad + bin125.currentFill
!set bin125.currentFill   := 0.0
!opexit
!openter truck7 collectGarbage(bin126)
!set truck7.currentLoad := truck7.currentLoad + bin126.currentFill
!set bin126.currentFill   := 0.0
!opexit
!openter truck7 collectGarbage(bin127)
!set truck7.currentLoad := truck7.currentLoad + bin127.currentFill
!set bin127.currentFill   := 0.0
!opexit
!openter truck7 collectGarbage(bin128)
!set truck7.currentLoad := truck7.currentLoad + bin128.currentFill
!set bin128.currentFill   := 0.0
!opexit
!openter truck7 collectGarbage(bin129)
!set truck7.currentLoad := truck7.currentLoad + bin129.currentFill
!set bin129.currentFill   := 0.0
!opexit
!openter truck7 collectGarbage(bin130)
!set truck7.currentLoad := truck7.currentLoad + bin130.currentFill
!set bin130.currentFill   := 0.0
!opexit
!openter truck7 collectGarbage(bin131)
!set truck7.currentLoad := truck7.currentLoad + bin131.currentFill
!set bin131.currentFill   := 0.0
!opexit
!openter truck7 collectGarbage(bin132)
!set truck7.currentLoad := truck7.currentLoad + bin132.currentFill
!set bin132.currentFill   := 0.0
!opexit
!openter truck7 collectGarbage(bin133)
!set truck7.currentLoad := truck7.currentLoad + bin133.currentFill
!set bin133.currentFill   := 0.0
!opexit
!openter truck7 collectGarbage(bin134)
!set truck7.currentLoad := truck7.currentLoad + bin134.currentFill
!set bin134.currentFill   := 0.0
!opexit
!openter truck7 collectGarbage(bin135)
!set truck7.currentLoad := truck7.currentLoad + bin135.currentFill
!set bin135.currentFill   := 0.0
!opexit
!openter truck7 collectGarbage(bin136)
!set truck7.currentLoad := truck7.currentLoad + bin136.currentFill
!set bin136.currentFill   := 0.0
!opexit
!openter truck7 collectGarbage(bin137)
!set truck7.currentLoad := truck7.currentLoad + bin137.currentFill
!set bin137.currentFill   := 0.0
!opexit
!openter truck7 collectGarbage(bin138)
!set truck7.currentLoad := truck7.currentLoad + bin138.currentFill
!set bin138.currentFill   := 0.0
!opexit
!openter truck7 collectGarbage(bin139)
!set truck7.currentLoad := truck7.currentLoad + bin139.currentFill
!set bin139.currentFill   := 0.0
!opexit
!openter truck7 collectGarbage(bin140)
!set truck7.currentLoad := truck7.currentLoad + bin140.currentFill
!set bin140.currentFill   := 0.0
!opexit

-- Truck8 collects all 20 bins on lane 8
!openter truck8 collectGarbage(bin141)
!set truck8.currentLoad := truck8.currentLoad + bin141.currentFill
!set bin141.currentFill   := 0.0
!opexit
!openter truck8 collectGarbage(bin142)
!set truck8.currentLoad := truck8.currentLoad + bin142.currentFill
!set bin142.currentFill   := 0.0
!opexit
!openter truck8 collectGarbage(bin143)
!set truck8.currentLoad := truck8.currentLoad + bin143.currentFill
!set bin143.currentFill   := 0.0
!opexit
!openter truck8 collectGarbage(bin144)
!set truck8.currentLoad := truck8.currentLoad + bin144.currentFill
!set bin144.currentFill   := 0.0
!opexit
!openter truck8 collectGarbage(bin145)
!set truck8.currentLoad := truck8.currentLoad + bin145.currentFill
!set bin145.currentFill   := 0.0
!opexit
!openter truck8 collectGarbage(bin146)
!set truck8.currentLoad := truck8.currentLoad + bin146.currentFill
!set bin146.currentFill   := 0.0
!opexit
!openter truck8 collectGarbage(bin147)
!set truck8.currentLoad := truck8.currentLoad + bin147.currentFill
!set bin147.currentFill   := 0.0
!opexit
!openter truck8 collectGarbage(bin148)
!set truck8.currentLoad := truck8.currentLoad + bin148.currentFill
!set bin148.currentFill   := 0.0
!opexit
!openter truck8 collectGarbage(bin149)
!set truck8.currentLoad := truck8.currentLoad + bin149.currentFill
!set bin149.currentFill   := 0.0
!opexit
!openter truck8 collectGarbage(bin150)
!set truck8.currentLoad := truck8.currentLoad + bin150.currentFill
!set bin150.currentFill   := 0.0
!opexit
!openter truck8 collectGarbage(bin151)
!set truck8.currentLoad := truck8.currentLoad + bin151.currentFill
!set bin151.currentFill   := 0.0
!opexit
!openter truck8 collectGarbage(bin152)
!set truck8.currentLoad := truck8.currentLoad + bin152.currentFill
!set bin152.currentFill   := 0.0
!opexit
!openter truck8 collectGarbage(bin153)
!set truck8.currentLoad := truck8.currentLoad + bin153.currentFill
!set bin153.currentFill   := 0.0
!opexit
!openter truck8 collectGarbage(bin154)
!set truck8.currentLoad := truck8.currentLoad + bin154.currentFill
!set bin154.currentFill   := 0.0
!opexit
!openter truck8 collectGarbage(bin155)
!set truck8.currentLoad := truck8.currentLoad + bin155.currentFill
!set bin155.currentFill   := 0.0
!opexit
!openter truck8 collectGarbage(bin156)
!set truck8.currentLoad := truck8.currentLoad + bin156.currentFill
!set bin156.currentFill   := 0.0
!opexit
!openter truck8 collectGarbage(bin157)
!set truck8.currentLoad := truck8.currentLoad + bin157.currentFill
!set bin157.currentFill   := 0.0
!opexit
!openter truck8 collectGarbage(bin158)
!set truck8.currentLoad := truck8.currentLoad + bin158.currentFill
!set bin158.currentFill   := 0.0
!opexit
!openter truck8 collectGarbage(bin159)
!set truck8.currentLoad := truck8.currentLoad + bin159.currentFill
!set bin159.currentFill   := 0.0
!opexit
!openter truck8 collectGarbage(bin160)
!set truck8.currentLoad := truck8.currentLoad + bin160.currentFill
!set bin160.currentFill   := 0.0
!opexit

-- Truck9 collects all 20 bins on lane 9
!openter truck9 collectGarbage(bin161)
!set truck9.currentLoad := truck9.currentLoad + bin161.currentFill
!set bin161.currentFill   := 0.0
!opexit
!openter truck9 collectGarbage(bin162)
!set truck9.currentLoad := truck9.currentLoad + bin162.currentFill
!set bin162.currentFill   := 0.0
!opexit
!openter truck9 collectGarbage(bin163)
!set truck9.currentLoad := truck9.currentLoad + bin163.currentFill
!set bin163.currentFill   := 0.0
!opexit
!openter truck9 collectGarbage(bin164)
!set truck9.currentLoad := truck9.currentLoad + bin164.currentFill
!set bin164.currentFill   := 0.0
!opexit
!openter truck9 collectGarbage(bin165)
!set truck9.currentLoad := truck9.currentLoad + bin165.currentFill
!set bin165.currentFill   := 0.0
!opexit
!openter truck9 collectGarbage(bin166)
!set truck9.currentLoad := truck9.currentLoad + bin166.currentFill
!set bin166.currentFill   := 0.0
!opexit
!openter truck9 collectGarbage(bin167)
!set truck9.currentLoad := truck9.currentLoad + bin167.currentFill
!set bin167.currentFill   := 0.0
!opexit
!openter truck9 collectGarbage(bin168)
!set truck9.currentLoad := truck9.currentLoad + bin168.currentFill
!set bin168.currentFill   := 0.0
!opexit
!openter truck9 collectGarbage(bin169)
!set truck9.currentLoad := truck9.currentLoad + bin169.currentFill
!set bin169.currentFill   := 0.0
!opexit
!openter truck9 collectGarbage(bin170)
!set truck9.currentLoad := truck9.currentLoad + bin170.currentFill
!set bin170.currentFill   := 0.0
!opexit
!openter truck9 collectGarbage(bin171)
!set truck9.currentLoad := truck9.currentLoad + bin171.currentFill
!set bin171.currentFill   := 0.0
!opexit
!openter truck9 collectGarbage(bin172)
!set truck9.currentLoad := truck9.currentLoad + bin172.currentFill
!set bin172.currentFill   := 0.0
!opexit
!openter truck9 collectGarbage(bin173)
!set truck9.currentLoad := truck9.currentLoad + bin173.currentFill
!set bin173.currentFill   := 0.0
!opexit
!openter truck9 collectGarbage(bin174)
!set truck9.currentLoad := truck9.currentLoad + bin174.currentFill
!set bin174.currentFill   := 0.0
!opexit
!openter truck9 collectGarbage(bin175)
!set truck9.currentLoad := truck9.currentLoad + bin175.currentFill
!set bin175.currentFill   := 0.0
!opexit
!openter truck9 collectGarbage(bin176)
!set truck9.currentLoad := truck9.currentLoad + bin176.currentFill
!set bin176.currentFill   := 0.0
!opexit
!openter truck9 collectGarbage(bin177)
!set truck9.currentLoad := truck9.currentLoad + bin177.currentFill
!set bin177.currentFill   := 0.0
!opexit
!openter truck9 collectGarbage(bin178)
!set truck9.currentLoad := truck9.currentLoad + bin178.currentFill
!set bin178.currentFill   := 0.0
!opexit
!openter truck9 collectGarbage(bin179)
!set truck9.currentLoad := truck9.currentLoad + bin179.currentFill
!set bin179.currentFill   := 0.0
!opexit
!openter truck9 collectGarbage(bin180)
!set truck9.currentLoad := truck9.currentLoad + bin180.currentFill
!set bin180.currentFill   := 0.0
!opexit

-- Truck10 collects all 20 bins on lane 10
!openter truck10 collectGarbage(bin181)
!set truck10.currentLoad := truck10.currentLoad + bin181.currentFill
!set bin181.currentFill   := 0.0
!opexit
!openter truck10 collectGarbage(bin182)
!set truck10.currentLoad := truck10.currentLoad + bin182.currentFill
!set bin182.currentFill   := 0.0
!opexit
!openter truck10 collectGarbage(bin183)
!set truck10.currentLoad := truck10.currentLoad + bin183.currentFill
!set bin183.currentFill   := 0.0
!opexit
!openter truck10 collectGarbage(bin184)
!set truck10.currentLoad := truck10.currentLoad + bin184.currentFill
!set bin184.currentFill   := 0.0
!opexit
!openter truck10 collectGarbage(bin185)
!set truck10.currentLoad := truck10.currentLoad + bin185.currentFill
!set bin185.currentFill   := 0.0
!opexit
!openter truck10 collectGarbage(bin186)
!set truck10.currentLoad := truck10.currentLoad + bin186.currentFill
!set bin186.currentFill   := 0.0
!opexit
!openter truck10 collectGarbage(bin187)
!set truck10.currentLoad := truck10.currentLoad + bin187.currentFill
!set bin187.currentFill   := 0.0
!opexit
!openter truck10 collectGarbage(bin188)
!set truck10.currentLoad := truck10.currentLoad + bin188.currentFill
!set bin188.currentFill   := 0.0
!opexit
!openter truck10 collectGarbage(bin189)
!set truck10.currentLoad := truck10.currentLoad + bin189.currentFill
!set bin189.currentFill   := 0.0
!opexit
!openter truck10 collectGarbage(bin190)
!set truck10.currentLoad := truck10.currentLoad + bin190.currentFill
!set bin190.currentFill   := 0.0
!opexit
!openter truck10 collectGarbage(bin191)
!set truck10.currentLoad := truck10.currentLoad + bin191.currentFill
!set bin191.currentFill   := 0.0
!opexit
!openter truck10 collectGarbage(bin192)
!set truck10.currentLoad := truck10.currentLoad + bin192.currentFill
!set bin192.currentFill   := 0.0
!opexit
!openter truck10 collectGarbage(bin193)
!set truck10.currentLoad := truck10.currentLoad + bin193.currentFill
!set bin193.currentFill   := 0.0
!opexit
!openter truck10 collectGarbage(bin194)
!set truck10.currentLoad := truck10.currentLoad + bin194.currentFill
!set bin194.currentFill   := 0.0
!opexit
!openter truck10 collectGarbage(bin195)
!set truck10.currentLoad := truck10.currentLoad + bin195.currentFill
!set bin195.currentFill   := 0.0
!opexit
!openter truck10 collectGarbage(bin196)
!set truck10.currentLoad := truck10.currentLoad + bin196.currentFill
!set bin196.currentFill   := 0.0
!opexit
!openter truck10 collectGarbage(bin197)
!set truck10.currentLoad := truck10.currentLoad + bin197.currentFill
!set bin197.currentFill   := 0.0
!opexit
!openter truck10 collectGarbage(bin198)
!set truck10.currentLoad := truck10.currentLoad + bin198.currentFill
!set bin198.currentFill   := 0.0
!opexit
!openter truck10 collectGarbage(bin199)
!set truck10.currentLoad := truck10.currentLoad + bin199.currentFill
!set bin199.currentFill   := 0.0
!opexit
!openter truck10 collectGarbage(bin200)
!set truck10.currentLoad := truck10.currentLoad + bin200.currentFill
!set bin200.currentFill   := 0.0
!opexit

-- -----------------------------------------------------------
-- 7. Check all constraints
-- -----------------------------------------------------------
check

-- Expected: all invariants true.
-- truck1.currentLoad = 9.6 m3  (<= maxCapacity 20.0)
-- truck2.currentLoad = 9.6 m3  (<= maxCapacity 20.0)
-- truck3.currentLoad = 9.6 m3  (<= maxCapacity 20.0)
-- truck4.currentLoad = 9.6 m3  (<= maxCapacity 20.0)
-- truck5.currentLoad = 9.6 m3  (<= maxCapacity 20.0)
-- truck6.currentLoad = 9.6 m3  (<= maxCapacity 20.0)
-- truck7.currentLoad = 9.6 m3  (<= maxCapacity 20.0)
-- truck8.currentLoad = 9.6 m3  (<= maxCapacity 20.0)
-- truck9.currentLoad = 9.6 m3  (<= maxCapacity 20.0)
-- truck10.currentLoad = 9.6 m3  (<= maxCapacity 20.0)
-- Each route: edgeCost() = 21.0 min; fuelPenalty-relevant check: 21.0 <= fuelRange 50.0
-- All 200 bins have currentFill = 0 (collected)
-- All 10 trucks assigned exactly one route; 500 Road instances total
--   (210 spine edges used by RouteRoad + 290 unused decorative edges)
