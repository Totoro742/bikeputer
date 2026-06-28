package com.bikeputer.ui

import java.util.Locale

object Format {
    fun duration(ms: Long): String {
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
    }

    fun speed(kmh: Double?, imperial: Boolean): String {
        if (kmh == null) return if (imperial) "-- mph" else "-- km/h"
        return if (imperial) String.format(Locale.US, "%.1f mph", kmh * 0.621371)
        else String.format(Locale.US, "%.1f km/h", kmh)
    }

    fun distance(meters: Double, imperial: Boolean): String =
        if (imperial) String.format(Locale.US, "%.2f mi", meters / 1609.344)
        else String.format(Locale.US, "%.2f km", meters / 1000.0)
}
