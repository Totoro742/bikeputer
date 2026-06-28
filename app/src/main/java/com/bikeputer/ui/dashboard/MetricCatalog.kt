package com.bikeputer.ui.dashboard

import com.bikeputer.domain.MetricId
import com.bikeputer.domain.RideState

/** Color intent for a tile, resolved to a concrete Compose color at render time. */
enum class ColorRole { Text, Dim, HeartRate, PowerZone, HrZone, Accent }

/** Everything a metric tile needs to render, fully formatted. */
data class MetricReadout(
    val label: String,
    val value: String,
    val unit: String?,
    val color: ColorRole,
)

/** Pure, framework-free formatting for every placeable metric. Reuses [DashFormat]. */
object MetricCatalog {

    private const val DASH = "––"

    fun label(id: MetricId, imperial: Boolean): String = when (id) {
        MetricId.POWER -> "POWER"
        MetricId.AVG_POWER_3S -> "3s POWER"
        MetricId.AVG_POWER_10S -> "10s POWER"
        MetricId.NORMALIZED_POWER -> "NORM POWER"
        MetricId.SESSION_AVG_POWER -> "AVG POWER"
        MetricId.FTP_PERCENT -> "% FTP"
        MetricId.POWER_ZONE -> "POWER ZONE"
        MetricId.CADENCE -> "CADENCE"
        MetricId.HEART_RATE -> "HEART RATE"
        MetricId.AVG_HEART_RATE -> "AVG HR"
        MetricId.HR_ZONE -> "HR ZONE"
        MetricId.SPEED -> "SPEED · ${DashFormat.speedUnit(imperial)}"
        MetricId.AVG_SPEED -> "AVG SPD · ${DashFormat.speedUnit(imperial)}"
        MetricId.MAX_SPEED -> "MAX SPD · ${DashFormat.speedUnit(imperial)}"
        MetricId.DISTANCE -> "DIST · ${DashFormat.distanceUnit(imperial)}"
        MetricId.ELEVATION_GAIN -> "ASCENT · ${if (imperial) "FT" else "M"}"
        MetricId.ELAPSED -> "ELAPSED"
        MetricId.ENERGY_KJ -> "ENERGY · kJ"
        MetricId.GRADE -> "GRADE"
    }

    fun read(id: MetricId, state: RideState, imperial: Boolean, ftp: Int): MetricReadout {
        val value: String
        val unit: String?
        val color: ColorRole
        when (id) {
            MetricId.POWER -> {
                value = DashFormat.power(state.instantPowerW); unit = "W"
                color = if (state.powerConnected) ColorRole.Text else ColorRole.Dim
            }
            MetricId.AVG_POWER_3S -> { value = DashFormat.power(state.avgPower3sW); unit = "W"; color = ColorRole.Text }
            MetricId.AVG_POWER_10S -> { value = DashFormat.power(state.avgPower10sW); unit = "W"; color = ColorRole.Text }
            MetricId.NORMALIZED_POWER -> { value = DashFormat.power(state.normalizedPowerW); unit = "W"; color = ColorRole.Text }
            MetricId.SESSION_AVG_POWER -> { value = DashFormat.power(state.sessionAvgPowerW); unit = "W"; color = ColorRole.Text }
            MetricId.FTP_PERCENT -> {
                value = DashFormat.ftpPercent(state.instantPowerW, ftp)?.toString() ?: DASH
                unit = "%"; color = ColorRole.PowerZone
            }
            MetricId.POWER_ZONE -> {
                value = state.powerZone?.let { "Z$it" } ?: DASH; unit = null; color = ColorRole.PowerZone
            }
            MetricId.CADENCE -> { value = DashFormat.int(state.cadenceRpm); unit = "rpm"; color = ColorRole.Text }
            MetricId.HEART_RATE -> {
                value = DashFormat.int(state.heartRateBpm); unit = "bpm"
                color = if (state.hrConnected) ColorRole.HeartRate else ColorRole.Dim
            }
            MetricId.AVG_HEART_RATE -> { value = DashFormat.int(state.avgHeartRateBpm); unit = "bpm"; color = ColorRole.HeartRate }
            MetricId.HR_ZONE -> { value = state.hrZone?.let { "Z$it" } ?: DASH; unit = null; color = ColorRole.HrZone }
            MetricId.SPEED -> {
                value = DashFormat.speed(state.speedKmh, imperial); unit = null
                color = if (state.gpsAvailable) ColorRole.Text else ColorRole.Dim
            }
            MetricId.AVG_SPEED -> { value = DashFormat.speed(state.avgSpeedKmh, imperial); unit = null; color = ColorRole.Text }
            MetricId.MAX_SPEED -> { value = DashFormat.speed(state.maxSpeedKmh, imperial); unit = null; color = ColorRole.Text }
            MetricId.DISTANCE -> { value = DashFormat.distance(state.distanceM, imperial); unit = null; color = ColorRole.Text }
            MetricId.ELEVATION_GAIN -> { value = DashFormat.elevation(state.elevationGainM, imperial); unit = null; color = ColorRole.Text }
            MetricId.ELAPSED -> { value = DashFormat.duration(state.elapsedMs); unit = null; color = ColorRole.Text }
            MetricId.ENERGY_KJ -> { value = DashFormat.int(DashFormat.energyKj(state)); unit = null; color = ColorRole.Text }
            MetricId.GRADE -> { value = DASH; unit = null; color = ColorRole.Accent }
        }
        return MetricReadout(label(id, imperial), value, unit, color)
    }
}
