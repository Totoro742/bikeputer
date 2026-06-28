package com.bikeputer.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.bikeputer.domain.RideState
import com.bikeputer.ui.theme.JetBrainsMono
import com.bikeputer.ui.theme.LocalBikeColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Shared ride chrome: back (ends the ride session), live clock, a REC-style
 * live/paused indicator, and a pause/resume control. Recomposes with each ride
 * tick, so the clock stays current.
 */
@Composable
fun RideTopBar(
    state: RideState,
    paused: Boolean,
    onBack: () -> Unit,
    onTogglePause: () -> Unit,
    modifier: Modifier = Modifier,
    onMap: Boolean = false,
) {
    val c = LocalBikeColors.current
    val clockColor = if (onMap) c.text else c.text2
    val clock = remember0(state.elapsedMs)

    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(c.panel)
                    .border(1.dp, c.border, CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Text("‹", color = c.text2, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                clock,
                modifier = Modifier.padding(start = 12.dp),
                color = clockColor,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (paused) c.dim else c.accent),
            )
            Text(
                if (paused) "PAUSED" else "REC",
                modifier = Modifier.padding(start = 6.dp),
                color = if (paused) c.dim else c.accent,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                letterSpacing = 2.sp,
            )
        }

        Box(
            Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(c.panel)
                .border(1.dp, c.border, CircleShape)
                .clickable(onClick = onTogglePause),
            contentAlignment = Alignment.Center,
        ) {
            PlayPauseGlyph(paused)
        }
    }
}

@Composable
private fun PlayPauseGlyph(paused: Boolean) {
    val color = LocalBikeColors.current.text
    Canvas(Modifier.size(14.dp)) {
        if (paused) {
            // play triangle
            val p = Path().apply {
                moveTo(size.width * 0.18f, 0f)
                lineTo(size.width * 0.18f, size.height)
                lineTo(size.width, size.height / 2f)
                close()
            }
            drawPath(p, color)
        } else {
            // pause bars
            val barW = size.width * 0.28f
            drawRoundRect(
                color = color,
                topLeft = Offset(0f, 0f),
                size = androidx.compose.ui.geometry.Size(barW, size.height),
            )
            drawRoundRect(
                color = color,
                topLeft = Offset(size.width - barW, 0f),
                size = androidx.compose.ui.geometry.Size(barW, size.height),
            )
        }
    }
}

// Reads the wall clock; the tick arg only exists to drive recomposition.
@Composable
private fun remember0(tick: Long): String =
    SimpleDateFormat("HH:mm", Locale.US).format(Date())
