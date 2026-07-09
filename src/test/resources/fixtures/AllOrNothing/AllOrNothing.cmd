-- ===========================================================
-- Synthetic fixture instance for use2qubo's own test suite.
-- 1 Picker, 4 Options, all chosen so `check` passes at load.
-- ===========================================================

!create p1 : Picker

!create o1 : Option
!create o2 : Option
!create o3 : Option
!create o4 : Option

!insert (p1, o1) into Chosen
!insert (p1, o2) into Chosen
!insert (p1, o3) into Chosen
!insert (p1, o4) into Chosen

check
