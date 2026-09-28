-- ===========================================================
-- JAVA-020: MAX-CLIQUE pairwise ablation.
-- Same 10-vertex, 18-edge graph and optimal clique as
-- examples/MaxClique/MaxClique.cmd (AutoQUBO paper, Moraglio et al.,
-- GECCO 2022, Figure 3 / Section 5.2), plus the 27 non-adjacent
-- (complement) pairs materialised as NonAdjacent links.
--
-- Optimal solution: {v3, v4, v6, v7, v9} - clique of size 5.
-- ===========================================================

-- -------------------------------------------------------
-- Vertices
-- -------------------------------------------------------
!create v1  : Vertex
!set v1.vertexId  := 1
!create v2  : Vertex
!set v2.vertexId  := 2
!create v3  : Vertex
!set v3.vertexId  := 3
!create v4  : Vertex
!set v4.vertexId  := 4
!create v5  : Vertex
!set v5.vertexId  := 5
!create v6  : Vertex
!set v6.vertexId  := 6
!create v7  : Vertex
!set v7.vertexId  := 7
!create v8  : Vertex
!set v8.vertexId  := 8
!create v9  : Vertex
!set v9.vertexId  := 9
!create v10 : Vertex
!set v10.vertexId := 10

-- -------------------------------------------------------
-- Edges (kept for parity with examples/MaxClique; unused by
-- nonAdjacentPenalty, which only reads NonAdjacent).
-- Paper edge set: {(1,3),(2,4),(2,5),(3,6),(3,9),(3,7),(3,4),
--                  (4,6),(4,9),(4,7),(4,8),(5,7),(5,8),(6,7),
--                  (6,9),(7,8),(7,9),(7,10)}
-- -------------------------------------------------------
!insert (v1,  v3)  into Edge
!insert (v2,  v4)  into Edge
!insert (v2,  v5)  into Edge
!insert (v3,  v6)  into Edge
!insert (v3,  v9)  into Edge
!insert (v3,  v7)  into Edge
!insert (v3,  v4)  into Edge
!insert (v4,  v6)  into Edge
!insert (v4,  v9)  into Edge
!insert (v4,  v7)  into Edge
!insert (v4,  v8)  into Edge
!insert (v5,  v7)  into Edge
!insert (v5,  v8)  into Edge
!insert (v6,  v7)  into Edge
!insert (v6,  v9)  into Edge
!insert (v7,  v8)  into Edge
!insert (v7,  v9)  into Edge
!insert (v7,  v10) into Edge

-- -------------------------------------------------------
-- NonAdjacent: complement of the 18 edges above, over the same
-- 10-vertex graph (C(10,2) = 45 pairs total, 45 - 18 = 27 non-edges).
-- -------------------------------------------------------
!insert (v1,  v2)  into NonAdjacent
!insert (v1,  v4)  into NonAdjacent
!insert (v1,  v5)  into NonAdjacent
!insert (v1,  v6)  into NonAdjacent
!insert (v1,  v7)  into NonAdjacent
!insert (v1,  v8)  into NonAdjacent
!insert (v1,  v9)  into NonAdjacent
!insert (v1,  v10) into NonAdjacent
!insert (v2,  v3)  into NonAdjacent
!insert (v2,  v6)  into NonAdjacent
!insert (v2,  v7)  into NonAdjacent
!insert (v2,  v8)  into NonAdjacent
!insert (v2,  v9)  into NonAdjacent
!insert (v2,  v10) into NonAdjacent
!insert (v3,  v5)  into NonAdjacent
!insert (v3,  v8)  into NonAdjacent
!insert (v3,  v10) into NonAdjacent
!insert (v4,  v5)  into NonAdjacent
!insert (v4,  v10) into NonAdjacent
!insert (v5,  v6)  into NonAdjacent
!insert (v5,  v9)  into NonAdjacent
!insert (v5,  v10) into NonAdjacent
!insert (v6,  v8)  into NonAdjacent
!insert (v6,  v10) into NonAdjacent
!insert (v8,  v9)  into NonAdjacent
!insert (v8,  v10) into NonAdjacent
!insert (v9,  v10) into NonAdjacent

-- -------------------------------------------------------
-- Solution: optimal clique {v3, v4, v6, v7, v9}
-- All pairs are connected: verify with `check` below.
-- -------------------------------------------------------
!create sol : Solution
!insert (sol, v3) into Contains
!insert (sol, v4) into Contains
!insert (sol, v6) into Contains
!insert (sol, v7) into Contains
!insert (sol, v9) into Contains

check
