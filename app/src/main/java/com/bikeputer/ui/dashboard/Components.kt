package com.bikeputer.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.bikeputer.ui.theme.Barlow
import com.bikeputer.ui.theme.JetBrainsMono
import com.bikeputer.ui.theme.LocalBikeColors

/**
 * Segmented zone ramp. The [active] segment (0-based) burns at full colour;
 * the rest sit dimmed. Used for both power (7) and HR (5) ramps.
 */
@Composable
fun ZoneBar(
    colors: List<Color>,
    active: Int?,
    modifier: Modifier = Modifier,
    height: Dp = 7.dp,
    gap: Dp = 2.dp,
) {
    Row(
        modifier.height(height),
        horizontalArrangement = Arrangement.spacedBy(gap),
    ) {
        colors.forEachIndexed { i, c ->
            val isActive = i == active
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(1.dp))
                    .alpha(if (isActive) 1f else 0.28f)
                    .background(c),
            )
        }
    }
}

/** Uppercase, letter-spaced caption in dim colour. */
@Composable
fun MetricLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LocalBikeColors.current.dim,
    size: Int = 11,
    spacing: Double = 2.0,
    align: TextAlign? = null,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Medium,
        fontSize = size.sp,
        letterSpacing = spacing.sp,
        textAlign = align,
    )
}

/** Big tabular Barlow metric number with an optional trailing unit. */
@Composable
fun MetricNumber(
    value: String,
    modifier: Modifier = Modifier,
    size: Int = 54,
    color: Color = LocalBikeColors.current.text,
    weight: FontWeight = FontWeight.ExtraBold,
    unit: String? = null,
    unitSize: Int = 20,
) {
    val c = LocalBikeColors.current
    Row(modifier, verticalAlignment = androidx.compose.ui.Alignment.Bottom) {
        Text(
            text = value,
            color = color,
            fontFamily = Barlow,
            fontWeight = weight,
            fontSize = size.sp,
            letterSpacing = (-1).sp,
        )
        if (unit != null) {
            Text(
                text = unit,
                modifier = Modifier.padding(start = 4.dp, bottom = (size * 0.12).dp),
                color = c.dim,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.SemiBold,
                fontSize = unitSize.sp,
            )
        }
    }
}

/** Thin theme divider line. */
@Composable
fun HLine(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(LocalBikeColors.current.line),
    )
}
