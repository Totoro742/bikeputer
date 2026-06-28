package com.bikeputer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.TextStyle

@Composable
fun BikeputerTheme(
    dark: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colors = if (dark) DarkBikeColors else LightBikeColors

    val scheme = if (dark) {
        darkColorScheme(
            primary = colors.accent,
            background = colors.bg,
            surface = colors.panel,
            onPrimary = colors.bg,
            onBackground = colors.text,
            onSurface = colors.text,
        )
    } else {
        lightColorScheme(
            primary = colors.accent,
            background = colors.bg,
            surface = colors.panel,
            onPrimary = colors.bg,
            onBackground = colors.text,
            onSurface = colors.text,
        )
    }

    // Mono everywhere by default; metric numbers opt into Barlow explicitly.
    val base = Typography()
    val typography = Typography(
        displayLarge = base.displayLarge.mono(),
        displayMedium = base.displayMedium.mono(),
        displaySmall = base.displaySmall.mono(),
        headlineLarge = base.headlineLarge.mono(),
        headlineMedium = base.headlineMedium.mono(),
        headlineSmall = base.headlineSmall.mono(),
        titleLarge = base.titleLarge.mono(),
        titleMedium = base.titleMedium.mono(),
        titleSmall = base.titleSmall.mono(),
        bodyLarge = base.bodyLarge.mono(),
        bodyMedium = base.bodyMedium.mono(),
        bodySmall = base.bodySmall.mono(),
        labelLarge = base.labelLarge.mono(),
        labelMedium = base.labelMedium.mono(),
        labelSmall = base.labelSmall.mono(),
    )

    CompositionLocalProvider(LocalBikeColors provides colors) {
        MaterialTheme(colorScheme = scheme, typography = typography, content = content)
    }
}

private fun TextStyle.mono() = copy(fontFamily = JetBrainsMono)
