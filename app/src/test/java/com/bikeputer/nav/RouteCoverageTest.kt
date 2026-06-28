package com.bikeputer.nav

import com.bikeputer.domain.GeoPos
import org.junit.Assert.assertEquals
import org.junit.Test

class RouteCoverageTest {
    // Four vertices along the equator, ~111 m apart.
    private val planned = listOf(
        GeoPos(0.0, 0.000), GeoPos(0.0, 0.001), GeoPos(0.0, 0.002), GeoPos(0.0, 0.003),
    )

    @Test fun full_coverage_when_ridden_equals_planned() {
        assertEquals(1.0f, routeCoverage(planned, planned), 1e-4f)
    }

    @Test fun zero_coverage_for_empty_ridden() {
        assertEquals(0.0f, routeCoverage(planned, emptyList()), 1e-4f)
    }

    @Test fun zero_coverage_for_empty_planned() {
        assertEquals(0.0f, routeCoverage(emptyList(), planned), 1e-4f)
    }

    @Test fun half_coverage_when_ridden_covers_first_half() {
        // Ridden passes only the first two vertices; v2/v3 are ~111 m away (> 35 m).
        val ridden = listOf(GeoPos(0.0, 0.000), GeoPos(0.0, 0.001))
        assertEquals(0.5f, routeCoverage(planned, ridden), 1e-4f)
    }

    @Test fun threshold_changes_the_result() {
        // A single vertex; the lone ridden point is ~55.6 m away (0.0005° lon).
        val one = listOf(GeoPos(0.0, 0.0))
        val ridden = listOf(GeoPos(0.0, 0.0005))
        assertEquals(1.0f, routeCoverage(one, ridden, thresholdM = 100.0), 1e-4f)
        assertEquals(0.0f, routeCoverage(one, ridden, thresholdM = 35.0), 1e-4f)
    }
}
