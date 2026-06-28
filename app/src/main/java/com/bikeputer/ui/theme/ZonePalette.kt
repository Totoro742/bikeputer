package com.bikeputer.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt

/**
 * Zone colour ramp and labels, shared by every gauge, bar and threshold colour.
 * Power has 7 zones, heart-rate 5 — matching the pure-Kotlin [com.bikeputer.metrics.Zones]
 * model so the live `powerZone` / `hrZone` indices map straight onto these colours.
 * Brand teal ([BikeColors.accent]) is reserved for chrome & live state — never data.
 */

val PowerZoneColors = listOf(
    Color(0xFF5E6B7A), // Z1
    Color(0xFF3B82F6), // Z2
    Color(0xFF22C55E), // Z3
    Color(0xFFFACC15), // Z4
    Color(0xFFFB923C), // Z5
    Color(0xFFEF4444), // Z6
    Color(0xFFC084FC), // Z7
)

val HrZoneColors = listOf(
    Color(0xFF3B82F6), // Z1
    Color(0xFF22C55E), // Z2
    Color(0xFFFACC15), // Z3
    Color(0xFFFB923C), // Z4
    Color(0xFFEF4444), // Z5
)

private val PowerZoneNames =
    listOf("Recovery", "Endurance", "Tempo", "Threshold", "VO₂ max", "Anaerobic", "Neuromuscular")
private val HrZoneNames =
    listOf("Recovery", "Endurance", "Aerobic", "Threshold", "Maximum")

/** 1-based zone index → colour, clamped. Null zone → null. */
fun powerZoneColor(zone: Int?): Color? =
    zone?.let { PowerZoneColors[(it - 1).coerceIn(0, PowerZoneColors.lastIndex)] }

fun hrZoneColor(zone: Int?): Color? =
    zone?.let { HrZoneColors[(it - 1).coerceIn(0, HrZoneColors.lastIndex)] }

fun powerZoneName(zone: Int?): String? =
    zone?.let { PowerZoneNames[(it - 1).coerceIn(0, PowerZoneNames.lastIndex)] }

fun hrZoneName(zone: Int?): String? =
    zone?.let { HrZoneNames[(it - 1).coerceIn(0, HrZoneNames.lastIndex)] }

data class ZoneRow(val tag: String, val name: String, val range: String, val color: Color)

/** Power-zone table for the settings preview. Ramp mirrors Zones.powerZonesFromFtp. */
fun powerZoneTable(ftp: Int): List<ZoneRow> {
    fun w(p: Double) = (p * ftp).roundToInt()
    val b = listOf(0.55, 0.75, 0.90, 1.05, 1.20, 1.50).map { w(it) }
    return listOf(
        ZoneRow("Z1", PowerZoneNames[0], "≤${b[0]}", PowerZoneColors[0]),
        ZoneRow("Z2", PowerZoneNames[1], "${b[0] + 1}–${b[1]}", PowerZoneColors[1]),
        ZoneRow("Z3", PowerZoneNames[2], "${b[1] + 1}–${b[2]}", PowerZoneColors[2]),
        ZoneRow("Z4", PowerZoneNames[3], "${b[2] + 1}–${b[3]}", PowerZoneColors[3]),
        ZoneRow("Z5", PowerZoneNames[4], "${b[3] + 1}–${b[4]}", PowerZoneColors[4]),
        ZoneRow("Z6", PowerZoneNames[5], "${b[4] + 1}–${b[5]}", PowerZoneColors[5]),
        ZoneRow("Z7", PowerZoneNames[6], "${b[5] + 1}+", PowerZoneColors[6]),
    )
}

/** HR-zone table for the settings preview. Ramp mirrors Zones.hrZonesFromMax. */
fun hrZoneTable(maxHr: Int): List<ZoneRow> {
    fun h(p: Double) = (p * maxHr).roundToInt()
    val b = listOf(0.60, 0.70, 0.80, 0.90).map { h(it) }
    return listOf(
        ZoneRow("Z1", HrZoneNames[0], "≤${b[0]}", HrZoneColors[0]),
        ZoneRow("Z2", HrZoneNames[1], "${b[0] + 1}–${b[1]}", HrZoneColors[1]),
        ZoneRow("Z3", HrZoneNames[2], "${b[1] + 1}–${b[2]}", HrZoneColors[2]),
        ZoneRow("Z4", HrZoneNames[3], "${b[2] + 1}–${b[3]}", HrZoneColors[3]),
        ZoneRow("Z5", HrZoneNames[4], "${b[3] + 1}–$maxHr", HrZoneColors[4]),
    )
}
