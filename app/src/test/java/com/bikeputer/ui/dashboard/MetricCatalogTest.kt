package com.bikeputer.ui.dashboard

import com.bikeputer.domain.MetricId
import com.bikeputer.domain.RideState
import org.junit.Assert.assertEquals
import org.junit.Test

class MetricCatalogTest {

    private val empty = RideState.EMPTY
    private val live = RideState.EMPTY.copy(
        instantPowerW = 250, heartRateBpm = 150, speedKmh = 30.0, distanceM = 12_000.0,
        elevationGainM = 100.0, powerZone = 4, hrZone = 3,
        powerConnected = true, hrConnected = true, gpsAvailable = true,
    )

    @Test fun every_metric_reads_without_crashing() {
        for (id in MetricId.values()) {
            val r = MetricCatalog.read(id, empty, imperial = false, ftp = 200)
            // label always present; value never blank
            assert(r.label.isNotBlank())
            assert(r.value.isNotBlank())
        }
    }

    @Test fun missing_values_render_em_dash() {
        assertEquals("––", MetricCatalog.read(MetricId.POWER, empty, false, 200).value)
        assertEquals("––", MetricCatalog.read(MetricId.CADENCE, empty, false, 200).value)
    }

    @Test fun power_uses_dim_when_disconnected_and_text_when_connected() {
        assertEquals(ColorRole.Dim, MetricCatalog.read(MetricId.POWER, empty, false, 200).color)
        assertEquals(ColorRole.Text, MetricCatalog.read(MetricId.POWER, live, false, 200).color)
    }

    @Test fun heart_rate_color_role_follows_connectivity() {
        assertEquals(ColorRole.Dim, MetricCatalog.read(MetricId.HEART_RATE, empty, false, 200).color)
        assertEquals(ColorRole.HeartRate, MetricCatalog.read(MetricId.HEART_RATE, live, false, 200).color)
    }

    @Test fun zone_metrics_use_zone_roles() {
        assertEquals(ColorRole.PowerZone, MetricCatalog.read(MetricId.POWER_ZONE, live, false, 200).color)
        assertEquals(ColorRole.HrZone, MetricCatalog.read(MetricId.HR_ZONE, live, false, 200).color)
        assertEquals("Z4", MetricCatalog.read(MetricId.POWER_ZONE, live, false, 200).value)
    }

    @Test fun speed_and_distance_respect_imperial() {
        // 30 km/h ≈ 18.6 mph
        assertEquals("18.6", MetricCatalog.read(MetricId.SPEED, live, imperial = true, ftp = 200).value)
        // 12 km ≈ 7.5 mi
        assertEquals("7.5", MetricCatalog.read(MetricId.DISTANCE, live, imperial = true, ftp = 200).value)
        assertEquals("30.0", MetricCatalog.read(MetricId.SPEED, live, imperial = false, ftp = 200).value)
    }

    @Test fun ftp_percent_computes_against_ftp() {
        // 250 W at FTP 200 = 125%
        assertEquals("125", MetricCatalog.read(MetricId.FTP_PERCENT, live, false, 200).value)
        assertEquals("––", MetricCatalog.read(MetricId.FTP_PERCENT, empty, false, 200).value)
    }

    @Test fun grade_is_a_placeholder() {
        assertEquals("––", MetricCatalog.read(MetricId.GRADE, live, false, 200).value)
    }
}
