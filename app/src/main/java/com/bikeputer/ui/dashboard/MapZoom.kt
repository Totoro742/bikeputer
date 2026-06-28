package com.bikeputer.ui.dashboard

/**
 * Pure zoom math for the Dashboard C ride map. Kept osmdroid-free so it is
 * JVM-testable. [adaptiveMin]/[adaptiveMax] preserve today's clamp span
 * (default − 2 .. default + 1), so [DEFAULT] reproduces the historical
 * [14.0, 17.0] fit-ahead clamp and 16.0 fixed-follow zoom.
 */
object MapZoom {
    const val DEFAULT = 16
    const val MIN_SETTING = 13
    const val MAX_SETTING = 18

    fun coerce(z: Int): Int = z.coerceIn(MIN_SETTING, MAX_SETTING)

    fun adaptiveMin(default: Int): Double = default - 2.0

    fun adaptiveMax(default: Int): Double = default + 1.0
}
