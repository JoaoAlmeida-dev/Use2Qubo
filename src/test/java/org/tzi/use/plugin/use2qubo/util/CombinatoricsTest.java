package org.tzi.use.plugin.use2qubo.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CombinatoricsTest {

    @Test
    void binomialOfFiveChooseTwo() {
        assertEquals(10, Combinatorics.binomial(5, 2));
    }

    @Test
    void binomialChooseZeroIsOne() {
        assertEquals(1, Combinatorics.binomial(7, 0));
    }

    @Test
    void binomialChooseNIsOne() {
        assertEquals(1, Combinatorics.binomial(7, 7));
    }

    @Test
    void binomialOutOfRangeKReturnsZero() {
        assertEquals(0, Combinatorics.binomial(5, -1));
        assertEquals(0, Combinatorics.binomial(5, 6));
    }
}
