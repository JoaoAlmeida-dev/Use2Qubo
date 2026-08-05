package org.tzi.use.plugin.use2qubo.qubo.engine;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PolySamplerTest {

    @Test
    void sample_reportsStructuredProgressWithCorrectCountsAndTotal() throws Exception {
        int n = 4;
        List<ProgressEvent> events = new ArrayList<>();

        PolySampler.sample(n, 0, 2, Collections.emptyMap(), "cost",
                x -> 0.0, null, events::add);

        // degree 0: C(4,0)=1, degree 1: C(4,1)=4, degree 2: C(4,2)=6
        List<ProgressEvent> degree0 = filterByDegree(events, 0);
        List<ProgressEvent> degree1 = filterByDegree(events, 1);
        List<ProgressEvent> degree2 = filterByDegree(events, 2);

        assertEquals(1, degree0.size());
        assertEquals(4, degree1.size());
        assertEquals(6, degree2.size());

        assertCountsAndTotalProgression(degree0, 1);
        assertCountsAndTotalProgression(degree1, 4);
        assertCountsAndTotalProgression(degree2, 6);

        for (ProgressEvent e : events) {
            assertEquals("cost", e.samplePrefix);
        }
    }

    @Test
    void sample_withNullStructuredCallbackDoesNotThrow() throws Exception {
        PolySampler.sample(3, 0, 2, Collections.emptyMap(), "pen", x -> 0.0, null, null);
        // reaching here without NPE is the assertion
    }

    private static List<ProgressEvent> filterByDegree(List<ProgressEvent> events, int degree) {
        List<ProgressEvent> result = new ArrayList<>();
        for (ProgressEvent e : events) {
            if (e.degree == degree) result.add(e);
        }
        return result;
    }

    private static void assertCountsAndTotalProgression(List<ProgressEvent> events, int expectedTotal) {
        for (int i = 0; i < events.size(); i++) {
            ProgressEvent e = events.get(i);
            assertEquals(expectedTotal, e.total);
            assertEquals(i + 1, e.current);
        }
        assertTrue(events.get(events.size() - 1).current == expectedTotal);
    }
}
