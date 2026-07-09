-- ===========================================================
-- Synthetic fixture instance for use2qubo's own test suite.
-- 1 Picker, 3 Options (weights 5/10/15), 1 Tag.
-- Chosen(p1,o1) satisfies exactlyOneChosen so `check` passes at load.
-- ===========================================================

!create p1 : Picker

!create o1 : Option
!set o1.weight := 5.0
!create o2 : Option
!set o2.weight := 10.0
!create o3 : Option
!set o3.weight := 15.0

!create t1 : Tag

!insert (p1, o1) into Chosen
!insert (p1, t1) into Marked

check
