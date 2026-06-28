package com.bikeputer.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.bikeputer.R

/** Big tabular metric numbers. */
val Barlow = FontFamily(
    Font(R.font.barlow_semi_condensed_medium, FontWeight.Medium),
    Font(R.font.barlow_semi_condensed_semibold, FontWeight.SemiBold),
    Font(R.font.barlow_semi_condensed_bold, FontWeight.Bold),
    Font(R.font.barlow_semi_condensed_extrabold, FontWeight.ExtraBold),
)

/** Labels, units, chrome and all UI text. */
val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_semibold, FontWeight.SemiBold),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
)
