package io.github.halilozel1903.jikan.sample

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import kotlin.math.abs

/** A focusable pill: filled with the primary color when [primary], translucent otherwise. */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    selected: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val filled = primary || selected
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(shape = CircleShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (filled) colors.primary else Color.White.copy(alpha = 0.10f),
            contentColor = if (filled) colors.onPrimary else colors.onSurface,
            focusedContainerColor = colors.onSurface,
            focusedContentColor = colors.background,
            pressedContainerColor = colors.onSurface.copy(alpha = 0.8f),
            pressedContentColor = colors.background,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(2.dp, colors.onSurface), shape = CircleShape),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = LocalContentColor.current,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Scrolls only as far as needed to show the focused item, plus [marginPx] so a rail's title stays visible.
 * Compose's default on TV keeps the focused item at 30 % of the screen, which scrolls the hero away as soon
 * as anything below it is focused.
 */
@OptIn(ExperimentalFoundationApi::class)
class MinimalBringIntoViewSpec(private val marginPx: Float) : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        val leading = offset
        val trailing = offset + size
        return when {
            // Fully visible (with the margin where there is room): stay.
            leading >= 0f && trailing <= containerSize -> 0f
            // Bigger than the viewport: align its start.
            size >= containerSize -> leading
            // Above the viewport: scroll up so it starts [marginPx] below the top.
            leading < 0f -> leading - marginPx
            // Below: scroll down so it ends [marginPx] above the bottom, without going past its start.
            else -> minOf(trailing - containerSize + marginPx, leading).let { if (abs(it) < 0.5f) 0f else it }
        }
    }
}
