package com.bikeputer.ui.dashboard

import com.bikeputer.domain.RideState
import java.util.Locale

/**
 * Bare-value formatters for the dashboards. Labels carry the units, so these
 * return just the number (or an em-dash placeholder when the value is missing).
 */
object DashFormat {
    private const val DASH = "––"

    fun power(w: Int?): String = w?.toString() ?: DASH
    fun int(n: Int?): String = n?.toString() ?: DASH

    fun speed(kmh: Double?, imperial: Boolean): String {
        if (kmh == null) return DASH
        val v = if (imperial) kmh * 0.621371 else kmh
        return String.format(Locale.US, "%.1f", v)
    }

    fun speedUnit(imperial: Boolean) = if (imperial) "MPH" else "KM/H"

    fun distance(meters: Double, imperial: Boolean): String {
        val v = if (imperial) meters / 1609.344 else meters / 1000.0
        return String.format(Locale.US, "%.1f", v)
    }

    fun distanceUnit(imperial: Boolean) = if (imperial) "MI" else "KM"

    /** Below 0.5 km (metric) the readout switches to whole metres; imperial mirrors with
     * whole feet under 0.1 mi, beyond which both fall back to the km/mi number. */
    private const val METRES_BELOW_M = 500.0
    private const val FEET_BELOW_M = 0.1 * 1609.344

    private fun inSmallUnit(meters: Double, imperial: Boolean) =
        if (imperial) meters < FEET_BELOW_M else meters < METRES_BELOW_M

    /** Adaptive distance number: whole metres/feet when close, else km/mi to one decimal. */
    fun distanceValue(meters: Double, imperial: Boolean): String = when {
        !inSmallUnit(meters, imperial) -> distance(meters, imperial)
        imperial -> Math.round(meters * 3.28084).toString()
        else -> Math.round(meters).toString()
    }

    /** Unit matching [distanceValue] for the same inputs. */
    fun distanceUnit(meters: Double, imperial: Boolean): String = when {
        !inSmallUnit(meters, imperial) -> distanceUnit(imperial)
        imperial -> "FT"
        else -> "M"
    }

    fun elevation(meters: Double, imperial: Boolean): String =
        if (imperial) (meters * 3.28084).toInt().toString() else meters.toInt().toString()

    fun elevationUnit(imperial: Boolean) = if (imperial) "FEET" else "METRES"

    fun duration(ms: Long): String {
        val s = ms / 1000
        return String.format(Locale.US, "%d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)
    }

    /** % of FTP for the current instant power, or null if either is missing/zero. */
    fun ftpPercent(power: Int?, ftp: Int): Int? =
        if (power == null || ftp <= 0) null else Math.round(power * 100f / ftp)

    /** Work done so far in kJ, approximated as session-average power × elapsed time. */
    fun energyKj(state: RideState): Int? {
        val avg = state.sessionAvgPowerW ?: return null
        return (avg.toLong() * (state.elapsedMs / 1000) / 1000).toInt()
    }
}
