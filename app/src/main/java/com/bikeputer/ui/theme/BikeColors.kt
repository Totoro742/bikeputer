package com.bikeputer.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The full Bikeputer surface palette, mirrored from the approved design tokens
 * (`Bikeputer Dashboard.dc.html`). Both dark (OLED, primary) and light
 * (daylight) variants share the same teal [accent].
 */
data class BikeColors(
    val bg: Color,
    val bg2: Color,
    val panel: Color,
    val line: Color,
    val border: Color,
    val border2: Color,
    val text: Color,
    val text2: Color,
    val dim: Color,
    val dimmer: Color,
    val navpill: Color,
    val accentSoft: Color,
    val accent: Color,
    val route: Color,
    val isLight: Boolean,
)

val DarkBikeColors = BikeColors(
    bg = Color(0xFF0A0C0F),
    bg2 = Color(0xFF101317),
    panel = Color(0xFF14181D),
    line = Color(0xFF1B2026),
    border = Color(0xFF232A31),
    border2 = Color(0xFF2A323A),
    text = Color(0xFFF3F6F8),
    text2 = Color(0xFFC9CFD6),
    dim = Color(0xFF79828D),
    dimmer = Color(0xFF4A525B),
    navpill = Color(0xFF3A4047),
    accentSoft = Color(0xFF0C1D22),
    accent = Color(0xFF19E3C8),
    route = Color(0xFF19E3C8),
    isLight = false,
)

val LightBikeColors = BikeColors(
    bg = Color(0xFFF3F5F8),
    bg2 = Color(0xFFE9EDF2),
    panel = Color(0xFFFFFFFF),
    line = Color(0xFFE3E8ED),
    border = Color(0xFFD4DAE1),
    border2 = Color(0xFFCCD3DA),
    text = Color(0xFF161A20),
    text2 = Color(0xFF3B434C),
    dim = Color(0xFF69727C),
    dimmer = Color(0xFFAEB6BF),
    navpill = Color(0xFFC3C9CF),
    accentSoft = Color(0xFFDCF5F0),
    accent = Color(0xFF19E3C8),
    route = Color(0xFF0E9E8C),
    isLight = true,
)

/** Heart-rate metric colour — constant across themes. */
val HeartRateColor = Color(0xFFEF4444)

val LocalBikeColors = staticCompositionLocalOf { DarkBikeColors }
